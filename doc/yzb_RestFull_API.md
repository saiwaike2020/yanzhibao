# YZB CRM 系统 RESTful API 文档

> - 基于当前项目实现自动生成（Spring Boot 3.2 + JWT + PostgreSQL 16）
> - Base URL：`http://localhost:8080`
> - 格式：每个接口包含 **URL / description / parameters / return / example**

---

## 1. 通用约定

### 1.1 鉴权方式

除标注「无需登录」的接口外，所有接口均需携带请求头：

```
Authorization: Bearer <JWT Token>
```

Token 由注册 / 登录接口返回（`data.token`），默认有效期 24 小时。

### 1.2 统一响应结构（`ApiResponse<T>`）

```json
{
  "code": 0,          // 0=成功；非 0=业务错误（HTTP 200 返回）
  "message": "success",
  "data": { ... }     // 业务数据，可为 null
}
```

| HTTP 状态 | 说明 |
| :--- | :--- |
| 200 | 成功，或业务错误（通过 `code` 区分） |
| 400 | 参数校验失败 |
| 401 | 未认证或登录已过期 |
| 403 | 无权限执行该操作 |
| 500 | 系统内部错误 |

### 1.3 分页响应（`PageResponse<T>`）

分页接口的 `data` 结构：

```json
{ "items": [ ... ], "total": 100, "page": 1, "size": 20 }
```

### 1.4 常用错误码

| code | 说明 |
| :--- | :--- |
| 1001 / 1002 | 手机号已注册 / 未注册 |
| 1005 / 1006 / 1007 | 密码错误 / 账号禁用 / 账号注销 |
| 1101~1105 | 验证码发送频繁 / 无效 / 过期 / 次数超限 / 缺手机号 |
| 1201~1211 | 企业、成员、审批相关 |
| 1301~1305 | 分组、管理授权相关 |
| 1401~1412 | 资源、所有权、权限相关 |
| 1501 | 消息不存在 |
| 1601~1604 | 系统参数、存储配额相关 |
| 1701~1704 | 文件上传 / 处理相关 |
| 1801~1802 | 审计权限相关 |

---

## 2. 认证接口（AuthController）

### 2.1 手机号验证码注册（UC-001）

- **URL**：`POST /api/auth/register/phone`
- **description**：手机号 + 短信验证码注册，成功后自动登录并返回 JWT Token
- **auth**：无需登录　**前置**：发送验证码（scene=REGISTER）

**parameters**（JSON Body）：

| 参数 | 类型 | 必填 | 说明 |
| :--- | :--- | :--- | :--- |
| `phone` | String | 是 | 手机号（`^1[3-9]\d{9}$`） |
| `smsCode` | String | 是 | 短信验证码（4-6 位） |
| `password` | String | 是 | 登录密码（≥8 位，含字母和数字） |
| `nickname` | String | 否 | 昵称 |
| `avatarUrl` | String | 否 | 头像 URL |

**return**：`ApiResponse<AuthTokenResponse>`（`token`/`tokenType`/`expiresIn`/`userId`/`userNo`/`phoneMasked`/`systemRole`）
**example**：

```json
// request
{ "phone": "13800138000", "smsCode": "000000", "password": "abc12345", "nickname": "张三" }

// response
{
  "code": 0, "message": "success",
  "data": { "token": "eyJhbGciOiJIUzI1NiJ9...", "tokenType": "Bearer", "expiresIn": 86400,
            "userId": 1, "userNo": "USR20260827123456789001",
            "phoneMasked": "138****8000", "systemRole": "NONE" }
}
```

### 2.2 微信快速注册（UC-002）

- **URL**：`POST /api/auth/register/wechat`
- **description**：微信授权 + 手机号 + 短信验证码注册
- **auth**：无需登录

**parameters**（JSON Body）：`wechatCode`(是)、`state`(是)、`phone`(是)、`smsCode`(是)、`password`(否，跳过则仅可微信登录)
**return**：`ApiResponse<AuthTokenResponse>`

```json
// request
{ "wechatCode": "wx_code", "state": "abc123", "phone": "13800138000", "smsCode": "000000", "password": "abc12345" }
// response
{ "code": 0, "message": "success", "data": { "token": "...", "tokenType": "Bearer", "expiresIn": 86400, "userId": 1, "userNo": "USR...", "phoneMasked": "138****8000", "systemRole": "NONE" } }
```

### 2.3 手机号 + 密码登录（UC-003）

- **URL**：`POST /api/auth/login/phone`
- **description**：手机号密码登录，成功返回 JWT Token
- **auth**：无需登录

**parameters**（JSON Body）：`phone`(是)、`password`(是)
**return**：`ApiResponse<AuthTokenResponse>`

```json
// request
{ "phone": "13800138000", "password": "abc12345" }
// response
{ "code": 0, "message": "success", "data": { "token": "...", "tokenType": "Bearer", "expiresIn": 86400, "userId": 1, "userNo": "USR...", "phoneMasked": "138****8000", "systemRole": "NONE" } }
// 密码错误
{ "code": 1005, "message": "手机号或密码错误", "data": null }
```

### 2.4 生成微信扫码登录二维码

- **URL**：`GET /api/auth/login/wechat/qrcode`
- **description**：生成微信扫码登录二维码（含一次性 state）
- **auth**：无需登录　**parameters**：无
- **return**：`ApiResponse<WechatQrcodeResponse>`（`qrcodeUrl`/`state`/`expiresIn`）

