package com.crm.process;

import com.crm.common.enums.ResourceStatus;
import com.crm.common.enums.ResourceType;
import com.crm.common.exception.BusinessException;
import com.crm.common.exception.ErrorCode;
import com.crm.entity.Resource;
import com.crm.entity.ResourceOwner;
import com.crm.event.FileUploadedEvent;
import com.crm.repository.ResourceOwnerRepository;
import com.crm.repository.ResourceRepository;
import com.crm.service.ResourceService;
import com.crm.storage.StorageService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 文件上传事件异步监听器（事件驱动 EDA / 单体异步处理）。
 *
 * <p>接收 {@link FileUploadedEvent} 后：
 * <ul>
 *   <li>PDF / Word：直接执行 {@link FileProcessor}（当前 Mock）并标记处理完成；</li>
 *   <li>zip：解压压缩包，对其中每个 PDF / Word 创建子资源记录并逐个处理。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileProcessingListener {

    /** zip 内可处理的文档扩展名 */
    private static final Set<String> SUPPORTED_DOC_EXTENSIONS = Set.of("pdf", "doc", "docx");

    /** zip 最大条目数，防止压缩炸弹 */
    private static final int MAX_ZIP_ENTRIES = 100;

    /** 单个 zip 条目最大字节数 */
    private static final long MAX_ZIP_ENTRY_BYTES = 100L * 1024 * 1024;

    private final StorageService storageService;
    private final FileProcessor fileProcessor;
    private final ResourceRepository resourceRepository;
    private final ResourceOwnerRepository resourceOwnerRepository;
    private final ResourceService resourceService;

    /** 异步处理文件上传事件（事务提交后触发，确保异步线程能读到已提交的资源记录） */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFileUploaded(FileUploadedEvent event) {
        Resource resource = resourceRepository.findById(event.getResourceId()).orElse(null);
        if (resource == null) {
            log.warn("[文件处理] 资源不存在 resourceId={}", event.getResourceId());
            return;
        }

        String ext = extensionOf(resource.getFileKey());
        if ("zip".equalsIgnoreCase(ext)) {
            processZip(resource);
        } else {
            fileProcessor.process(resource);
        }
    }

    /** zip 解压处理：对其中每个 PDF / Word 创建子资源并逐个 Mock 处理 */
    private void processZip(Resource zipResource) {
        log.info("[文件处理] 开始解压 zip resourceId={}, fileKey={}", zipResource.getResourceId(), zipResource.getFileKey());
        String zipDir = baseDirOf(zipResource.getFileKey());

        try (ZipInputStream zis = new ZipInputStream(storageService.load(zipResource.getFileKey()))) {
            ZipEntry entry;
            int entryCount = 0;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String entryName = sanitizeEntryName(entry.getName());
                if (entryName == null) {
                    log.warn("[文件处理] 忽略非法 zip 条目: {}", entry.getName());
                    continue;
                }
                String ext = extensionOf(entryName);
                if (!SUPPORTED_DOC_EXTENSIONS.contains(ext)) {
                    continue;
                }
                if (++entryCount > MAX_ZIP_ENTRIES) {
                    log.warn("[文件处理] zip 条目数超过上限({})，停止解压 resourceId={}", MAX_ZIP_ENTRIES, zipResource.getResourceId());
                    break;
                }

                byte[] bytes = readEntryBytes(zis, entry.getSize());
                String childKey = zipDir + "/" + entryName;
                storageService.save(childKey, new ByteArrayInputStream(bytes));

                Resource child = buildChildResource(zipResource, entryName, childKey, (long) bytes.length, ext);
                resourceRepository.save(child);
                copyOwnership(zipResource.getResourceId(), child.getResourceId());

                fileProcessor.process(child);
            }
        } catch (IOException e) {
            log.error("[文件处理] zip 解压失败 resourceId={}", zipResource.getResourceId(), e);
            throw new BusinessException(ErrorCode.FILE_PROCESS_FAILED);
        }

        // zip 解压处理完成，标记 zip 资源自身为处理完成
        zipResource.setStatus(ResourceStatus.PROCESSED);
        resourceRepository.save(zipResource);
        log.info("[文件处理] zip 解压处理完成 resourceId={}", zipResource.getResourceId());
    }

    /** 构建 zip 子文件资源记录（状态：待处理 UPLOADED） */
    private Resource buildChildResource(Resource zipResource, String entryName, String fileKey,
                                        Long fileSize, String fileType) {
        Resource child = new Resource();
        child.setResourceNo(resourceService.generateResourceNo());
        child.setName(entryName);
        child.setResourceType(ResourceType.FILE);
        child.setParentResourceId(null);
        child.setCreatorUserId(zipResource.getCreatorUserId());
        child.setFileSize(fileSize);
        child.setFileType(fileType);
        child.setFileKey(fileKey);
        child.setStatus(ResourceStatus.UPLOADED);
        return child;
    }

    /** 将 zip 资源的所有权复制给子资源 */
    private void copyOwnership(Long fromResourceId, Long toResourceId) {
        List<ResourceOwner> owners = resourceOwnerRepository.findByResourceId(fromResourceId);
        for (ResourceOwner owner : owners) {
            ResourceOwner copy = new ResourceOwner();
            copy.setResourceId(toResourceId);
            copy.setOwnerType(owner.getOwnerType());
            copy.setOwnerId(owner.getOwnerId());
            copy.setValidFrom(owner.getValidFrom());
            copy.setValidUntil(owner.getValidUntil());
            copy.setGrantedBy(owner.getGrantedBy());
            copy.setStatus(1);
            resourceOwnerRepository.save(copy);
        }
    }

    /** 读取 zip 条目内容（限制大小防压缩炸弹） */
    private byte[] readEntryBytes(InputStream in, long declaredSize) throws IOException {
        long max = Math.max(MAX_ZIP_ENTRY_BYTES, declaredSize);
        return in.readNBytes((int) Math.min(max, Integer.MAX_VALUE));
    }

    /** 规范化 zip 条目名：拒绝绝对路径与上级目录（防 Zip Slip） */
    private String sanitizeEntryName(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.replace('\\', '/');
        if (normalized.startsWith("/") || normalized.contains("../") || normalized.contains("/..")
                || normalized.contains("..")) {
            return null;
        }
        return normalized;
    }

    /** 提取扩展名（小写） */
    private String extensionOf(String name) {
        int idx = name.lastIndexOf('.');
        if (idx < 0 || idx == name.length() - 1) {
            return "";
        }
        return name.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    /** file_key 去掉扩展名（zip 解压后子文件存放的目录前缀，含 uuid 目录，如 USRxxx/2026/08/27/uuid） */
    private String baseDirOf(String fileKey) {
        int idx = fileKey.lastIndexOf('.');
        return idx > 0 ? fileKey.substring(0, idx) : fileKey;
    }
}
