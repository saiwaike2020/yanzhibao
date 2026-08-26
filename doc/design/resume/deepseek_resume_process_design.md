# 简历文件上传、处理与检索系统设计方案

## 1. 总体架构

本方案实现 CRM 系统中简历文件的上传、解析、元数据提取、向量化存储与语义检索。当前文件存储采用服务器本地磁盘，但通过接口抽象可无缝切换至对象存储（如 MinIO）。文件处理核心逻辑同样通过接口-实现类设计，支持按文件类型扩展。

### 1.1 系统组件

文件存储服务：负责文件保存、读取、删除，定义统一接口，默认本地实现。
文件处理管道：将整个处理流程拆分为多个阶段，通过接口定义，支持不同文件类型。
异步任务处理器：使用 @Async 或消息队列执行耗时操作，避免阻塞上传接口。
数据存储：使用 PostgreSQL + pgvector，统一存储文件基础信息、简历元数据和向量数据。
检索服务：接收查询文本，生成查询向量，执行相似度搜索并关联文件信息。

### 1.2 处理流程

1. 用户上传简历文件（PDF/Word）。
2. 后端校验权限、文件类型、大小。
3. 文件保存至本地存储，同时在 file_metadata 表插入基础信息。
4. 立即返回文件 ID，后台异步开始处理：
根据文件类型选择对应的 ResumeFileProcessor 实现类。
读取文件内容，转换为 Markdown。
提取个人信息（姓名、出生年月、手机、邮箱等），保存到 resume_metadata 表。
去除个人信息后的 Markdown 内容分块，调用 Embedding 模型生成向量。
将向量和分块文本保存到 resume_embeddings 表。
更新文件状态为 PROCESSED 或 FAILED。
5. 用户通过检索接口输入查询条件，系统将查询向量化，在 resumeembeddings 中搜索相似向量，并关联 filemetadata 和 resume_metadata 返回结果。

## 2. 数据库设计 

### 2.1 文件基础信息表 file_metadata
 
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGSERIAL PRIMARY KEY | 文件唯一标识 |
| file_name | VARCHAR(255) NOT NULL | 原始文件名 |
| file_size | BIGINT | 文件大小（字节） |
| file_type | VARCHAR(50) | MIME 类型，如 application/pdf |
| storage_path | VARCHAR(512) NOT NULL | 本地存储相对路径（或对象存储键） |
| uploader_id | BIGINT | 上传用户 ID |
| resource_id | BIGINT | 关联 CRM 资源 ID（可选） |
| status | VARCHAR(20) | 处理状态：UPLOADED, PROCESSING, PROCESSED, FAILED |
| md_content | TEXT | 转换后的 Markdown 全文（可选，便于预览） |
| created_at | TIMESTAMP | 上传时间 |
| updated_at | TIMESTAMP | 更新时间 |

### 2.2 简历元数据表 resume_metadata
 
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGSERIAL PRIMARY KEY | 元数据记录 ID |
| fileid | BIGINT REFERENCES filemetadata(id) ON DELETE CASCADE | 关联文件 ID |
| full_name | VARCHAR(100) | 姓名 |
| birth_date | DATE | 出生年月 |
| phone | VARCHAR(20) | 手机号 |
| email | VARCHAR(255) | 邮箱 |
| other_fields | JSONB | 其他提取字段（工作年限、学历、技能等） |
| extracted_at | TIMESTAMP | 提取时间 |
 
唯一索引：UNIQUE(file_id)

### 2.3 向量表 resume_embeddings
 
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGSERIAL PRIMARY KEY | 向量记录 ID |
| fileid | BIGINT REFERENCES filemetadata(id) ON DELETE CASCADE | 关联文件 ID |
| chunk_index | INT | 分块序号 |
| chunk_text | TEXT | 去除个人信息后的分块文本 |
| embedding | vector(1536) | 向量（维度根据模型而定） |
| created_at | TIMESTAMP | 创建时间 |
 
向量索引（pgvector）：
 
```sql
CREATE EXTENSION IF NOT EXISTS vector;
CREATE INDEX ON resumeembeddings USING hnsw (embedding vectorcosine_ops);
```

## 3. 文件存储接口设计（可替换为对象存储）

### 3.1 接口定义
 
```java
public interface FileStorageService {
    String store(InputStream inputStream, String objectKey, String contentType, long size) throws IOException;
    InputStream retrieve(String objectKey) throws IOException;
    void delete(String objectKey) throws IOException;
    String generateObjectKey(String originalFileName, Long resourceId);
}
```