### 2.5 微信扫码登录回调（UC-004）

- **URL**：`POST /api/auth/login/wechat/callback`
- **description**：微信扫码登录回调；未绑定则进入绑定/注册流程
- **auth**：无需登录
- **parameters**（JSON Body）：`code`(是，微信授权 code)、`state`(是，一次性 state)
- **return**：`ApiResponse<AuthTokenResponse>`

```json
// request
{ "code": "wx_code", "state": "abc123" }
// response
{ "code": 0, "message": "success", "data": { "token": "...", "userId": 1 } }
```

### 2.6 绑定微信

- **URL**：`POST /api/auth/bind/wechat`
- **description**：已登录用户绑定微信
- **auth**：需登录
- **parameters**（JSON Body）：`code`(是，微信授权 code)、`state`(是，一次性 state)
- **return**：`ApiResponse<Void>`（`data=null`）

### 2.7 绑定 / 更换手机号

- **URL**：`POST /api/auth/bind/phone`
- **description**：已登录用户绑定或更换手机号（短信验证码校验）
- **auth**：需登录　**前置**：发送验证码（scene=BIND_PHONE）
- **parameters**（JSON Body）：`newPhone`(是，新手机号)、`smsCode`(是，发送到新手机号的验证码)
- **return**：`ApiResponse<Void>`

### 2.8 解绑认证方式

- **URL**：`POST /api/auth/unbind`
- **description**：解绑认证方式（至少保留一种登录方式）
- **auth**：需登录
- **parameters**（JSON Body）：`authType`(是，PHONE / WECHAT)、`identifier`(否，默认解绑当前用户该类型唯一绑定)
- **return**：`ApiResponse<Void>`

### 2.9 设置 / 修改登录密码

- **URL**：`POST /api/auth/password/set`
- **description**：首次设置密码或修改密码（修改需校验原密码）
- **auth**：需登录
- **parameters**（JSON Body）：`newPassword`(是，≥8 位含字母和数字)、`oldPassword`(否，修改时必填)
- **return**：`ApiResponse<Void>`

### 2.10 忘记密码 / 重置密码（UC-025）

- **URL**：`POST /api/auth/password/reset`
- **description**：通过已注册手机号 + 短信验证码重置登录密码，成功后不自动登录
- **auth**：无需登录　**前置**：发送验证码（scene=RESET_PWD）
- **parameters**（JSON Body）：`phone`(是)、`smsCode`(是)、`newPassword`(是，≥8 位含字母和数字)
- **return**：`ApiResponse<Void>`

```json
// request
{ "phone": "13800138000", "smsCode": "000000", "newPassword": "xyz67890" }
// response
{ "code": 0, "message": "success", "data": null }
```

---

## 3. 短信验证码接口（SmsVerificationController）

### 3.1 发送短信验证码

- **URL**：`POST /api/sms/verification-code`
- **description**：向指定手机号发送短信验证码（Mock 通道固定 000000）
- **auth**：无需登录
- **规则**：验证码 5 分钟有效；同一手机号同一场景 60 秒限频；最多 5 次错误尝试

**parameters**（JSON Body）：

| 参数 | 类型 | 必填 | 说明 |
| :--- | :--- | :--- | :--- |
| `phone` | String | 是 | 手机号（`^1[3-9]\d{9}$`） |
| `scene` | String | 是 | REGISTER / LOGIN / BIND_PHONE / ADMIN_ROLE_CHANGE / GROUP_MANAGE_GRANT / AUDITOR_ASSIGN / RESET_PWD |

**return**：`ApiResponse<Void>`

```json
// request
{ "phone": "13800138000", "scene": "REGISTER" }
// response
{ "code": 0, "message": "success", "data": null }
```

---

## 4. 用户个人中心接口（UserController）

### 4.1 获取当前登录用户信息

- **URL**：`GET /api/users/me`
- **auth**：需登录　**parameters**：无
- **return**：`ApiResponse<UserProfileResponse>`（`userId`/`userNo`/`phoneMasked`/`nickname`/`avatarUrl`/`status`/`systemRole`/`createdAt`）

```json
// response
{ "code": 0, "message": "success", "data": { "userId": 1, "userNo": "USR...", "phoneMasked": "138****8000", "nickname": "张三", "avatarUrl": null, "status": "ACTIVE", "systemRole": "NONE", "createdAt": "2026-08-27T10:00:00" } }
```

### 4.2 更新个人资料

- **URL**：`PUT /api/users/me`
- **auth**：需登录
- **parameters**（JSON Body）：`nickname`(否，≤64 字符)、`avatarUrl`(否，≤255 字符)
- **return**：`ApiResponse<UserProfileResponse>`

```json
// request
{ "nickname": "新昵称", "avatarUrl": "https://example.com/a.png" }
```

### 4.3 账号安全信息

- **URL**：`GET /api/users/me/security`
- **auth**：需登录　**parameters**：无
- **return**：`ApiResponse<AccountSecurityResponse>`（`phoneMasked`/`boundAuthTypes`/`hasPassword`）

```json
// response
{ "code": 0, "message": "success", "data": { "phoneMasked": "138****8000", "boundAuthTypes": ["PHONE"], "hasPassword": true } }
```

### 4.4 查看指定用户公开信息

- **URL**：`GET /api/users/{userId}`
- **auth**：需登录
- **parameters**（Path）：`userId`(是，Long)
- **return**：`ApiResponse<UserProfileResponse>`

