# 方案

## 实现

### 一、 核心架构设计与目录规划

    为了让系统清晰区分“单文件”和“ZIP压缩包”，避免触发器死循环，必须在 OSS 中做好严格的目录规划：

    crm-data/single/：用于存放前端直接上传的单份 PDF/Word 文档。

    crm-data/zip-temp/：专门用于接收前端上传的 ZIP 压缩包。只有上传到这个目录的 .zip 文件才会触发解压动作。

    crm-data/extracted/：用于存放由函数计算（FC）解压释放出来的真实文档。

### 二、 第一步：配置阿里云 OSS 与 函数计算 (FC)

    目标：实现只要有 ZIP 包落入指定目录，云端自动以百兆级速度解压，并将解压后的文件清单推送给 Java 后端。

    1. 部署官方“ZIP 自动解压”应用
    登录阿里云 函数计算 FC 控制台，进入“应用中心”。

    搜索并创建 “OSS ZIP 自动解压” 模板应用。

    关键配置：

    触发器事件：选择 oss:ObjectCreated:PutObject 和 oss:ObjectCreated:PostObject。

    触发规则：前缀设置为 crm-data/zip-temp/，后缀设置为 .zip。

    解压目标目录：设置为 crm-data/extracted/。

    2. 深度定制 FC 代码：增加 Webhook 回调（关键）
    官方模板默认只负责把文件解压到 OSS。为了让您的 CRM 数据库感知到这些新文件，需要修改 FC 的代码（通常为 Python 或 Node.js），在解压完成后向您的 Java 后端发送一个 HTTP 回调。

    FC 定制逻辑（Python 伪代码示例）：

    ``` Python
    import requests
    import json
    import oss2

    def handler(event, context):
        # 1. 解析 OSS 触发事件，获取上传的 ZIP 文件名
        # 2. 从 OSS 下载 ZIP 到 FC 的临时内存盘 (/tmp)
        # 3. 执行解压，将解压后的文件逐个上传到 crm-data/extracted/
        
        extracted_files = [] # 记录解压成功的文件清单
        
        for file in unzipped_files:
            # 上传到 OSS 目标目录
            bucket.put_object('crm-data/extracted/' + file.name, file.read())
            extracted_files.append({
                "fileName": file.name,
                "ossPath": 'crm-data/extracted/' + file.name,
                "size": file.size
            })
            
        # 4. 【新增回调逻辑】通知 Java 后端入库
        webhook_url = "https://api.yourdomain.com/api/oss/webhook/zip-extracted"
        headers = {'Content-Type': 'application/json', 'X-Secure-Token': '您的防伪造密钥'}
        payload = {
            "sourceZip": "crm-data/zip-temp/batch_1.zip",
            "fileCount": len(extracted_files),
            "files": extracted_files
        }
        # 发送异步 POST 请求给 Java 后端
        requests.post(webhook_url, data=json.dumps(payload), headers=headers)
        
        return "Success"
    ```

    目的：打通数据流，实现“上传 -> 解压 -> 数据库自动落表”的闭环。

### 三、 第二步：Java 后端实现 (Spring Boot)

    目标：提供安全的前端 STS 授权，并处理来自前端（单文件）和 FC（ZIP批量）的入库请求。

    1. STS 授权接口（统一入口）
    复用之前提供的 STS 代码，但前端调用时可以根据文件类型，决定后续要往哪个目录传。

    2. 单文件/小批量入库 API（供前端调用）
    当用户只上传 1~10 个文件时，前端直传 OSS crm-data/single/ 目录后，调用此接口。

    Java
    @PostMapping("/api/documents/single")
    public R<?> saveSingleDocuments(@RequestBody List<DocumentDto> docs) {
        // 采用 MyBatis-Plus 或 JDBC 批量插入资源表
        documentService.saveBatch(docs);
        return R.ok("单文件保存成功");
    }
    3. ZIP 解压 Webhook 回调 API（供阿里云 FC 调用）
    接收函数计算发来的解压清单文件。由于解压可能包含上千个文件，建议采用异步或者快速确认机制。

    ```Java
    @RestController
    @RequestMapping("/api/oss/webhook")
    public class OssWebhookController {

        @Autowired
        private DocumentService documentService;

        @PostMapping("/zip-extracted")
        public String handleZipExtracted(@RequestHeader("X-Secure-Token") String token, 
                                        @RequestBody ZipExtractPayload payload) {
            // 1. 安全校验：验证请求是否真的来自阿里云 FC（比对 Token）
            if (!"您的防伪造密钥".equals(token)) {
                throw new UnauthorizedException("非法的回调请求");
            }
            
            // 2. 批量落库处理：将解压出来的上千个文件元数据插入 PostgreSQL
            List<Document> docEntities = payload.getFiles().stream().map(f -> {
                Document doc = new Document();
                doc.setFileName(f.getFileName());
                doc.setOssUrl(f.getOssPath());
                doc.setFileSize(f.getSize());
                doc.setUploadWay("ZIP_EXTRACT"); // 标记来源
                return doc;
            }).collect(Collectors.toList());
            
            // 使用批处理写入，提升性能
            documentService.saveBatch(docEntities);
            
            // 3. (可选) 删除原始的 ZIP 包以节省存储空间
            // ossClient.deleteObject("crm-data/zip-temp/", payload.getSourceZip());

            return "SUCCESS"; // 快速响应 FC，避免 FC 函数超时收费
        }
    }
    ```