### 3.2 本地实现 LocalFileStorageService
存储根路径由配置项 file.storage.local.base-path 指定。
objectKey 作为相对于根目录的路径，内部自动创建父目录。
写入时使用 CREATE_NEW 防止覆盖，确保唯一性。
可通过 @ConditionalOnProperty 控制启用本地实现。
 
```java
@Service
@ConditionalOnProperty(name = "file.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {
    // 实现略
}
```

## 4. 文件处理接口-实现类设计（核心扩展点）

### 4.1 接口定义

定义 ResumeFileProcessor 接口，每个文件类型提供一个实现类。

```java
public interface ResumeFileProcessor {
 
    /*是否支持处理该 MIME 类型*/
    boolean supports(String contentType);
 
    /*将文件内容转换为 Markdown*/
    String convertToMarkdown(InputStream fileInputStream) throws Exception;
 
    /*从 Markdown 中提取个人信息
        @param markdown 文件转换后的 Markdown 内容
        @return 简历元数据对象
     */
    ResumeMetadata extractMetadata(String markdown) throws Exception;
 
    /*去除个人信息后的内容（用于向量化）*/
    default String removePersonalInfo(String markdown, ResumeMetadata metadata) {
        // 默认使用元数据中的字段进行替换 
        String cleaned = markdown;
        if (metadata.getFullName() != null) cleaned = cleaned.replace(metadata.getFullName(), "");
        if (metadata.getPhone() != null) cleaned = cleaned.replace(metadata.getPhone(), "");
        if (metadata.getEmail() != null) cleaned = cleaned.replace(metadata.getEmail(), "");
        if (metadata.getBirthDate() != null) cleaned = cleaned.replace(metadata.getBirthDate().toString(), "");
        return cleaned;
    }
}
```

### 4.2 实现类

#### 4.2.1 PDF 简历处理器 PdfResumeFileProcessor

supports：匹配 application/pdf。
convertToMarkdown：使用 Apache PDFBox 提取文本，简单换行处理为 Markdown 段落。
extractMetadata：推荐使用正则 + LLM 辅助提取。
正则提取手机号、邮箱。
使用 LLM 提取姓名、出生年月等，提供 JSON 输出。

```java
@Component
public class PdfResumeFileProcessor implements ResumeFileProcessor {
    @Override
    public boolean supports(String contentType) {
        return "application/pdf".equals(contentType);
    }
    // 实现 convertToMarkdown, extractMetadata
}
```

#### 4.2.2 Word 简历处理器 WordResumeFileProcessor

supports：匹配 application/msword 和 application/vnd.openxmlformats-officedocument.wordprocessingml.document。
convertToMarkdown：使用 Apache POI 读取段落和表格，生成 Markdown。
extractMetadata：与 PDF 类似。
 
```java
@Component
public class WordResumeFileProcessor implements ResumeFileProcessor {
    @Override
    public boolean supports(String contentType) {
        return contentType != null && (
                contentType.equals("application/msword") ||
                contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        );
    }
    // 实现 convertToMarkdown, extractMetadata
}
```

### 4.3 处理器工厂 ResumeFileProcessorFactory

根据文件 MIME 类型选择合适的处理器，所有处理器实现类通过 Spring 自动注入。

```java
@Component 
public class ResumeFileProcessorFactory {
    private final List<ResumeFileProcessor> processors;
 
    public ResumeFileProcessorFactory(List<ResumeFileProcessor> processors) {
        this.processors = processors;
    }
 
    public ResumeFileProcessor getProcessor(String contentType) {
        return processors.stream()
                .filter(p -> p.supports(contentType))
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("不支持的文件类型: " + contentType));
    }
}
```

## 5. 处理任务编排（异步执行）

### 5.1 处理入口

定义 ResumeProcessingService，使用 @Async 异步执行处理流程。