---

## 5. 企业接口（CompanyController）

### 5.1 创建企业（UC-005）

- **URL**：`POST /api/companies`
- **auth**：需登录
- **parameters**（JSON Body）：`name`(是，≤128 字符)、`logoUrl`(否)
- **return**：`ApiResponse<CompanyResponse>`（`companyId`/`companyNo`/`name`/`logoUrl`/`ownerUserId`/`status`/`createdAt`）

```json
// request
{ "name": "示例科技", "logoUrl": "https://example.com/logo.png" }
// response
{ "code": 0, "message": "success", "data": { "companyId": 1, "companyNo": "CPY20260827...", "name": "示例科技", "logoUrl": "...", "ownerUserId": 1, "status": "ACTIVE", "createdAt": "2026-08-27T10:00:00" } }
```

### 5.2 我的企业列表

- **URL**：`GET /api/companies`
- **auth**：需登录　**parameters**：无
- **return**：`ApiResponse<List<CompanyResponse>>`

### 5.3 企业详情

- **URL**：`GET /api/companies/{companyId}`
- **auth**：需登录
- **parameters**（Path）：`companyId`(是，Long)
- **return**：`ApiResponse<CompanyResponse>`

### 5.4 更新企业信息

- **URL**：`PUT /api/companies/{companyId}`
- **description**：更新企业名称 / Logo（仅企业管理员）
- **auth**：需登录
- **parameters**（Path）：`companyId`(是)；Body：`name`(是)、`logoUrl`(否)
- **return**：`ApiResponse<CompanyResponse>`

### 5.5 发起企业所有权转让申请（UC-033）

- **URL**：`POST /api/companies/{companyId}/transfer`
- **description**：企业所有者发起所有权转让申请，经系统管理员或有权限的审计人员审批后生效
- **auth**：需登录
- **parameters**（Path）：`companyId`(是)；Body：`newOwnerUserId`(是，需为企业活跃成员)
- **return**：`ApiResponse<Void>`

### 5.6 发起企业注销申请（UC-018 / UC-033）

- **URL**：`POST /api/companies/{companyId}/dissolve`
- **description**：企业所有者发起注销申请，经审批通过后企业状态置为 DISSOLVED
- **auth**：需登录
- **parameters**（Path）：`companyId`(是)
- **return**：`ApiResponse<Void>`

---

## 6. 企业成员接口（CompanyMemberController）

### 6.1 邀请成员加入企业（UC-006 / UC-027）

- **URL**：`POST /api/companies/{companyId}/members/invite`
- **description**：企业管理员邀请已注册用户加入，成功后向被邀请用户发送邀请消息
- **auth**：需登录
- **parameters**（Path）：`companyId`(是)；Body：`phone`(是，被邀请人手机号)、`note`(否，≤255 字符)
- **return**：`ApiResponse<Void>`

### 6.2 用户申请加入企业（UC-026）

- **URL**：`POST /api/companies/{companyId}/members/apply`
- **description**：用户申请加入企业，创建待审批记录（INVITED）并通知企业管理员
- **auth**：需登录
- **parameters**（Path）：`companyId`(是)
- **return**：`ApiResponse<Void>`

### 6.3 批准加入申请（UC-032）

- **URL**：`POST /api/companies/{companyId}/members/{memberId}/approve`
- **description**：企业所有者/管理员批准加入申请，成员状态 INVITED→ACTIVE
- **auth**：需登录
- **parameters**（Path）：`companyId`(是)、`memberId`(是)
- **return**：`ApiResponse<Void>`

### 6.4 拒绝加入申请（UC-032）

- **URL**：`POST /api/companies/{companyId}/members/{memberId}/reject`
- **description**：企业所有者/管理员拒绝加入申请，成员状态 INVITED→EXITED
- **auth**：需登录
- **parameters**（Path）：`companyId`(是)、`memberId`(是)
- **return**：`ApiResponse<Void>`

### 6.5 接受企业邀请（UC-037）

- **URL**：`POST /api/companies/{companyId}/members/{memberId}/accept`
- **description**：被邀请用户接受邀请，成员状态 INVITED→ACTIVE
- **auth**：需登录
- **parameters**（Path）：`companyId`(是)、`memberId`(是)
- **return**：`ApiResponse<Void>`

### 6.6 成员列表

- **URL**：`GET /api/companies/{companyId}/members?page=1&size=20`
- **auth**：需登录
- **parameters**：`companyId`(Path)；`page`(Query，默认 1)、`size`(Query，默认 20)
- **return**：`ApiResponse<PageResponse<CompanyMemberResponse>>`（`memberId`/`companyId`/`userId`/`userNo`/`nickname`/`phoneMasked`/`role`/`status`/`joinedAt`）

### 6.7 成员详情

- **URL**：`GET /api/companies/{companyId}/members/{memberId}`
- **auth**：需登录
- **parameters**（Path）：`companyId`(是)、`memberId`(是)
- **return**：`ApiResponse<CompanyMemberResponse>`

### 6.8 变更企业成员角色（UC-022）

- **URL**：`PUT /api/companies/{companyId}/members/{memberId}/role`
- **description**：设置/取消企业管理员（高危敏感操作，需操作者手机短信验证码二次校验）
- **auth**：需登录　**前置**：发送验证码（scene=ADMIN_ROLE_CHANGE）
- **parameters**（Path）：`companyId`(是)、`memberId`(是)；Body：`role`(是，ADMIN / MEMBER)、`smsCode`(是)
- **return**：`ApiResponse<Void>`