### 四、 第三步：前端智能上传路由 (Vue/React)

    目标：根据用户选择的文件数量，智能走不同的上传策略，保障极致 UX。

    前端可以引入 jszip 库，实现纯前端打包，或者引导用户直接上传打包好的 .zip。

    前端伪代码逻辑：

    ```JavaScript
    import OSS from 'ali-oss';
    import JSZip from 'jszip'; // 用于前端动态打包

    async function handleFilesUpload(files) {
        // 获取 STS Token
        const sts = await getStsTokenFromBackend();
        const client = new OSS({ /* 配置 STS */ });

        if (files.length <= 10) {
            // 【策略 A：单文件/小批量】-> 并发直传到 single 目录
            const results = await uploadConcurrently(files, client, 'crm-data/single/');
            // 通知后端落库
            await backendApi.saveSingleDocuments(results);
            alert("上传完成！");
            
        } else {
            // 【策略 B：大批量（成百上千）】-> 前端打成 ZIP 后直传到 zip-temp 目录
            showLoading("正在本地打包，请稍候...");
            const zip = new JSZip();
            files.forEach(f => zip.file(f.name, f));
            const zipBlob = await zip.generateAsync({type:"blob", compression: "DEFLATE"});
            
            showLoading("正在上传压缩包...");
            const zipName = `crm-data/zip-temp/batch_${Date.now()}.zip`;
            // 只需执行一次上传动作，避免浏览器卡死
            await client.multipartUpload(zipName, zipBlob, {
                progress: (p) => updateProgressBar(p)
            });
            
            alert("打包上传成功！服务器正在后台解压并自动入库，稍后请刷新页面查看。");
            // 后续全交由 阿里云 FC 和 Webhook 异步处理，前端无需等待。
        }
    }
    ```

### 五、 方案总结与核心优势验证

    彻底消除后端 OOM 风险：无论上千文件还是几个 G 的压缩包，流量完全不经过 Java 后端进程，JVM 内存零压力。

    极速的用户体验：上千份文件在浏览器端逐个建立 HTTP 握手极慢。压缩成单 ZIP 传输，将大量网络 I/O 转化为一次连续传输，耗时极大缩短。

    弹性计算，成本极低：阿里云 FC 按毫秒计费。平时无上传时不花一分钱；上传几千份文件时，云端分配 2GB 内存甚至多线程在几秒内瞬间解压完毕，单次成本不到一分钱。

    数据库写入优化：由 FC 整理好统一的 JSON 数组推送给 Java 后端，Java 后端只需执行一次 INSERT INTO ... VALUES (),(),() 的批量操作，极大降低了对 PostgreSQL 的连接开销和事务压力。

## 分析

## MinI

### 1. MinIO 是什么？

MinIO 是一个**高性能、开源的对象存储服务器**，完全兼容 Amazon S3 API，允许应用程序通过标准 HTTP 接口存储和检索非结构化数据（如文件、图片、视频、文档等）。其核心设计目标是为云原生应用提供可扩展、可靠且易于部署的存储后端。

- **对象存储**：数据以“对象”形式存放在扁平的“桶（Bucket）”中，每个对象有唯一的键（Key）和元数据。
- **S3 兼容**：可使用任何支持 S3 协议的 SDK 或工具（如 AWS SDK、boto3、MinIO Java SDK）进行操作。
- **轻量快速**：单个二进制文件即可运行，占用资源少，性能极高，适合海量小文件或大文件的并发读写。
- **分布式**：支持多节点集群部署，提供数据冗余（纠删码）和横向扩展能力。
- **开源**：社区版免费，商业版提供额外支持。

### 2. MinIO 用来做什么？

MinIO 常用于以下场景：

- 替换传统文件服务器/NAS，为 Web 应用提供统一的文件存储服务。
- 大数据/湖仓存储，存储 Parquet、Avro 等分析数据。
- AI/ML 数据存储，存储训练数据集、模型文件、特征数据等。
- 备份与归档，支持生命周期管理。
- 静态资源托管，存储图片、视频、CSS/JS 等，通过 CDN 加速分发。

在 Web 系统中，MinIO 的核心用途是**存储用户上传的文件**，例如图片、文档、音视频等。

### 3. 在 Web 系统中对大量文件上传的作用 

当系统需要支持上千份文件批量上传时，直接使用本地磁盘或传统文件系统会面临很多挑战：