```java
@Service
public class ResumeProcessingService {
 
    @Autowired
    private FileStorageService storageService;
    @Autowired
    private ResumeFileProcessorFactory processorFactory;
    @Autowired 
    private FileMetadataRepository fileMetadataRepository;
    @Autowired
    private ResumeMetadataRepository resumeMetadataRepository;
    @Autowired
    private ResumeEmbeddingRepository resumeEmbeddingRepository;
    @Autowired
    private EmbeddingModel embeddingModel;
 
    @Async
    public void processFile(Long fileId) {
        FileMetadata fileMeta = fileMetadataRepository.findById(fileId).orElseThrow();
        fileMeta.setStatus("PROCESSING");
        fileMetadataRepository.save(fileMeta);
        try {
            // 1. 读取文件
            InputStream inputStream = storageService.retrieve(fileMeta.getStoragePath());
            ResumeFileProcessor processor = processorFactory.getProcessor(fileMeta.getFileType());
            
            // 2. 转换为 Markdown 
            String markdown = processor.convertToMarkdown(inputStream);
            fileMeta.setMdContent(markdown);
            fileMetadataRepository.save(fileMeta);
            
            // 3. 提取个人信息
            ResumeMetadata metadata = processor.extractMetadata(markdown);
            metadata.setFileId(fileId);
            resumeMetadataRepository.save(metadata);
            
            // 4. 去除个人信息
            String cleanedContent = processor.removePersonalInfo(markdown, metadata);
            
            // 5. 分块
            List<String> chunks = splitText(cleanedContent);
            
            // 6. 向量化并存储
            List<Embedding> embeddings = embeddingModel.embedAll(chunks).content();
            for (int i = 0; i < chunks.size(); i++) {
                ResumeEmbedding re = new ResumeEmbedding();
                re.setFileId(fileId);
                re.setChunkIndex(i);
                re.setChunkText(chunks.get(i));
                re.setEmbedding(embeddings.get(i).vectorAsList());
                resumeEmbeddingRepository.save(re);
            }
            
            // 7. 更新状态
            fileMeta.setStatus("PROCESSED");
            fileMetadataRepository.save(fileMeta);
        } catch (Exception e) {
            fileMeta.setStatus("FAILED");
            fileMetadataRepository.save(fileMeta);
            log.error("文件处理失败, fileId={}", fileId, e);
        }
    }
}
```

### 5.2 文本分块逻辑 

将 Markdown 内容按段落和长度切分，每块约 200~500 tokens，保留上下文。

```java
private List<String> splitText(String text) {
    // 简单按段落分割，或使用固定长度滑动窗口
    List<String> paragraphs = Arrays.asList(text.split("\n\n"));
    // 进一步合并或切割 
    // ...
    return paragraphs;
}
```

## 6. 检索服务设计

### 6.1 检索流程

1. 用户提交查询文本（自然语言描述）。
2. 调用 Embedding 模型将查询文本转为向量。
3. 在 resume_embeddings 中执行余弦相似度搜索，返回 Top K 记录。
4. 为每条记录关联 filemetadata 和 resumemetadata，返回文件基础信息和个人信息。
5. 根据用户权限过滤结果（仅返回有权访问的文件）。

### 6.2 API 设计

#### 6.2.1 上传文件

URL：POST /api/v1/resumes/upload
请求：multipart/form-data，字段 file，可选 resourceId
响应：{ "fileId": 1001, "status": "UPLOADED" }

#### 6.2.2 查询处理状态

URL：GET /api/v1/resumes/{fileId}/status
响应：{ "fileId": 1001, "status": "PROCESSED" }

#### 6.2.3 语义检索

URL：POST /api/v1/resumes/search
请求体：{ "query": "5年Java经验", "topK": 10 }
响应：
json
{
  "results": [
    {
      "fileId": 1001,
      "fileName": "张三_Java工程师.pdf",
      "fullName": "张三",
      "phone": "13800138000",
      "email": "zhangsan@example.com",
      "similarity": 0.92,
      "chunkText": "5年Java开发经验..."
    }
  ]
}

## 7. 可扩展性说明

存储扩展：FileStorageService 接口允许新增 MinioFileStorageService、S3FileStorageService 等实现，通过配置切换。
文件类型扩展：新增文件类型（如纯文本、HTML 简历）只需实现 ResumeFileProcessor 并注册为 Spring Bean 即可。
处理步骤扩展：可通过在 ResumeProcessingService 中增加插件接口，实现步骤级扩展（例如新增 OCR、翻译等）。
向量数据库替换：当前使用 pgvector，未来可替换为专用向量库，只需修改数据访问层，不影响处理逻辑。

## 8. 总结

本方案采用接口-实现类设计，将文件存储、文件处理、元数据提取、向量化等模块解耦，既满足当前本地存储和简历处理需求，又为后续扩展（如切换对象存储、增加新文件格式、集成更强大的向量库）奠定了良好基础。整体处理流程异步化，保证了高并发上传场景下的系统稳定性和用户体验。

以上内容均由AI搜集总结并生成，仅供参考