### 6.9 禁用成员

- **URL**：`PUT /api/companies/{companyId}/members/{memberId}/disable`
- **auth**：需登录
- **parameters**（Path）：`companyId`(是)、`memberId`(是)
- **return**：`ApiResponse<Void>`

### 6.10 恢复成员

- **URL**：`PUT /api/companies/{companyId}/members/{memberId}/restore`
- **auth**：需登录
- **parameters**（Path）：`companyId`(是)、`memberId`(是)
- **return**：`ApiResponse<Void>`

### 6.11 移除成员（UC-016）

- **URL**：`DELETE /api/companies/{companyId}/members/{memberId}`
- **description**：移除成员（逻辑失效，设置 valid_until）
- **auth**：需登录
- **parameters**（Path）：`companyId`(是)、`memberId`(是)
- **return**：`ApiResponse<Void>`

### 6.12 退出企业（UC-017）

- **URL**：`POST /api/companies/{companyId}/members/leave`
- **description**：用户退出企业（逻辑失效，设置 valid_until）
- **auth**：需登录
- **parameters**（Path）：`companyId`(是)
- **return**：`ApiResponse<Void>`

---

## 7. 企业分组接口（GroupController）

### 7.1 创建一级分组（UC-007）

- **URL**：`POST /api/companies/{companyId}/groups`
- **description**：仅企业管理员可创建一级分组（parentGroupId 必须为空）
- **auth**：需登录
- **parameters**（Path）：`companyId`(是)；Body：`name`(是，≤64 字符)、`description`(否)、`parentGroupId`(否，当前必须为 null)
- **return**：`ApiResponse<GroupResponse>`（`groupId`/`companyId`/`parentGroupId`/`name`/`description`/`status`/`createdBy`/`createdAt`）

### 7.2 分组列表

- **URL**：`GET /api/companies/{companyId}/groups`
- **auth**：需登录
- **parameters**（Path）：`companyId`(是)
- **return**：`ApiResponse<List<GroupResponse>>`

### 7.3 分组详情

- **URL**：`GET /api/companies/{companyId}/groups/{groupId}`
- **auth**：需登录
- **parameters**（Path）：`companyId`(是)、`groupId`(是)
- **return**：`ApiResponse<GroupResponse>`

### 7.4 编辑分组

- **URL**：`PUT /api/companies/{companyId}/groups/{groupId}`
- **auth**：需登录
- **parameters**（Path）：`companyId`(是)、`groupId`(是)；Body：`name`(是)、`description`(否)
- **return**：`ApiResponse<GroupResponse>`

### 7.5 删除分组

- **URL**：`DELETE /api/companies/{companyId}/groups/{groupId}`
- **auth**：需登录
- **parameters**（Path）：`companyId`(是)、`groupId`(是)
- **return**：`ApiResponse<Void>`

---

## 8. 分组成员接口（GroupMemberController）

### 8.1 添加分组成员（UC-009）

- **URL**：`POST /api/groups/{groupId}/members`
- **description**：企业管理员或该分组的分组管理员可添加企业成员入组
- **auth**：需登录
- **parameters**（Path）：`groupId`(是)；Body：`userId`(是，需为企业成员)
- **return**：`ApiResponse<Void>`

### 8.2 分组成员列表

- **URL**：`GET /api/groups/{groupId}/members`
- **auth**：需登录
- **parameters**（Path）：`groupId`(是)
- **return**：`ApiResponse<List<GroupMemberResponse>>`（`groupMemberId`/`groupId`/`userId`/`userNo`/`nickname`/`phoneMasked`/`role`/`joinedAt`）

### 8.3 移除分组成员（UC-016）

- **URL**：`DELETE /api/groups/{groupId}/members/{groupMemberId}`
- **auth**：需登录
- **parameters**（Path）：`groupId`(是)、`groupMemberId`(是)
- **return**：`ApiResponse<Void>`

---

## 9. 分级授权接口（ManagementDelegationController）

### 9.1 授予分组管理权（设置分组管理员，UC-008 / UC-015）

- **URL**：`POST /api/companies/{companyId}/delegations`
- **description**：企业管理员将指定分组的管理权授予成员（角色提权敏感操作，需操作者手机短信验证码）
- **auth**：需登录　**前置**：发送验证码（scene=GROUP_MANAGE_GRANT）
- **parameters**（Path）：`companyId`(是)；Body：`groupId`(是)、`granteeUserId`(是)、`smsCode`(是)
- **return**：`ApiResponse<DelegationResponse>`（`delegationId`/`companyId`/`groupId`/`granteeUserId`/`grantedBy`/`scope`/`status`/`createdAt`）

### 9.2 管理授权列表

- **URL**：`GET /api/companies/{companyId}/delegations`
- **auth**：需登录
- **parameters**（Path）：`companyId`(是)
- **return**：`ApiResponse<List<DelegationResponse>>`

### 9.3 授权详情

- **URL**：`GET /api/companies/{companyId}/delegations/{delegationId}`
- **auth**：需登录
- **parameters**（Path）：`companyId`(是)、`delegationId`(是)
- **return**：`ApiResponse<DelegationResponse>`

### 9.4 撤销授权（移除分组管理员）

