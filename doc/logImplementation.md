# CRM 系统日志实现分析

> - 分析对象：`yanzhibao`（Spring Boot 3.2.12 + Spring Data JPA + Spring Security + PostgreSQL 16）
> - 分析日期：2026-08
> - 结论先行（v3.7 已落地）：本项目现通过 `logback-spring.xml` + `application.yml` 的 `logging` 配置实现**控制台 + 滚动文件**日志（按日 + 大小 + gz 压缩 + 保留策略）、**统一日志 pattern**、**根级别 + 包级别且 dev/prod 用 profile 区分**，日志文件存储根目录可通过 `LOG_PATH` 环境变量配置；代码中的日志点集中在少数业务 Service 与全局异常处理中。业务级审计日志（`audit_logs` 表）已建表但写入逻辑尚未接入业务代码。

---

## 目录

1. [日志框架与依赖](#1-日志框架与依赖)
2. [日志级别（Level）配置](#2-日志级别level配置)
3. [日志文件保存](#3-日志文件保存)
4. [日志格式](#4-日志格式)
5. [日志内容与代码使用点](#5-日志内容与代码使用点)
6. [业务审计日志（audit_logs）](#6-业务审计日志audit_logs)
7. [当前局限与改进建议](#7-当前局限与改进建议)

---

## 1. 日志框架与依赖

| 项 | 说明 |
| :--- | :--- |
| 日志门面 | **SLF4J**（`org.slf4j`） |
| 日志实现 | **Logback**（`logback-classic` / `logback-core`），Spring Boot 默认日志实现 |
| 引入方式 | 未在 `pom.xml` 中显式声明；通过 `spring-boot-starter-web`、`spring-boot-starter-security`、`spring-boot-starter-data-jpa` 等 starter **传递引入** `spring-boot-starter-logging` |
| 桥接 | Spring Boot 自动将 `java.util.logging`（JUL）、`commons-logging`、`log4j` 等桥接到 SLF4J |
| 自定义配置 | **不存在** `logback.xml` / `logback-spring.xml` / `log4j2.xml`，完全使用 Spring Boot 内置默认配置 |
| `pom.xml` 相关依赖 | 无日志相关 `<dependency>` 声明（依赖 `spring-boot-starter-parent` 的默认管理） |

代码中统一通过 **Lombok `@Slf4j`** 注解注入 logger，未直接使用 `LoggerFactory.getLogger(...)`：

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    // Lombok 自动生成 private static final Logger log = LoggerFactory.getLogger(AuthService.class);
}
```

---

## 2. 日志级别（Level）配置

### 2.1 当前配置（v3.7 已实现）

- `src/main/resources/application.yml` 已配置 `logging.level`：**根级别 + 包级别**（`root`、`com.crm`、`org.springframework`、`org.hibernate` 等）。
- **dev / prod 用 profile 区分**（多文档 profile 段）：
  - `dev`（默认，`spring.profiles.default: dev`）：根级别 `DEBUG`，`com.crm` `DEBUG`，`org.hibernate.SQL` `DEBUG`（打印 SQL）；
  - `prod`（`SPRING_PROFILES_ACTIVE=prod`）：根级别 `INFO`，`com.crm` `INFO`，`org.hibernate.SQL` `WARN`（不打印 SQL）。
- 兜底级别由 `logback-spring.xml` 的 `<root level="INFO">` 保证。

### 2.2 代码中使用到的级别

| 级别 | 使用场景 | 代码位置 |
| :--- | :--- | :--- |
| `log.info` | 业务成功事件：注册成功、密码重置成功、验证码生成、站内消息发送、Mock 短信发送 | `AuthService`、`SmsVerificationService`、`MessageService` |
| `log.warn` | 业务异常（`BusinessException`） | `GlobalExceptionHandler.handleBusinessException` |
| `log.error` | 系统未知异常（带完整堆栈） | `GlobalExceptionHandler.handleException` |
| `log.debug` / `log.trace` | 未使用 | — |

### 2.3 配置方式（现状未启用，提供参考）

Spring Boot 支持通过 `application.yml` 配置级别，例如：

```yaml
logging:
  level:
    root: INFO                 # 全局级别
    com.crm: DEBUG             # 业务包
    org.hibernate.SQL: DEBUG   # 打印 JPA/Hibernate SQL
    org.springframework.security: WARN
```

也可使用 `logback-spring.xml` 配合 profile：

```xml
<springProfile name="dev">
    <root level="DEBUG"/>
</springProfile>
<springProfile name="prod">
    <root level="INFO"/>
</springProfile>
```

---

## 3. 日志文件保存

### 3.1 当前配置（v3.7 已实现）

- **滚动文件输出**：`src/main/resources/logback-spring.xml` 已配置 `RollingFileAppender`：
  - 按日滚动：`crm-%d{yyyy-MM-dd}.%i.log.gz`；
  - 单文件上限 `maxFileSize=100MB`、保留 `maxHistory=30` 天、总容量上限 `totalSizeCap=5GB`，历史文件 **gz 压缩**；
  - 同时保留**控制台输出**（`ConsoleAppender`）。
- **日志根目录可配置**：`application.yml` 中 `logging.file.path: ${LOG_PATH:logs}`，可用环境变量 / 系统属性 `LOG_PATH` 覆盖（默认 `logs/`）。
- docker / 部署层面：`docker/docker-compose.yml` 中仅 postgres 容器挂载数据卷，应用日志未挂载日志卷（可通过 `LOG_PATH` 指向持久化目录）。

### 3.2 配置方式（现状未启用，提供参考）

如需落盘并按日滚动、限制大小与保留数量，建议引入 `logback-spring.xml`（示例片段）：

```xml
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/crm.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
        <fileNamePattern>logs/crm-%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
        <maxFileSize>100MB</maxFileSize>
        <maxHistory>30</maxHistory>
        <totalSizeCap>5GB</totalSizeCap>
    </rollingPolicy>
    <encoder>
        <pattern>%d{yyyy-MM-dd'T'HH:mm:ss.SSSXXX} %5p ${PID} --- [%t] %-40.40logger{39} : %m%n</pattern>
    </encoder>
</appender>
```

---

## 4. 日志格式

### 4.1 当前格式（v3.7 已实现）

统一日志 pattern 在 `logback-spring.xml` 中以 `<property name="LOG_PATTERN">` 定义，**控制台与文件 appender 共用**：

```
%d{yyyy-MM-dd'T'HH:mm:ss.SSSXXX} %5p ${PID:- } --- [%t] %-40.40logger{39} : %m%n
```

字段说明：

| 占位符 | 含义 | 示例 |
| :--- | :--- | :--- |
| `%d{...}` | 时间戳（ISO-8601，含时区偏移） | `2026-08-24T18:21:31.968+08:00` |
| `%5p` | 日志级别（右对齐，5 字符宽） | ` INFO` |
| `${PID}` | 进程 ID | `1212` |
| `%t` | 线程名 | `main`、`http-nio-8080-exec-1` |
| `%-40.40logger{39}` | Logger 名（截断到 40 字符） | `c.crm.AuthService` |
| `%m%n` | 日志消息 + 换行 | 业务日志消息 |

实际样例（来自集成测试输出）：

```
2026-08-24T18:21:31.968+08:00  INFO 1212 --- [crm] [           main] c.crm.AuthPasswordResetIntegrationTest : Starting AuthPasswordResetIntegrationTest using Java 21.0.9 with PID 1212
```

> 说明：`[crm]` 为应用名（`spring.application.name: crm`），由 Spring Boot 默认 pattern 自动携带。

### 4.2 结构化 / JSON 日志（现状未启用）

当前为纯文本行日志。生产环境如需对接日志平台（ELK / Loki），可引入 `logstash-logback-encoder` 输出 JSON：

```xml
<appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
</appender>
```

---

## 5. 日志内容与代码使用点

### 5.1 现有日志点清单

| 文件 | 级别 | 日志内容 | 备注 |
| :--- | :--- | :--- | :--- |
| `AuthService` | INFO | `手机号验证码注册成功: userId={}, userNo={}, phone={}` | 手机号经 `maskPhone()` 掩码（`138****1234`） |
| `AuthService` | INFO | `密码重置成功: userId={}, userNo={}, phone={}` | 手机号掩码 |
| `SmsVerificationService` | INFO | `验证码已生成并保存: phone={}, scene={}, 有效期={}分钟` | 不含验证码明文 |
| `SmsVerificationService` | INFO | `[Mock 短信通道] 向手机号 {} 发送验证码: {}` | **含验证码明文**（Mock 阶段，接入真实网关后需移除） |
| `MessageService` | INFO | `站内消息已发送: userId={}, type={}, title={}` | — |
| `MessageService` | INFO | `[消息中心] 短信提醒预留，暂不发送: ...` | 扩展点占位 |
| `GlobalExceptionHandler` | WARN | `业务异常: code={}, message={}` | 统一业务异常出口 |
| `GlobalExceptionHandler` | ERROR | `系统异常`（附堆栈） | 未知异常出口 |

### 5.2 内容特点

1. **占位符传参**：全部使用 `{}` 占位符延迟拼接，避免字符串拼接开销（`log.info("...{}", user)`）。
2. **隐私保护**：手机号统一掩码输出；验证码存储场景（`SmsVerification`）为 SHA-256 哈希，日志不含验证码哈希。
3. **异常堆栈**：仅 `GlobalExceptionHandler.handleException` 输出完整堆栈（`log.error("系统异常", e)`）。
4. **请求链路信息缺失**：未通过 MDC 注入请求 ID / 用户 ID / 客户端 IP，多请求并发时难以按单请求串联日志。

### 5.3 框架日志

- **JPA/Hibernate**：`application.yml` 未配置 `spring.jpa.show-sql` 或 `logging.level.org.hibernate.SQL`，默认**不打印 SQL**。
- **Spring Security**：未开启 debug，默认仅 WARN/ERROR 级别输出。
- **Web 访问日志**：未配置 `RequestLoggingFilter` / access log。

---

## 6. 业务审计日志（audit_logs）

框架日志之外，系统设计了**业务审计日志**（表 `audit_logs`，第 14 张表）：

### 6.1 数据模型（`AuditLog` 实体）

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| `log_id` | BIGINT | 日志 ID（主键） |
| `user_id` | BIGINT | 操作用户 ID |
| `user_type` | VARCHAR(32) | USER / COMPANY_USER / SYSTEM |
| `company_id` | BIGINT | 所属企业 ID |
| `action` | VARCHAR(64) | 操作行为（如角色变更、资源授权） |
| `resource_type` / `resource_id` | VARCHAR | 操作对象 |
| `detail` | JSONB | 操作详情（结构化） |
| `ip_address` / `user_agent` | VARCHAR | 客户端信息 |
| `status` | SMALLINT | 1-正常 |
| `created_at` | TIMESTAMP | 记录时间 |

### 6.2 现状

- 表结构已建（`docker/init-sql/1_init_schema.sql`）、实体与仓储（`AuditLogRepository`）已存在。
- **`AuditService` 目前为 TODO 骨架**：`queryAuditLogs` / `queryUserLogs` 均抛 `UnsupportedOperationException`。
- **业务代码尚未写入审计日志**：如企业管理员角色变更（UC-022）、资源授权（UC-011/012）、审批流程（UC-032/033）等敏感操作，尚未调用审计写入。
- 查询入口已预留：`AuditController`（审计人员 `GET /api/audit/logs`）、`SystemAdminController`（系统管理员 `GET /api/admin/audit-logs`）。

### 6.3 与框架日志的关系

| 维度 | 框架日志（Logback） | 业务审计日志（audit_logs） |
| :--- | :--- | :--- |
| 载体 | 控制台 / 文件（当前仅控制台） | PostgreSQL 表 |
| 用途 | 运行期排障、开发调试 | 合规审计、责任追踪 |
| 写入方 | 代码中 `log.x()` | 业务代码显式调用审计写入（待接入） |
| 查询 | 运维直接看日志 | 系统管理员 / 审计人员 / 客服按权限查询 |

---

## 7. 当前局限与改进建议

### 7.1 当前局限

1. **不落盘**：日志仅控制台输出，重启即丢失，无法追溯线上问题。
2. **级别不可调**：无 `logging.level` 配置，排障时无法按需放大（如开启 Hibernate SQL / 业务 DEBUG）。
3. **无滚动归档**：即使落盘也需滚动策略、保留周期与容量上限。
4. **无请求链路关联**：缺少 MDC 注入请求 ID / 用户 ID，日志难以按请求串行分析。
5. **敏感信息**：`SmsVerificationService` Mock 通道日志含验证码明文，上线前必须移除。
6. **审计日志未接入**：`audit_logs` 表空转，敏感操作无业务审计记录。
7. **日志点覆盖不全**：仅少量 Service 有日志，Controller 层入参、鉴权拒绝、资源操作等关键路径无日志。

### 7.2 改进建议（按优先级）

| 优先级 | 建议 | 说明 |
| :--- | :--- | :--- |
| P0 | 接入业务审计日志写入 | 在敏感操作（角色变更、授权、审批、系统参数调整）处调用 `AuditService` 落库 |
| P0 | 移除 Mock 短信日志中的验证码明文 | 接入真实短信网关后删除 `sendCode` 内明文输出 |
| P1 | 配置 `logback-spring.xml` | ✅ **已完成**：控制台 + 滚动文件（按日 + 大小 + 压缩 + 保留策略） |
| P1 | 配置 `logging.level` | ✅ **已完成**：根级别 + 包级别，dev/prod 用 profile 区分 |
| P2 | 引入 MDC 请求追踪 | `OncePerRequestFilter` 注入 `requestId` / `userId`，pattern 中输出 |
| P2 | 统一日志 pattern | ✅ **已完成**：`<property name="LOG_PATTERN">` 供控制台与文件复用 |
| P3 | 结构化 JSON 输出 | 对接日志平台时引入 `logstash-logback-encoder` |
| P3 | 关键路径补齐日志 | Controller 请求入参（脱敏）、鉴权失败、资源读写等 |

---

> 本文档与代码同步；若后续引入 `logback-spring.xml` 或调整 `logging` 配置，请同步更新本文档第 2 / 3 / 4 节。