| 问题 | 传统做法（本地磁盘/NAS） | MinIO 解决方案 |
|------|--------------------------|----------------|
| **可扩展性** | 单机磁盘容量有限，扩展困难 | 多节点集群，动态扩展存储容量 |
| **性能瓶颈** | 高并发上传可能导致磁盘 I/O 瓶颈 | 分布式架构，多节点分担负载 |
| **高可用** | 单点故障风险，需要额外备份 | 数据自动冗余（纠删码），节点故障不丢失 |
| **上传方式** | 一般只能整体上传，难支持断点续传 | 支持分片上传（Multipart Upload），大文件更稳定 |
| **访问控制** | 需要自行实现文件权限管理 | 提供预签名 URL、桶策略、对象级别权限 |
| **运维成本** | 需要维护文件服务器、备份、监控 | 部署简单，与云原生生态无缝集成 |

具体作用包括：

#### 3.1 高并发上传与下载

MinIO 采用 Go 语言编写，单机即可支撑非常高的吞吐量（数 GB/s），非常适合文件批量上传场景。即使前端同时上传数百个文件，后端存储也能轻松应对。

#### 3.2 分片上传与断点续传

对于上千份文件或大文件（如几百 MB 的 PDF 合集），MinIO 原生支持 S3 Multipart Upload 协议。前端可以将文件切片后上传，后端合并，支持断点续传，避免网络波动导致整个文件重传。

#### 3.3 对象键管理

每个文件在 MinIO 中以唯一键（Key）存储，例如 `resource/123/uuid.pdf`。系统可以灵活组织键结构，方便按资源、用户、日期等维度管理文件。

#### 3.4 与 AI 服务解耦

在 CRM 系统中，文件上传到 MinIO 后，AI 服务（如 Python 应用）可以直接通过 S3 SDK 读取文件进行解析和向量化。无需将文件从一个服务传递到另一个服务，避免网络和内存开销。

#### 3.5 安全与权限控制

MinIO 支持预签名 URL（Presigned URL），可以生成临时访问链接，供前端直接上传/下载，而无需经过后端中转，减轻后端压力。同时可设置桶策略，限制访问 IP、用户等。

#### 4. 举例说明

假设 CRM 系统需要实现一个功能：**企业管理员批量上传 2000 份客户合同（PDF 文件）到“客户资料库”**。

##### 传统做法（本地磁盘）

- 前端将 2000 个文件依次通过 HTTP 请求发给后端。
- 后端将文件保存到服务器本地磁盘（如 `/data/uploads/`）。
- 如果文件很大，后端内存可能不够；如果并发高，磁盘 I/O 会拖慢整个服务。
- 文件存在单个服务器上，如果服务器宕机，所有上传失败。
- 后续 AI 服务需要读取文件，必须挂载相同的磁盘或通过后端接口获取文件，增加耦合。

##### 使用 MinIO 的做法

1. **前端直接上传到 MinIO**（通过预签名 URL，避免占用后端带宽）。  
   - 前端先向后端请求一个预签名 URL。
   - 然后直接将该文件 PUT 到 MinIO。
   - 这种“直传”模式大大减轻了后端压力。
2. **后端只保存文件元数据**（文件名、大小、存储键等）到数据库。
3. **MinIO 自动将文件分布存储在多个节点上**，保证高可用和冗余。
4. **AI 服务直接通过 S3 SDK 从 MinIO 拉取文件**进行解析和向量化，无需经过后端。
5. **下载或预览时**，后端生成一个临时预签名 URL 给前端，前端直接从 MinIO 下载，后端无压力。

**结果**：

- 上传速度快，支持断点续传，大文件也不怕。
- 后端服务资源占用低，可专注业务逻辑。
- 存储可无限横向扩展，轻松应对未来文件量增长。
- 文件安全可靠，支持权限控制和审计。

#### 5. 总结

MinIO 是一个强大的对象存储引擎，尤其适合 Web 系统中的大量文件上传场景。它能提供高性能、高可用、易扩展的存储能力，并通过 S3 兼容 API 与前端、后端、AI 服务无缝集成。在 CRM 这类需要处理海量文档的业务系统中，使用 MinIO 可以显著提升用户体验和系统稳定性。

### MinIO方案对比

| 评估项 | 阿里云自建 MinIO | 阿里云原生 OSS |
| ---- | ---- | ---- |
| 计费方式 | 固定成本：ECS 实例费 + ESSD云盘存储费。无流量费、无API请求费。 | 组合计费：存储包 + 外网流出流量费 + API 请求次数费。 |
| 适用场景 | 高频 API 读写、内部系统高频大流量传输、要求数据绝对不出云主机。 | 文件海量且读写频率极不稳定、需要极高运维容灾能力（99.999999999% 可靠性）。 |
| 运维成本 | 较弱：需自行配置云盘快照备份、监控磁盘容量、维护高可用集群。 | 零运维：阿里云全托管，天生支持开箱即用与自动化生命周期转归档。 |
| 数据安全保障 | 强依赖阿里云快照：需在阿里云控制台为 ESSD 云盘开启自动快照策略。 | 多副本异地冗余存储。 |