- **URL**：`DELETE /api/companies/{companyId}/delegations/{delegationId}`
- **auth**：需登录
- **parameters**（Path）：`companyId`(是)、`delegationId`(是)
- **return**：`ApiResponse<Void>`

---

## 10. 资源接口（ResourceController）

### 10.1 创建资源（UC-010）

- **URL**：`POST /api/resources`
- **description**：创建资料库 / 文件夹 / 文件；企业用户可指定所有权归属个人或企业；创建文件时校验存储配额
- **auth**：需登录

**parameters**（JSON Body）：

| 参数 | 类型 | 必填 | 说明 |
| :--- | :--- | :--- | :--- |
| `resourceType` | String | 是 | LIBRARY / FOLDER / FILE |
| `name` | String | 是 | 资源名称（≤255 字符） |
| `parentResourceId` | Long | 否 | 父资源 ID（资料库为 null） |
| `ownerType` | String | 是 | 所有者类型：USER / COMPANY |
| `ownerId` | Long | 是 | 所有者 ID（归属个人须为本人） |
| `fileSize` | Long | 否 | 文件大小（字节，仅 FILE） |
| `fileType` | String | 否 | 文件 MIME 类型 |
| `fileKey` | String | 否 | 文件相对存储标识（存储服务生成） |

- **return**：`ApiResponse<ResourceResponse>`（`resourceId`/`resourceNo`/`name`/`resourceType`/`parentResourceId`/`creatorUserId`/`fileSize`/`fileType`/`fileKey`/`status`/`createdAt`/`updatedAt`）

```json
// request（创建资料库）
{ "resourceType": "LIBRARY", "name": "产品资料", "ownerType": "USER", "ownerId": 1 }
// response
{ "code": 0, "message": "success", "data": { "resourceId": 10, "resourceNo": "RES20260827...", "name": "产品资料", "resourceType": "LIBRARY", "parentResourceId": null, "creatorUserId": 1, "status": "ACTIVE", "createdAt": "2026-08-27T10:00:00", "updatedAt": "2026-08-27T10:00:00" } }
```

### 10.2 资源树

- **URL**：`GET /api/resources/tree?rootResourceId=1`
- **description**：按权限过滤的资源树
- **auth**：需登录
- **parameters**（Query）：`rootResourceId`(否)
- **return**：`ApiResponse<List<ResourceResponse>>`

### 10.3 资源详情

- **URL**：`GET /api/resources/{resourceId}`
- **auth**：需登录
- **parameters**（Path）：`resourceId`(是)
- **return**：`ApiResponse<ResourceResponse>`

### 10.4 更新资源

- **URL**：`PUT /api/resources/{resourceId}`
- **description**：重命名、移动（需父级写权限）
- **auth**：需登录
- **parameters**（Path）：`resourceId`(是)；Body：`name`(是)、`parentResourceId`(否)
- **return**：`ApiResponse<ResourceResponse>`

### 10.5 删除 / 归档资源

- **URL**：`DELETE /api/resources/{resourceId}`
- **description**：删除资源（需 OWNER 权限）
- **auth**：需登录
- **parameters**（Path）：`resourceId`(是)
- **return**：`ApiResponse<Void>`

---

## 11. 资源所有权接口（ResourceOwnerController）

### 11.1 资源所有者列表

- **URL**：`GET /api/resources/{resourceId}/owners`
- **auth**：需登录
- **parameters**（Path）：`resourceId`(是)
- **return**：`ApiResponse<List<ResourceOwnerResponse>>`（`ownershipId`/`resourceId`/`ownerType`/`ownerId`/`validFrom`/`validUntil`/`grantedBy`/`status`/`createdAt`/`updatedAt`）

### 11.2 登记新所有者

- **URL**：`POST /api/resources/{resourceId}/owners`
- **description**：为资源新增一个所有者（多所有者模型，操作者须为资源有效所有者）
- **auth**：需登录
- **parameters**（Path）：`resourceId`(是)；Body：`ownerType`(是，USER/COMPANY)、`ownerId`(是)、`validFrom`(是，起始可用日期)、`validUntil`(否，空=一直有效)
- **return**：`ApiResponse<ResourceOwnerResponse>`

### 11.3 所有权转让（UC-023）

- **URL**：`POST /api/resources/{resourceId}/owners/transfer`
- **description**：转让后原所有者失去所有权，接收方成为新所有者
- **auth**：需登录
- **parameters**（Path）：`resourceId`(是)；Body：`targetOwnerType`(是)、`targetOwnerId`(是)、`validFrom`(是)、`validUntil`(否)
- **return**：`ApiResponse<ResourceOwnerResponse>`

### 11.4 调整所有权有效期（UC-024）

- **URL**：`PUT /api/resources/{resourceId}/owners/{ownershipId}`
- **auth**：需登录
- **parameters**（Path）：`resourceId`(是)、`ownershipId`(是)；Body：`validFrom`(是)、`validUntil`(否)
- **return**：`ApiResponse<ResourceOwnerResponse>`

### 11.5 撤销所有权

- **URL**：`DELETE /api/resources/{resourceId}/owners/{ownershipId}`
- **description**：撤销所有权（停用 resource_owners 记录）
- **auth**：需登录
- **parameters**（Path）：`resourceId`(是)、`ownershipId`(是)
- **return**：`ApiResponse<Void>`

---

## 12. 资源权限接口（ResourcePermissionController）

### 12.1 资源权限列表

