# `/api/auth/register/phone` 手机号验证码注册接口实现详解

> - 设计依据：`doc/userAccountRequirement_260820_v2.md` 第 **4.4.1 手机号验证码注册** 节 / 用例 **UC-001**
> - 实现代码：`com.crm` 包（Controller → Service → Repository → Entity 分层，Spring Boot 3.2 + Spring Data JPA + Spring Security + PostgreSQL 16）
> - 涉及前置接口：`POST /api/sms/verification-code`（发送短信验证码）

## 目录

1. [接口概述](#1-接口概述)
2. [整体架构与调用链](#2-整体架构与调用链)
3. [数据库设计](#3-数据库设计)
4. [前置准备](#4-前置准备)
5. [实现步骤详解](#5-实现步骤详解)
6. [请求 / 响应示例](#6-请求--响应示例)
7. [测试](#7-测试)
8. [安全与注意事项](#8-安全与注意事项)

---

## 1. 接口概述

| 项 | 值 |
| :--- | :--- |
| HTTP 方法 | `POST` |
| 路径 | `/api/auth/register/phone` |
| 鉴权要求 | 无需登录（已在 `SecurityConfig` 白名单放行） |
| 前置接口 | `POST /api/sms/verification-code`（先获取短信验证码） |
| 核心功能 | 手机号 + 短信验证码注册；注册成功后自动登录并签发 JWT Token |
| 用例来源 | UC-001（设计文档 7.1 业务场景） |

### 1.1 业务规则（来自设计文档）

1. 手机号必须**未注册**，否则提示直接登录；
2. 短信验证码必须真实有效：**存在、未过期、未消耗、错误次数未超限、内容匹配**；
3. 密码需 **≥ 8 位且同时包含字母和数字**，使用 **BCrypt** 加密存储；
4. 注册成功后 `sys_users` 主表写入 `phone`，并在 `user_auths` 创建手机号认证记录；
5. 返回 JWT Token（默认 24 小时有效），实现「注册即登录」。

---

## 2. 整体架构与调用链

分层调用链路如下：

```text
浏览器 / 客户端
   │  HTTP POST /api/auth/register/phone   (JSON)
   ▼
SecurityConfig（Spring Security 过滤器链，本路径 permitAll 放行）
   ▼
AuthController.registerByPhone()                 [Controller 层] @Valid 校验请求体
   ▼
AuthService.registerByPhone()                   [Service 层] @Transactional
   ├─ ① SysUserRepository.existsByPhone()         校验手机号未注册
   ├─ ② SmsVerificationService.verifyCode()       校验短信验证码（通过后标记已消耗）
   ├─ ③ SysUserRepository.save()                  创建用户（sys_users）
   ├─ ④ UserAuthRepository.save()                 创建手机号认证（user_auths，bcrypt）
   └─ ⑤ JwtTokenProvider.generateToken()          签发 JWT
   ▼
GlobalExceptionHandler（异常统一转换为 ApiResponse 结构）
   ▼
HTTP 200 JSON  ←  ApiResponse<AuthTokenResponse>
```

涉及的主要代码文件：

| 分层 | 文件 |
| :--- | :--- |
| Controller | `src/main/java/com/crm/controller/AuthController.java` |
| 请求 DTO | `src/main/java/com/crm/dto/auth/PhoneRegisterRequest.java` |
| 响应 DTO | `src/main/java/com/crm/dto/auth/AuthTokenResponse.java` |
| Service | `src/main/java/com/crm/service/AuthService.java`、`SmsVerificationService.java` |
| Repository | `SysUserRepository`、`UserAuthRepository`、`SmsVerificationRepository` |
| Entity | `SysUser`、`UserAuth`、`SmsVerification` |
| 安全 | `SecurityConfig`、`JwtTokenProvider`、`LoginUser` |
| 通用 | `ApiResponse`、`GlobalExceptionHandler`、`BusinessException`、`ErrorCode` |

---

## 3. 数据库设计

注册流程涉及 3 张表（由 `docker/init-sql/1_init_schema.sql` 建表，JPA `ddl-auto: update` 负责校验/补齐）：

### 3.1 `sys_users`（用户主表）

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| `user_id` | BIGINT IDENTITY | 主键 |
| `user_no` | VARCHAR(64) UNIQUE | 用户全局唯一业务编号 |
| `phone` | VARCHAR(20) | 用户主手机号（注册时写入） |
| `nickname` | VARCHAR(64) | 昵称 |
| `avatar_url` | VARCHAR(255) | 头像 URL |
| `status` | SMALLINT | 1-正常 ACTIVE / 2-禁用 / 3-注销 |
| `system_role` | VARCHAR(32) | NONE / SYSTEM_ADMIN / AUDITOR / CUSTOMER_SERVICE |
| `created_at` / `updated_at` | TIMESTAMP | 创建 / 更新时间 |
| `deleted_at` | TIMESTAMP | 软删除标记 |

### 3.2 `user_auths`（认证标识表）

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| `auth_id` | BIGINT IDENTITY | 主键 |
| `user_id` | BIGINT | 关联 `sys_users.user_id` |
| `auth_type` | VARCHAR(20) | PHONE / WECHAT |
| `identifier` | VARCHAR(128) | 认证标识（手机号 / 微信 unionid） |
| `credential` | VARCHAR(255) | 凭证（密码 BCrypt 哈希） |
| `verified_at` | TIMESTAMP | 认证通过时间 |
| `status` | SMALLINT | 1-启用 / 0-禁用 |

> 唯一约束 `(auth_type, identifier)`：保证一个手机号或微信只能绑定一个用户。

### 3.3 `sms_verifications`（短信验证码表）

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| `id` | BIGINT IDENTITY | 主键 |
| `phone` | VARCHAR(20) | 接收验证码的手机号 |
| `code_hash` | VARCHAR(128) | 验证码哈希（SHA-256 hex） |
| `scene` | VARCHAR(30) | REGISTER / LOGIN / BIND_PHONE / ADMIN_ROLE_CHANGE 等 |
| `expired_at` | TIMESTAMP | 过期时间（5 分钟） |
| `used_at` | TIMESTAMP | 消耗时间（NULL 表示未使用） |
| `attempts` | SMALLINT | 尝试校验错误次数（上限 5） |
| `ip_address` | VARCHAR(45) | 客户端 IP |
| `created_at` | TIMESTAMP | 发送时间 |

---

## 4. 前置准备

### 4.1 数据库与连接配置

数据库通过 `docker/docker-compose.yml` 启动，项目连接配置见 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/yzb
    username: yzb_user
    password: YZB_DB2026
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    open-in-view: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

> `ddl-auto: update` 表示实体与表结构存在差异时自动补齐（不会覆盖已有列）。

### 4.2 JWT 配置（application.yml）

```yaml
crm:
  jwt:
    # HS256 签名密钥：长度必须 >= 32 字节，生产环境用环境变量覆盖
    secret: ${JWT_SECRET:yzb-crm-2026-jwt-signing-key-must-be-at-least-32-bytes-0123456789}
    # Token 有效期（毫秒），默认 24 小时
    expiration-ms: ${JWT_EXPIRATION_MS:86400000}
```

### 4.3 依赖（pom.xml）

- `spring-boot-starter-web`：Web MVC、JSON 序列化
- `spring-boot-starter-data-jpa`：JPA + Hibernate
- `spring-boot-starter-validation`：`@Valid` 参数校验
- `spring-boot-starter-security`：安全框架
- `org.postgresql:postgresql`：PostgreSQL 驱动
- `io.jsonwebtoken:jjwt-api/impl/jackson:0.12.6`：JWT 生成与解析
- `org.projectlombok:lombok`：简化样板代码

### 4.4 安全放行（SecurityConfig）

`/api/auth/register/**`、`/api/sms/**` 等注册、登录、验证码相关路径**无需认证**即可访问：

```java
.authorizeHttpRequests(auth -> auth
        .requestMatchers("/test-register.html", "/api/auth/register/**", "/api/auth/login/**",
                "/api/sms/**", "/error").permitAll()
        .anyRequest().authenticated())
```

---

## 5. 实现步骤详解

### 步骤 0：请求进入（安全链路）

应用采用**无状态 JWT** 认证模式（`SessionCreationPolicy.STATELESS`）。注册接口在安全白名单内，`JwtAuthenticationFilter` 对未携带 Token 的匿名请求**直接放行**，无需认证即可进入 Controller。

### 步骤 1：定义请求 DTO（`PhoneRegisterRequest`）

`src/main/java/com/crm/dto/auth/PhoneRegisterRequest.java`：

```java
@Data
public class PhoneRegisterRequest {

    /** 手机号 */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 短信验证码 */
    @NotBlank(message = "验证码不能为空")
    @Size(min = 4, max = 6, message = "验证码长度不正确")
    private String smsCode;

    /** 登录密码：至少 8 位，需包含字母和数字 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度需在 8-64 位之间")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码需包含字母和数字")
    private String password;

    /** 昵称（可选） */
    @Size(max = 64, message = "昵称不能超过 64 个字符")
    private String nickname;

    /** 头像 URL（可选） */
    @Size(max = 255, message = "头像 URL 过长")
    private String avatarUrl;
}
```

**关键点：** 通过 Jakarta Bean Validation 注解在**入口处**完成参数合法性校验：
- `phone`：非空 + 中国大陆手机号正则 `^1[3-9]\d{9}$`
- `smsCode`：非空 + 长度 4~6
- `password`：非空 + 8~64 位 + 必须同时含字母和数字（`(?=.*[A-Za-z])(?=.*\d)`）
- 校验失败由 `GlobalExceptionHandler` 统一返回 `code=400`

### 步骤 2：Controller 层（`AuthController`）

`src/main/java/com/crm/controller/AuthController.java`：

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 手机号 + 短信验证码注册 (UC-001) */
    @PostMapping("/register/phone")
    public ApiResponse<AuthTokenResponse> registerByPhone(@Valid @RequestBody PhoneRegisterRequest request) {
        return ApiResponse.ok(authService.registerByPhone(request));
    }
}
```

**关键点：**
- `@RestController`：方法返回值自动 JSON 序列化；
- `@RequestMapping("/api/auth")` + `@PostMapping("/register/phone")`：拼出完整路径 `/api/auth/register/phone`；
- `@Valid @RequestBody`：JSON 反序列化 + 触发 `PhoneRegisterRequest` 的字段校验；
- 业务逻辑全部委托给 `AuthService`，Controller 只负责「接收 → 校验 → 调用 → 包装返回」。

### 步骤 3：Service 层入口（`AuthService.registerByPhone`）

`src/main/java/com/crm/service/AuthService.java`：

```java
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserRepository sysUserRepository;
    private final UserAuthRepository userAuthRepository;
    private final SmsVerificationService smsVerificationService;
    private final PasswordEncoder passwordEncoder;   // BCryptPasswordEncoder
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthTokenResponse registerByPhone(PhoneRegisterRequest request) {
        String phone = request.getPhone();

        // ① 手机号未注册校验
        if (sysUserRepository.existsByPhone(phone)) {
            throw new BusinessException(ErrorCode.PHONE_ALREADY_REGISTERED);
        }

        // ② 短信验证码校验（通过后标记为已消耗）
        smsVerificationService.verifyCode(phone, SmsScene.REGISTER, request.getSmsCode());

        // ③ 创建用户账号
        SysUser user = new SysUser();
        user.setUserNo(generateUserNo());
        user.setPhone(phone);
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname() : maskPhone(phone));
        user.setAvatarUrl(request.getAvatarUrl());
        user.setStatus(UserStatus.ACTIVE);
        user.setSystemRole(SystemRole.NONE);
        sysUserRepository.save(user);

        // ④ 创建手机号认证记录（密码 bcrypt 加密）
        UserAuth auth = new UserAuth();
        auth.setUserId(user.getUserId());
        auth.setAuthType(AuthType.PHONE);
        auth.setIdentifier(phone);
        auth.setCredential(passwordEncoder.encode(request.getPassword()));
        auth.setVerifiedAt(LocalDateTime.now());
        auth.setStatus(1);
        userAuthRepository.save(auth);

        // ⑤ 生成 JWT Token 返回
        return buildAuthToken(user);
    }
}
```

**关键点：`@Transactional`（事务一致性）**
`验证码消耗 + 用户创建 + 认证创建` 处于同一数据库事务中。任一步骤抛异常（如唯一约束冲突），**整个事务回滚**：不会出现「用户建了但验证码没用掉」或「验证码消耗了但用户没建成」的中间状态。

### 步骤 3.1 手机号未注册校验

```java
if (sysUserRepository.existsByPhone(phone)) {
    throw new BusinessException(ErrorCode.PHONE_ALREADY_REGISTERED);  // code=1001
}
```

- 底层由 Spring Data JPA 依据方法名生成 `SELECT COUNT(*) FROM sys_users WHERE phone = ?`；
- 已注册 → 抛业务异常，由 `GlobalExceptionHandler` 转为 `{code:1001, message:"该手机号已注册"}`。

### 步骤 3.2 短信验证码校验

```java
smsVerificationService.verifyCode(phone, SmsScene.REGISTER, request.getSmsCode());
```

校验逻辑详见「步骤 4」。校验**通过**时会将该验证码记录标记为已消耗（`used_at` 写入当前时间），防止验证码被重复使用。

### 步骤 3.3 创建用户（`SysUser`）

| 字段 | 值 | 说明 |
| :--- | :--- | :--- |
| `user_no` | `USR` + `yyyyMMddHHmmssSSS` + 3 位随机数 | 全局唯一业务编号，冲突时重试 |
| `phone` | 请求参数 | 写入用户主手机号 |
| `nickname` | 请求昵称，缺省用掩码手机号 `138****8000` | 保证非空 |
| `status` | `ACTIVE` | 状态枚举 → SMALLINT(1) |
| `system_role` | `NONE` | 注册默认为普通用户 |

`user_no` 生成逻辑（带唯一性重试）：

```java
private String generateUserNo() {
    for (int i = 0; i < 10; i++) {
        String userNo = "USR" + DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(LocalDateTime.now())
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
        if (!sysUserRepository.existsByUserNo(userNo)) {
            return userNo;
        }
    }
    throw new BusinessException(ErrorCode.INTERNAL_ERROR, "用户编号生成失败，请重试");
}
```

`SysUser` 实体的 `@PrePersist` 回调会在插入前自动补齐 `created_at`、`updated_at` 及默认值：

```java
@PrePersist
void onCreate() {
    LocalDateTime now = LocalDateTime.now();
    if (createdAt == null) createdAt = now;
    if (updatedAt == null) updatedAt = now;
    if (status == null) status = UserStatus.ACTIVE;
    if (systemRole == null) systemRole = SystemRole.NONE;
}
```

### 步骤 3.4 创建手机号认证（`UserAuth`）

| 字段 | 值 | 说明 |
| :--- | :--- | :--- |
| `user_id` | 上一步生成的 `user.getUserId()` | 关联用户 |
| `auth_type` | `PHONE` | 手机号认证 |
| `identifier` | 手机号 | 唯一约束 `(auth_type, identifier)` |
| `credential` | `passwordEncoder.encode(password)` | **BCrypt**（自动加盐，每次加密结果不同） |
| `verified_at` | 当前时间 | 手机号已通过短信验证 |
| `status` | 1 | 启用 |

> 密码**绝不明文存储**。`BCryptPasswordEncoder` 是 Spring Security 提供的 BCrypt 实现，登录时用 `passwordEncoder.matches(rawPwd, hash)` 校验。

### 步骤 3.5 签发 JWT 并组装响应

```java
private AuthTokenResponse buildAuthToken(SysUser user) {
    LoginUser loginUser = new LoginUser(user.getUserId(), user.getUserNo(), user.getPhone(), user.getSystemRole());
    AuthTokenResponse response = new AuthTokenResponse();
    response.setToken(jwtTokenProvider.generateToken(loginUser));      // JWT
    response.setTokenType("Bearer");
    response.setExpiresIn(jwtTokenProvider.getExpirationSeconds());    // 86400 秒
    response.setUserId(user.getUserId());
    response.setUserNo(user.getUserNo());
    response.setPhoneMasked(maskPhone(user.getPhone()));               // 138****8000
    response.setSystemRole(user.getSystemRole());
    return response;
}
```

JWT 生成（`JwtTokenProvider`，HS256）：

```java
return Jwts.builder()
        .subject(String.valueOf(user.getUserId()))                                   // 主体：userId
        .claim("userNo", user.getUserNo())                                           // 自定义载荷
        .claim("phone", user.getPhone())
        .claim("systemRole", user.getSystemRole() == null ? "NONE" : user.getSystemRole().name())
        .issuedAt(now)                                                               // 签发时间
        .expiration(expiry)                                                          // 过期时间（24h）
        .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))       // HS256 签名
        .compact();
```

### 步骤 4：短信验证码（`SmsVerificationService`）

#### 4.1 发送验证码 `sendVerificationCode`（前置接口实现）

`POST /api/sms/verification-code` 的实现：

```java
@Transactional
public void sendVerificationCode(SendSmsCodeRequest request, String ipAddress) {
    String phone = request.getPhone();
    SmsScene scene = request.getScene();

    // 注册场景：手机号已注册则提示直接登录
    if (scene == SmsScene.REGISTER && sysUserRepository.existsByPhone(phone)) {
        throw new BusinessException(ErrorCode.PHONE_ALREADY_REGISTERED);   // 1001
    }

    // 60 秒限频
    LocalDateTime now = LocalDateTime.now();
    long recentCount = smsVerificationRepository.countByPhoneAndSceneAndCreatedAtAfter(
            phone, scene, now.minusSeconds(60));
    if (recentCount > 0) {
        throw new BusinessException(ErrorCode.SMS_SEND_TOO_FREQUENT);      // 1101
    }

    // Mock 通道固定发送 000000（真实网关接入前）
    String code = MOCK_CODE;   // "000000"
    sendCode(phone, code);     // 目前仅打印日志

    SmsVerification verification = new SmsVerification();
    verification.setPhone(phone);
    verification.setScene(scene);
    verification.setCodeHash(hash(code));                                  // SHA-256 hex
    verification.setExpiredAt(now.plusMinutes(5));                         // 5 分钟有效
    verification.setAttempts(0);
    verification.setIpAddress(ipAddress);
    smsVerificationRepository.save(verification);
}
```

**关键点：**
- **Mock 通道**：`sendCode()` 暂不真正发短信，仅打印日志（`[Mock 短信通道] ...验证码: 000000`）；后续接入真实短信网关只改这一个方法；
- **限频防刷**：`countByPhoneAndSceneAndCreatedAtAfter(phone, scene, now-60s)`，同手机号同场景 60 秒内只能发一次；
- **验证码不落明文**：`hash()` 使用 SHA-256 摘要（hex）存入 `code_hash`，防止数据库泄露后验证码被直接读取。

#### 4.2 校验验证码 `verifyCode`

```java
@Transactional
public void verifyCode(String phone, SmsScene scene, String code) {
    SmsVerification verification = smsVerificationRepository
            .findTopByPhoneAndSceneOrderByCreatedAtDesc(phone, scene)      // 取最近一条
            .orElseThrow(() -> new BusinessException(ErrorCode.SMS_CODE_INVALID));   // 1102 不存在

    if (verification.getUsedAt() != null) {                               // 1102 已使用
        throw new BusinessException(ErrorCode.SMS_CODE_INVALID);
    }
    if (verification.getExpiredAt().isBefore(LocalDateTime.now())) {      // 1103 过期
        throw new BusinessException(ErrorCode.SMS_CODE_EXPIRED);
    }
    if (verification.getAttempts() >= 5) {                                // 1104 超限
        throw new BusinessException(ErrorCode.SMS_CODE_ATTEMPT_LIMIT);
    }
    if (!verification.getCodeHash().equals(hash(code))) {                 // 内容不匹配
        verification.setAttempts(verification.getAttempts() + 1);
        smsVerificationRepository.save(verification);
        if (verification.getAttempts() >= 5) {
            throw new BusinessException(ErrorCode.SMS_CODE_ATTEMPT_LIMIT);  // 1104
        }
        throw new BusinessException(ErrorCode.SMS_CODE_INVALID);            // 1102
    }

    verification.setUsedAt(LocalDateTime.now());   // 校验通过 → 标记已消耗
    smsVerificationRepository.save(verification);
}
```

校验顺序严格遵循设计文档 UC-001 主流程：**存在 → 未过期 → 未消耗 → 尝试次数未超限 → 匹配正确**。

### 步骤 5：统一响应与异常处理

#### 5.1 统一响应结构（`ApiResponse`）

所有接口统一返回三字段结构，便于前端统一处理：

```json
{ "code": 0, "message": "success", "data": { ... } }
```

- `code = 0` 成功；非 0 为业务错误码
- 成功：`ApiResponse.ok(data)`；失败：`ApiResponse.fail(code, message)`

#### 5.2 全局异常处理器（`GlobalExceptionHandler`）

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常 → 返回对应业务错误码（HTTP 200，业务层判定） */
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        return ApiResponse.fail(e.getCode(), e.getMessage());
    }

    /** 参数校验失败（@Valid 请求体）→ 400 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "请求参数错误";
        return ApiResponse.fail(ErrorCode.BAD_REQUEST.getCode(), message);   // code=400
    }

    /** 未知异常 → 500 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return ApiResponse.fail(ErrorCode.INTERNAL_ERROR.getCode(), ErrorCode.INTERNAL_ERROR.getMessage());
    }
}
```

因此业务代码中只需 `throw new BusinessException(ErrorCode.XXX)`，即可统一返回 `{code, message}`，**无需每个方法写 try-catch**。

---

## 6. 请求 / 响应示例

### 6.1 第一步：发送验证码

```http
POST /api/sms/verification-code
Content-Type: application/json

{"phone": "13800138000", "scene": "REGISTER"}
```

成功响应：

```json
{ "code": 0, "message": "success", "data": null }
```

> 应用日志中会打印 `[Mock 短信通道] 向手机号 13800138000 发送验证码: 000000`。

### 6.2 第二步：手机号 + 验证码注册

```http
POST /api/auth/register/phone
Content-Type: application/json

{
  "phone": "13800138000",
  "smsCode": "000000",
  "password": "abc12345",
  "nickname": "张三"
}
```

成功响应（注册即登录，返回 JWT）：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI1IiwidXNlck5vIjoiVVNSMjAyNjA4MjIxNTU3MTk2MDM3NDgi...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "userId": 5,
    "userNo": "USR20260822155719603748",
    "phoneMasked": "138****8000",
    "systemRole": "NONE"
  }
}
```

### 6.3 常见失败响应

| 场景 | HTTP 状态 | code | message |
| :--- | :--- | :--- | :--- |
| 手机号已注册（发码/注册时） | 200 | 1001 | 该手机号已注册 |
| 手机号格式错误 | 200 | 400 | 手机号格式不正确 |
| 验证码长度错误 | 200 | 400 | 验证码长度不正确 |
| 密码强度不足 | 200 | 400 | 密码需包含字母和数字 |
| 60 秒内重复发送 | 200 | 1101 | 验证码发送过于频繁，请稍后再试 |
| 验证码错误 | 200 | 1102 | 验证码错误或已失效 |
| 验证码过期 | 200 | 1103 | 验证码已过期，请重新获取 |
| 验证码错误次数超限 | 200 | 1104 | 验证码错误次数过多，已失效 |

> 注：业务错误以 HTTP 200 返回、通过 `code` 区分，是前后端约定的统一风格；真正未处理异常才返回 HTTP 500。

---

## 7. 测试

### 7.1 自动化集成测试（`AuthRegistrationIntegrationTest`）

`src/test/java/com/crm/AuthRegistrationIntegrationTest.java`，基于 `@SpringBootTest` + `@AutoConfigureMockMvc`，**连接真实 PostgreSQL** 验证全流程：

| 测试用例 | 验证内容 |
| :--- | :--- |
| `phoneRegisterFlowShouldSucceed` | 发送验证码 → `000000` 注册 → 返回 token、userNo、`phoneMasked`、`systemRole=NONE` |
| `registerWithWrongCodeShouldFail` | 错误验证码注册失败，`code=1102` |
| `resendCodeWithin60sShouldBeRateLimited` | 60 秒内重复发送被限频，`code=1101` |
| `registerSamePhoneTwiceShouldFail` | 重复注册提示已注册，`code=1001` |

运行方式：

```bash
mvn test
```

### 7.2 手工测试

**方式一：浏览器测试页** `http://localhost:8080/test-register.html`
提供「发送验证码」「注册」两个表单，直接点击即可看到完整请求响应。

**方式二：命令行（PowerShell）**

```powershell
# 1. 发送验证码
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/sms/verification-code" `
  -ContentType "application/json" `
  -Body '{"phone":"13800138000","scene":"REGISTER"}'

# 2. 注册（Mock 验证码固定 000000）
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/auth/register/phone" `
  -ContentType "application/json" `
  -Body '{"phone":"13800138000","smsCode":"000000","password":"abc12345"}'
```

**方式三：curl**

```bash
curl -X POST http://localhost:8080/api/sms/verification-code \
  -H "Content-Type: application/json" \
  -d '{"phone":"13800138000","scene":"REGISTER"}'

curl -X POST http://localhost:8080/api/auth/register/phone \
  -H "Content-Type: application/json" \
  -d '{"phone":"13800138000","smsCode":"000000","password":"abc12345"}'
```

### 7.3 注册后的数据库验证

```sql
-- 用户与认证记录
SELECT user_id, user_no, phone, status, system_role FROM sys_users ORDER BY user_id DESC LIMIT 3;
SELECT auth_id, user_id, auth_type, identifier, left(credential, 20) AS pwd_bcrypt FROM user_auths ORDER BY auth_id DESC LIMIT 3;

-- 验证码记录（注册成功的 used 应为 t）
SELECT id, phone, scene, attempts, used_at IS NOT NULL AS used FROM sms_verifications ORDER BY id DESC LIMIT 3;
```

---

## 8. 安全与注意事项

1. **密码安全**：使用 BCrypt（自动加盐、抗彩虹表），登录时用 `matches()` 比对；注册成功后可继续实现「手机号密码登录」复用该哈希。
2. **验证码安全**：
   - 验证码**不落明文**，数据库只存 SHA-256 哈希；
   - 5 分钟有效期 + 60 秒发送限频 + 最多 5 次错误尝试，从时效、频率、次数三个维度防刷；
   - 验证码**一次性**，校验通过立即标记 `used_at`，防止重放。
3. **事务一致性**：`@Transactional` 保证验证码消耗与用户/认证创建原子提交，任何一步失败全部回滚。
4. **唯一性约束**：`sys_users.user_no`、`user_auths(auth_type, identifier)` 由数据库唯一索引兜底；`user_no` 生成带重试。
5. **隐私保护**：响应中手机号统一掩码输出（`138****8000`），不返回明文手机号。
6. **JWT**：HS256 签名密钥必须 ≥ 32 字节，生产环境务必用 `JWT_SECRET` 环境变量覆盖默认值；Token 有效期 24 小时（可按需缩短）。
7. **Mock 短信**：当前 `sendCode()` 只打日志、验证码固定 `000000`；**上线前**必须接入真实短信网关并改为随机验证码（如 6 位数字），同时将 `MOCK_CODE` 逻辑移除。
8. **安全放行范围**：`/api/auth/register/**`、`/api/auth/login/**`、`/api/sms/**` 为匿名白名单；`/api/auth/bind/**`、`/api/auth/password/set` 等需要登录的接口已在白名单之外，必须携带 JWT 访问。
9. **测试数据**：集成测试使用随机手机号（`139` + 8 位随机数），不会与真实数据冲突；如需清理测试用户，可直接删除 `sys_users`、`user_auths`、`sms_verifications` 中的测试记录。

---

> 本文档与代码保持同步；后续若调整注册流程（如接入真实短信、增加图形验证码、接入微信注册），请同步更新本文档「步骤 3 / 4」小节。





