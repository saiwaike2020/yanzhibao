# 消息中心接口实现详解

> - 设计依据：`doc/userAccountRequirement_260820_v3.md` 第 **4.8 消息中心设计** 节 / 用例 **UC-026 / UC-027 / UC-028**
> - 实现代码：`com.crm` 包（Controller → Service → Repository → Entity 分层，Spring Boot 3.2 + Spring Data JPA + Spring Security + PostgreSQL 16）
> - 短信通知：当前版本**不通过短信发送额外提醒**，仅站内消息；`sms_notified` 字段预留短信扩展点

## 目录

1. [接口概述](#1-接口概述)
2. [消息类型与内容约定](#2-消息类型与内容约定)
3. [数据库设计](#3-数据库设计)
4. [消息中心接口](#4-消息中心接口)
5. [申请 / 邀请消息联动](#5-申请--邀请消息联动)
6. [安全与注意事项](#6-安全与注意事项)

---

## 1. 接口概述

| 项 | 值 |
| :--- | :--- |
| 基础路径 | `/api/messages` |
| 鉴权要求 | 需登录（携带 `Authorization: Bearer <token>`） |
| 核心功能 | 查看我的消息、未读数、标记已读 / 全部已读 |
| 用例来源 | UC-028（设计文档 7.28 业务场景） |

### 1.1 业务规则（来自设计文档）

1. 每个用户拥有**独立**的消息列表，仅能查看 / 操作属于自己的消息；
2. 消息来源：用户申请加入企业（`JOIN_REQUEST`，发给企业管理员）、企业邀请用户加入（`INVITATION`，发给被邀请用户）、系统主动发送（`SYSTEM`）；
3. 当前版本**不通过短信发送额外提醒**，仅站内消息；预留 `sms_notified` 字段与发送扩展点，后续接入短信网关后同步补发短信。

---

## 2. 消息类型与内容约定

| `message_type` | 触发场景 | 接收者 | 内容示例 |
| :--- | :--- | :--- | :--- |
| `JOIN_REQUEST` | 用户申请加入企业（UC-026） | 企业全部管理员（OWNER / ADMIN） | 「用户申请加入企业【示例公司】，请前往成员管理页面处理。」 |
| `INVITATION` | 管理员邀请用户加入（UC-027） | 被邀请用户 | 「企业【示例公司】邀请您加入，请前往消息中心确认。」 |
| `SYSTEM` | 系统 / 管理员主动发送 | 指定用户 | 由系统管理员配置 |

---

## 3. 数据库设计

### 3.1 `user_messages`（用户消息中心表）

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| `message_id` | BIGINT IDENTITY | 消息 ID（主键） |
| `user_id` | BIGINT | 接收者用户 ID |
| `message_type` | VARCHAR(30) | SYSTEM / JOIN_REQUEST / INVITATION |
| `title` | VARCHAR(128) | 消息标题 |
| `content` | VARCHAR(512) | 消息正文 |
| `related_company_id` | BIGINT | 关联企业 ID（申请 / 邀请消息填充） |
| `related_user_id` | BIGINT | 关联用户 ID（申请发起人 / 邀请发起人） |
| `is_read` | SMALLINT | 0-未读，1-已读 |
| `read_at` | TIMESTAMP | 已读时间 |
| `sms_notified` | SMALLINT | 短信通知标记：0-未发短信（当前固定），1-已发短信（后续短信通道接入后） |
| `created_at` | TIMESTAMP | 消息创建时间 |

- **索引**：`idx_msg_user_created (user_id, created_at)`、`idx_msg_user_read (user_id, is_read)`。
- 建表脚本：`docker/init-sql/1_init_schema.sql`（第 15 张表）；JPA 实体 `UserMessage` 通过 `ddl-auto: update` 校验 / 补齐。

---

## 4. 消息中心接口

### 4.1 分页查询我的消息

```
GET /api/messages?page=1&size=20
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [
      {
        "messageId": 1,
        "messageType": "JOIN_REQUEST",
        "title": "新的企业加入申请",
        "content": "用户申请加入企业【示例公司】，请前往成员管理页面处理。",
        "relatedCompanyId": 100,
        "relatedUserId": 200,
        "isRead": 0,
        "readAt": null,
        "smsNotified": 0,
        "createdAt": "2026-08-24T18:00:00"
      }
    ],
    "total": 1,
    "page": 1,
    "size": 20
  }
}
```

### 4.2 未读消息数

```
GET /api/messages/unread-count
```

响应：

```json
{ "code": 0, "message": "success", "data": 3 }
```

### 4.3 标记单条消息已读

```
PUT /api/messages/{messageId}/read
```

响应：

```json
{ "code": 0, "message": "success", "data": null }
```

> 消息不存在或不属于当前用户：`1501`「消息不存在」。

### 4.4 批量标记已读

```
PUT /api/messages/read-batch
Content-Type: application/json

{ "messageIds": [1, 2, 3] }
```

### 4.5 全部标记已读

```
PUT /api/messages/read-all
```

响应（返回受影响条数）：

```json
{ "code": 0, "message": "success", "data": 3 }
```

---

## 5. 申请 / 邀请消息联动

### 5.1 用户申请加入企业（UC-026）

```
POST /api/companies/{companyId}/members/apply
```

- 系统校验企业存在且 ACTIVE、用户未在该企业中；
- 创建 `company_members` 记录（`status=INVITED` 待审批）；
- 向企业**全部管理员**（OWNER / ADMIN，状态 ACTIVE）发送 `JOIN_REQUEST` 消息。

### 5.2 企业邀请用户加入（UC-027）

```
POST /api/companies/{companyId}/members/invite
Content-Type: application/json

{ "phone": "13800138000", "note": "欢迎加入" }
```

- 系统校验操作者为企业管理员、被邀请人已注册且未在该企业中；
- 创建 `company_members` 记录（`status=INVITED` 待接受）；
- 向被邀请用户发送 `INVITATION` 消息。

> 当前无短信通道，未注册用户暂无法接收站内邀请（`1002`「该手机号未注册」）；后续短信通道接入后支持向未注册用户发送短信邀请。

---

## 6. 安全与注意事项

1. **归属隔离**：所有查询 / 已读操作均基于当前登录用户 `userId`，无法查看或操作他人消息。
2. **短信扩展**：`MessageService.sendSmsReminder()` 为预留扩展点（当前空实现仅打日志）；接入短信网关后实现该方法并置 `sms_notified=1` 即可，无需改动业务调用方。
3. **消息不可变**：消息发送后仅允许更新已读状态，不提供修改 / 删除接口（审计留痕）。
4. **分页**：按 `created_at` 倒序展示，最新消息在最前。
5. **性能**：未读数查询走 `(user_id, is_read)` 索引；列表查询走 `(user_id, created_at)` 索引。

---

> 本文档与代码保持同步；后续若增加消息类型（如系统公告、资源分享通知）或接入短信通道，请同步更新本文档「消息类型」与「短信扩展」小节。