- **URL**：`GET /api/resources/{resourceId}/permissions`
- **auth**：需登录
- **parameters**（Path）：`resourceId`(是)
- **return**：`ApiResponse<List<ResourcePermissionResponse>>`（`permissionId`/`resourceId`/`granteeType`/`granteeId`/`permissionLevel`/`validFrom`/`validUntil`/`grantedBy`/`createdAt`）

### 12.2 分配资源权限给分组（UC-011）

- **URL**：`POST /api/resources/{resourceId}/permissions/groups`
- **description**：为指定分组配置资源权限（操作者须为资源有效所有者）
- **auth**：需登录
- **parameters**（Path）：`resourceId`(是)；Body：`granteeType`(是，GROUP)、`granteeId`(是)、`permissionLevel`(是，READ/WRITE/OWNER)、`validFrom`(是)、`validUntil`(否)
- **return**：`ApiResponse<Void>`

### 12.3 分配资源权限给用户（UC-012）

- **URL**：`POST /api/resources/{resourceId}/permissions/users`
- **auth**：需登录
- **parameters**（Path）：`resourceId`(是)；Body：`granteeType`(是，USER)、`granteeId`(是)、`permissionLevel`(是)、`validFrom`(是)、`validUntil`(否)
- **return**：`ApiResponse<Void>`

```json
// request
{ "granteeType": "USER", "granteeId": 2, "permissionLevel": "READ", "validFrom": "2026-08-27T00:00:00", "validUntil": "2027-08-27T00:00:00" }
```

### 12.4 修改权限级别 / 有效期

- **URL**：`PUT /api/resources/{resourceId}/permissions/{permissionId}`
- **auth**：需登录
- **parameters**（Path）：`resourceId`(是)、`permissionId`(是)；Body：`permissionLevel`(是)、`validFrom`(是)、`validUntil`(否)
- **return**：`ApiResponse<Void>`

### 12.5 撤销资源权限（UC-016）

- **URL**：`DELETE /api/resources/{resourceId}/permissions/{permissionId}`
- **auth**：需登录
- **parameters**（Path）：`resourceId`(是)、`permissionId`(是)
- **return**：`ApiResponse<Void>`

### 12.6 调整原所有者访问权限（UC-034）

- **URL**：`PUT /api/resources/{resourceId}/permissions/original-owner/{userId}`
- **description**：所有权转让给企业后，企业管理员将原用户权限调整为无权 / 只读 / 可写
- **auth**：需登录
- **parameters**（Path）：`resourceId`(是)、`userId`(是，原所有者)；Body：`permissionLevel`(是，NONE / READ / WRITE)
- **return**：`ApiResponse<Void>`

```json
// request（原用户改为只读）
{ "permissionLevel": "READ" }
```

---

## 13. 数据行级权限接口（RowPermissionController）

### 13.1 设置数据行级权限规则（UC-013）

- **URL**：`POST /api/resources/{resourceId}/row-permissions`
- **auth**：需登录
- **parameters**（Path）：`resourceId`(是)；Body：`granteeType`(是，GROUP/USER)、`granteeId`(是)、`ruleType`(是)、`filterExpression`(否，JSONB 对象)
- **return**：`ApiResponse<Void>`

```json
// request
{ "granteeType": "USER", "granteeId": 2, "ruleType": "REGION", "filterExpression": { "region": "华东" } }
```

### 13.2 规则列表

- **URL**：`GET /api/resources/{resourceId}/row-permissions`
- **auth**：需登录
- **parameters**（Path）：`resourceId`(是)
- **return**：`ApiResponse<List<RowPermissionRuleResponse>>`（`ruleId`/`resourceId`/`granteeType`/`granteeId`/`ruleType`/`filterExpression`/`status`/`createdBy`/`createdAt`）

### 13.3 更新规则

- **URL**：`PUT /api/resources/{resourceId}/row-permissions/{ruleId}`
- **auth**：需登录
- **parameters**（Path）：`resourceId`(是)、`ruleId`(是)；Body：`ruleType`(是)、`filterExpression`(否)
- **return**：`ApiResponse<Void>`

### 13.4 删除规则

- **URL**：`DELETE /api/resources/{resourceId}/row-permissions/{ruleId}`
- **auth**：需登录
- **parameters**（Path）：`resourceId`(是)、`ruleId`(是)
- **return**：`ApiResponse<Void>`

---

## 14. 文件上传接口（FileUploadController）

### 14.1 上传文件（UC-035）

- **URL**：`POST /api/resources/upload`（`Content-Type: multipart/form-data`）
- **description**：上传 PDF / Word / zip；保存物理文件（用户编号目录 + 相对 file_key），记录资源状态为待处理（UPLOADED），随后异步 Mock 处理；zip 解压后对每个 PDF/Word 分别处理
- **auth**：需登录

**parameters**（multipart form）：

| 参数 | 类型 | 必填 | 说明 |
| :--- | :--- | :--- | :--- |
| `file` | File | 是 | 上传文件（pdf / doc / docx / zip） |
| `parentResourceId` | Long | 否 | 父资源 ID |
| `ownerType` | String | 否 | 所有权归属：USER（默认）/ COMPANY |
| `ownerId` | Long | 否 | 所有者 ID（默认当前用户） |

- **return**：`ApiResponse<ResourceResponse>`（`status=UPLOADED`，`fileKey` 为相对标识）

```bash
curl -X POST http://localhost:8080/api/resources/upload \
  -H "Authorization: Bearer <token>" \
  -F "file=@resume.pdf"
```

