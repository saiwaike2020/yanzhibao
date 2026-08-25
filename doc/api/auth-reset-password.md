# `/api/auth/password/reset` 忘记密码 / 手机号验证码重置密码接口实现详解

> - 设计依据：`doc/userAccountRequirement_260820_v3.md` 第 **4.7 密码重置流程** 节 / 用例 **UC-025**
> - 实现代码：`com.crm` 包（Controller → Service → Repository → Entity 分层，Spring Boot 3.2 + Spring Data JPA + Spring Security + PostgreSQL 16）
> - 涉及前置接口：`POST /api/sms/verification-code`（发送短信验证码，`scene=RESET_PWD`）

## 目录

1. [接口概述](#1-接口概述)
2. [整体架构与调用链](#2-整体架构与调用链)
3. [数据库设计](#3-数据库设计)
4. [实现步骤详解](#4-实现步骤详解)
5. [请求 / 响应示例](#5-请求--响应示例)
6. [测试](#6-测试)
7. [安全与注意事项](#7-安全与注意事项)

---

## 1. 接口概述

| 项 | 值 |
| :--- | :--- |
| HTTP 方法 | `POST` |
| 路径 | `/api/auth/password/reset` |
| 鉴权要求 | 无需登录（已在 `SecurityConfig` 白名单放行） |
| 前置接口 | `POST /api/sms/verification-code`（先获取短信验证码，`scene=RESET_PWD`） |
| 核心功能 | 忘记密码时，通过已注册手机号 + 短信验证码重置登录密码 |
| 用例来源 | UC-025（设计文档 7.25 业务场景） |

### 1.1 业务规则（来自设计文档）

1. 手机号必须**已注册**且账号状态为 **ACTIVE**，否则禁止重置；
2. 短信验证码必须真实有效：**存在、未过期、未消耗、错误次数未超限、内容匹配**，且场景为 `RESET_PWD`；
3. 新密码需 **≥ 8 位且同时包含字母和数字**，使用 **BCrypt** 加密存储；
4. 仅更新 `user_auths` 中 `auth_type=PHONE` 记录的密码哈希；未绑定手机号认证（仅微信认证）的账号提示无法重置；
5. 重置成功后**不签发 Token**，用户使用新密码重新登录。

---

## 2. 整体架构与调用链

分层调用链路如下：

```text
浏览器 / 客户端
   │  HTTP POST /api/auth/password/reset   (JSON)
   ▼
SecurityConfig（Spring Security 过滤器链，本路径 permitAll 放行）
   ▼
AuthController.resetPassword()                 [Controller 层] @Valid 校验请求体
   ▼
AuthService.resetPassword()                    [Service 层] @Transactional
   ├─ ① SysUserRepository.findByPhone()         校验手机号已注册 + 账号状态
   ├─ ② SmsVerificationService.verifyCode()     校验验证码（RESET_PWD，通过后标记已消耗）
   ├─ ③ UserAuthRepository.findByAuthTypeAndIdentifier()  查找 PHONE 认证记录
   └─ ④ UserAuthRepository.save()               更新密码哈希（bcrypt）
   ▼
GlobalExceptionHandler（异常统一转换为 ApiResponse 结构）
   ▼
HTTP 200 JSON  ←  ApiResponse<Void>
```

涉及的主要代码文件：

| 分层 | 文件 |
| :--- | :--- |
| Controller | `src/main/java/com/crm/controller/AuthController.java` |
| 请求 DTO | `src/main/java/com/crm/dto/auth/ResetPasswordRequest.java` |
| Service | `src/main/java/com/crm/service/AuthService.java`、`SmsVerificationService.java` |
| Repository | `SysUserRepository`、`UserAuthRepository`、`SmsVerificationRepository` |
| Entity | `SysUser`、`UserAuth`、`SmsVerification` |
| 安全 | `SecurityConfig`（白名单 `/api/auth/password/reset`） |
| 通用 | `ApiResponse`、`GlobalExceptionHandler`、`BusinessException`、`ErrorCode` |

---

## 3. 数据库设计

重置密码仅涉及已有表，**无需新增表 / 字段**：

- `sys_users`：校验手机号已注册、账号状态为 ACTIVE（禁用 / 注销禁止重置）；
- `user_auths`：更新 `auth_type=PHONE`、`identifier=手机号` 记录的 `credential`（bcrypt）与 `verified_at`；
- `sms_verifications`：发送 / 校验 `scene=RESET_PWD` 的验证码（该场景枚举值在 v3 数据库设计中已预留）。

---

## 4. 实现步骤详解

### 4.1 发送验证码（前置接口）

`POST /api/sms/verification-code`，请求体：

```json
{ "phone": "13800138000", "scene": "RESET_PWD" }
```

`SmsVerificationService.sendVerificationCode()` 中新增校验（与注册场景对称）：

- `REGISTER` 场景：手机号已注册 → 提示直接登录；
- **`RESET_PWD` 场景：手机号未注册 → `PHONE_NOT_REGISTERED(1002)`「该手机号未注册」**；
- 其余逻辑复用：5 分钟有效、60 秒限频（同一手机号同一场景）、最多 5 次错误尝试。

### 4.2 重置密码

`AuthService.resetPassword()` 流程：

1. `sysUserRepository.findByPhone(phone)`：未找到或已软删除 → `PHONE_NOT_REGISTERED(1002)`；
2. 账号状态检查：`DISABLED` → `ACCOUNT_DISABLED(1006)`，`CANCELLED` → `ACCOUNT_CANCELLED(1007)`；
3. `smsVerificationService.verifyCode(phone, SmsScene.RESET_PWD, smsCode)`：校验通过后标记验证码已消耗；
4. `userAuthRepository.findByAuthTypeAndIdentifier(AuthType.PHONE, phone)`：不存在 → 提示「该手机号未绑定密码登录方式，无法重置密码」（沿用 1002）；
5. `auth.setCredential(passwordEncoder.encode(newPassword))`，更新 `verified_at` 并保存。

---

## 5. 请求 / 响应示例

### 5.1 发送验证码

```bash
curl -X POST http://localhost:8080/api/sms/verification-code \
  -H "Content-Type: application/json" \
  -d '{"phone":"13800138000","scene":"RESET_PWD"}'
```

响应：

```json
{ "code": 0, "message": "success", "data": null }
```

### 5.2 重置密码

```bash
curl -X POST http://localhost:8080/api/auth/password/reset \
  -H "Content-Type: application/json" \
  -d '{"phone":"13800138000","smsCode":"000000","newPassword":"xyz67890"}'
```

响应：

```json
{ "code": 0, "message": "success", "data": null }
```

### 5.3 常见失败响应

| 场景 | HTTP 状态 | code | message |
| :--- | :--- | :--- | :--- |
| 手机号未注册（发码 / 重置时） | 200 | 1002 | 该手机号未注册 |
| 手机号格式错误 | 200 | 400 | 手机号格式不正确 |
| 密码强度不足 | 200 | 400 | 密码需包含字母和数字 |
| 60 秒内重复发送 | 200 | 1101 | 验证码发送过于频繁，请稍后再试 |
| 验证码错误 | 200 | 1102 | 验证码错误或已失效 |
| 验证码过期 | 200 | 1103 | 验证码已过期，请重新获取 |
| 验证码错误次数超限 | 200 | 1104 | 验证码错误次数过多，已失效 |
| 账号被禁用 | 200 | 1006 | 账号已被禁用 |
| 账号已注销 | 200 | 1007 | 账号已注销 |
| 未绑定手机号密码认证 | 200 | 1002 | 该手机号未绑定密码登录方式，无法重置密码 |

> 注：业务错误以 HTTP 200 返回、通过 `code` 区分，是前后端约定的统一风格；真正未处理异常才返回 HTTP 500。

---

## 6. 测试

### 6.1 自动化集成测试（`AuthPasswordResetIntegrationTest`）

`src/test/java/com/crm/AuthPasswordResetIntegrationTest.java`，基于 `@SpringBootTest` + `@AutoConfigureMockMvc`，**连接真实 PostgreSQL** 验证全流程：

| 测试用例 | 验证内容 |
| :--- | :--- |
| `passwordResetFlowShouldSucceed` | 注册 → 发送 `RESET_PWD` 验证码 → `000000` 重置 → 数据库密码哈希已更新（新密码匹配、旧密码不匹配） |
| `sendResetCodeForUnregisteredPhoneShouldFail` | 未注册手机号发送 `RESET_PWD` 验证码失败，`code=1002` |
| `resetWithWrongCodeShouldFail` | 错误验证码重置失败，`code=1102`，且密码未变更 |

运行方式：

```bash
mvn test
```

### 6.2 手工测试

**方式一：浏览器测试页** `http://localhost:8080/test-reset-password.html`
提供「发送验证码」「重置密码」两个表单，直接点击即可看到完整请求响应。

**方式二：命令行（PowerShell）**

```powershell
# 1. 发送重置密码验证码（Mock 验证码固定 000000）
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/sms/verification-code" `
  -ContentType "application/json" `
  -Body '{"phone":"13800138000","scene":"RESET_PWD"}'

# 2. 重置密码
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/auth/password/reset" `
  -ContentType "application/json" `
  -Body '{"phone":"13800138000","smsCode":"000000","newPassword":"xyz67890"}'
```

### 6.3 重置后的数据库验证

```sql
-- 查看手机号认证记录的密码哈希是否更新（bcrypt 每次加密结果不同，直接比对旧值无意义）
SELECT auth_id, user_id, auth_type, identifier, left(credential, 20) AS pwd_bcrypt, verified_at
FROM user_auths WHERE auth_type='PHONE' ORDER BY auth_id DESC LIMIT 3;

-- 验证码记录（重置成功的 used 应为非空）
SELECT id, phone, scene, attempts, used_at IS NOT NULL AS used FROM sms_verifications ORDER BY id DESC LIMIT 3;
```

---

## 7. 安全与注意事项

1. **密码安全**：新密码同样使用 BCrypt（自动加盐、抗彩虹表），与注册 / 登录保持同一套密码策略。
2. **验证码安全**：完全复用注册验证码策略——验证码不落明文（SHA-256 哈希）、5 分钟有效、60 秒发送限频、最多 5 次错误尝试、一次性使用、校验通过立即标记 `used_at` 防重放。
3. **未注册手机号探测防护**：发送 `RESET_PWD` 验证码时即校验手机号已注册，未注册直接提示，避免盲目发送短信。
4. **账号状态校验**：禁用 / 注销账号禁止重置，防止绕过账号管控。
5. **不自动登录**：重置成功后不签发 Token，引导用户使用新密码重新登录，避免旧会话 / 异常会话污染。
6. **接口放行范围**：仅 `/api/auth/password/reset`（忘记密码）放行至匿名白名单；`/api/auth/password/set`（已登录修改密码）仍需携带 JWT 访问，二者权限边界不混淆。
7. **Mock 短信**：当前验证码固定 `000000`，上线前须接入真实短信网关并改为随机验证码（同注册流程）。

---

> 本文档与代码保持同步；后续若调整重置流程（如接入真实短信、增加图形验证码、强制下线已登录会话），请同步更新本文档「步骤 4」与「安全与注意事项」小节。