```json
// response
{ "code": 0, "message": "success", "data": { "resourceId": 20, "resourceType": "FILE", "name": "resume.pdf", "fileKey": "USR20260827.../2026/08/27/a1b2c3d4e5f6.pdf", "status": "UPLOADED", "createdAt": "2026-08-27T10:00:00", "updatedAt": "2026-08-27T10:00:00" } }
```

> 异步处理完成后资源状态自动更新为 `PROCESSED`（UC-036）；zip 解压出的每个 PDF/Word 会生成独立子资源记录。

---

## 15. 消息中心接口（MessageController）

### 15.1 分页查询我的消息

- **URL**：`GET /api/messages?page=1&size=20`
- **auth**：需登录
- **parameters**（Query）：`page`(默认 1)、`size`(默认 20)
- **return**：`ApiResponse<PageResponse<UserMessageResponse>>`（`messageId`/`messageType`/`title`/`content`/`relatedCompanyId`/`relatedUserId`/`isRead`/`readAt`/`smsNotified`/`createdAt`）

```json
// response
{ "code": 0, "message": "success", "data": { "items": [ { "messageId": 1, "messageType": "JOIN_REQUEST", "title": "新的企业加入申请", "content": "用户申请加入企业【示例公司】...", "isRead": 0, "smsNotified": 0, "createdAt": "2026-08-27T10:00:00" } ], "total": 1, "page": 1, "size": 20 } }
```

### 15.2 未读消息数

- **URL**：`GET /api/messages/unread-count`
- **auth**：需登录　**parameters**：无
- **return**：`ApiResponse<Long>`

```json
// response
{ "code": 0, "message": "success", "data": 3 }
```

### 15.3 标记单条消息已读

- **URL**：`PUT /api/messages/{messageId}/read`
- **auth**：需登录
- **parameters**（Path）：`messageId`(是)
- **return**：`ApiResponse<Void>`

### 15.4 批量标记已读

- **URL**：`PUT /api/messages/read-batch`
- **auth**：需登录
- **parameters**（JSON Body）：`messageIds`(是，Long 数组)
- **return**：`ApiResponse<Void>`

```json
// request
{ "messageIds": [1, 2, 3] }
```

### 15.5 全部标记已读

- **URL**：`PUT /api/messages/read-all`
- **auth**：需登录　**parameters**：无
- **return**：`ApiResponse<Integer>`（受影响条数）

---

## 16. 系统管理接口（SystemAdminController）

> 本组接口为系统管理员专属（需 `SYSTEM_ADMIN` 角色）。

### 16.1 用户列表

- **URL**：`GET /api/admin/users?page=1&size=20&keyword=`
- **auth**：需登录（系统管理员）
- **parameters**（Query）：`page`(默认 1)、`size`(默认 20)、`keyword`(否，昵称/编号/手机号模糊搜索)
- **return**：`ApiResponse<PageResponse<UserProfileResponse>>`

### 16.2 用户详情

- **URL**：`GET /api/admin/users/{userId}`
- **auth**：需登录（系统管理员）
- **parameters**（Path）：`userId`(是)
- **return**：`ApiResponse<UserProfileResponse>`

### 16.3 禁用用户

- **URL**：`PUT /api/admin/users/{userId}/disable`
- **auth**：需登录（系统管理员）
- **parameters**（Path）：`userId`(是)
- **return**：`ApiResponse<Void>`

### 16.4 恢复用户

- **URL**：`PUT /api/admin/users/{userId}/restore`
- **auth**：需登录（系统管理员）
- **parameters**（Path）：`userId`(是)
- **return**：`ApiResponse<Void>`

### 16.5 注销用户

- **URL**：`DELETE /api/admin/users/{userId}`
- **auth**：需登录（系统管理员）
- **parameters**（Path）：`userId`(是)
- **return**：`ApiResponse<Void>`

### 16.6 审计人员列表

- **URL**：`GET /api/admin/auditors`
- **auth**：需登录（系统管理员）
- **parameters**：无
- **return**：`ApiResponse<List<AuditorResponse>>`（`userId`/`userNo`/`nickname`/`phoneMasked`/`auditScope`/`scopeDetails`/`grantedBy`/`status`/`createdAt`）

### 16.7 分配审计人员权限（UC-019）

- **URL**：`POST /api/admin/auditors`
- **description**：分配审计人员权限及查看范围（敏感系统权限配置，需操作者手机短信验证码）
- **auth**：需登录（系统管理员）　**前置**：发送验证码（scene=AUDITOR_ASSIGN）
- **parameters**（JSON Body）：`userId`(是)、`auditScope`(是，ALL/REGULAR_USERS/ENTERPRISE_USERS)、`smsCode`(是)
- **return**：`ApiResponse<Void>`

### 16.8 调整审计人员权限

- **URL**：`PUT /api/admin/auditors/{userId}`
- **description**：调整审计人员查看范围（短信验证）
- **auth**：需登录（系统管理员）
- **parameters**（Path）：`userId`(是)；Body：`auditScope`(是)、`smsCode`(是)
- **return**：`ApiResponse<Void>`

### 16.9 撤销审计角色

- **URL**：`DELETE /api/admin/auditors/{userId}`
- **auth**：需登录（系统管理员）
- **parameters**（Path）：`userId`(是)
- **return**：`ApiResponse<Void>`

### 16.10 查看全部审计日志

- **URL**：`GET /api/admin/audit-logs?userId=&companyId=&action=&startTime=&endTime=&page=1&size=20`
- **auth**：需登录（系统管理员）
- **parameters**（Query）：`userId`(否)、`companyId`(否)、`action`(否)、`startTime`(否)、`endTime`(否)、`page`(默认 1)、`size`(默认 20)
- **return**：`ApiResponse<PageResponse<AuditLogResponse>>`（`logId`/`userId`/`userType`/`companyId`/`action`/`resourceType`/`resourceId`/`detail`/`ipAddress`/`userAgent`/`createdAt`）

### 16.11 查询系统参数

- **URL**：`GET /api/admin/settings/{key}`
- **auth**：需登录（系统管理员）
- **parameters**（Path）：`key`(是，如 `storage.quota.personal`)
- **return**：`ApiResponse<SystemSettingResponse>`（`key`/`value`）

### 16.12 配置系统参数（UC-030）

- **URL**：`PUT /api/admin/settings`
- **description**：配置系统参数；存储配额参数（`storage.quota.*`）值必须为正整数
- **auth**：需登录（系统管理员）
- **parameters**（JSON Body）：`key`(是)、`value`(是)
- **return**：`ApiResponse<Void>`

```json
// request（调整个体存储默认配额）
{ "key": "storage.quota.personal", "value": "209715200" }
```

### 16.13 设置 / 调整个体存储配额（UC-031）

- **URL**：`PUT /api/admin/storage-quotas`
- **auth**：需登录（系统管理员）
- **parameters**（JSON Body）：`quotaType`(是，USER/COMPANY)、`subjectId`(是)、`quotaBytes`(是，正整数)
- **return**：`ApiResponse<Void>`

### 16.14 查询个体存储配额

- **URL**：`GET /api/admin/storage-quotas/{quotaType}/{subjectId}`
- **auth**：需登录（系统管理员）
- **parameters**（Path）：`quotaType`(是，USER/COMPANY)、`subjectId`(是)
- **return**：`ApiResponse<StorageQuotaResponse>`（`quotaType`/`subjectId`/`quotaBytes`/`updatedBy`/`updatedAt`）

### 16.15 移除个体存储配额

- **URL**：`DELETE /api/admin/storage-quotas/{quotaType}/{subjectId}`
- **description**：移除个体配额，该主体恢复使用全局默认配额
- **auth**：需登录（系统管理员）
- **parameters**（Path）：`quotaType`(是)、`subjectId`(是)
- **return**：`ApiResponse<Void>`

### 16.16 待审批企业变更申请列表

- **URL**：`GET /api/admin/company-approvals/pending`
- **auth**：需登录（系统管理员或有权限的审计人员）
- **parameters**：无
- **return**：`ApiResponse<List<CompanyApproval>>`

### 16.17 审批企业变更申请（UC-033）

- **URL**：`POST /api/admin/company-approvals/{approvalId}/review`
- **description**：审批企业注销 / 所有权转让申请（系统管理员或有权限的审计人员）
- **auth**：需登录
- **parameters**（Path）：`approvalId`(是)；Body：`approved`(是，true 批准 / false 拒绝)、`note`(否，审批意见)
- **return**：`ApiResponse<Void>`

```json
// request
{ "approved": true, "note": "同意注销" }
```

---

## 17. 审计与客服接口（AuditController）

> 本组接口需 `AUDITOR` / `SYSTEM_ADMIN` / `CUSTOMER_SERVICE` 角色。

### 17.1 审计人员查询审计日志（UC-020）

- **URL**：`GET /api/audit/logs?userId=&companyId=&action=&startTime=&endTime=&page=1&size=20`
- **description**：审计人员按系统管理员分配的查看范围（audit_scope + scope_details）查询日志
- **auth**：需登录（审计人员 / 系统管理员）
- **parameters**（Query）：`userId`(否)、`companyId`(否)、`action`(否)、`startTime`(否)、`endTime`(否)、`page`(默认 1)、`size`(默认 20)
- **return**：`ApiResponse<PageResponse<AuditLogResponse>>`

> 范围规则：`ALL`=全部（不含 SYSTEM 系统用户操作）；`REGULAR_USERS`=仅普通用户；`ENTERPRISE_USERS`=仅企业用户（受 `allowed_company_ids` 限制）

### 17.2 客服查看用户信息（UC-021）

- **URL**：`GET /api/audit/users/{userId}/info`
- **description**：客服人员验证服务对象后查看用户信息
- **auth**：需登录（客服人员 / 系统管理员）
- **parameters**（Path）：`userId`(是)
- **return**：`ApiResponse<AuditUserInfoResponse>`（`userId`/`userNo`/`nickname`/`phoneMasked`/`status`/`systemRole`）

### 17.3 客服查看指定用户日志（UC-021）

- **URL**：`GET /api/audit/users/{userId}/logs?action=&startTime=&endTime=&page=1&size=20`
- **description**：客服人员查看指定用户权限范围内的日志
- **auth**：需登录（客服人员 / 系统管理员）
- **parameters**（Path）：`userId`(是)；Query：`action`(否)、`startTime`(否)、`endTime`(否)、`page`(默认 1)、`size`(默认 20)
- **return**：`ApiResponse<PageResponse<AuditLogResponse>>`

---

> 文档生成依据：项目当前 `com.crm.controller` 包下 16 个 Controller 的实际实现；接口参数与返回结构与对应 DTO 一致。








