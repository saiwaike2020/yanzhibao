package com.crm.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 文件上传完成领域事件（事件驱动架构 EDA）。
 *
 * <p>上传主流程保存物理文件 + 记录「待处理」状态后发布本事件，
 * 由异步监听器（{@code FileProcessingListener}）执行耗时的文件处理逻辑。
 */
@Getter
public class FileUploadedEvent extends ApplicationEvent {

    /** 资源记录 ID */
    private final Long resourceId;

    /** 文件相对抽象存储标识符 */
    private final String fileKey;

    /** 原始文件名 */
    private final String originalFileName;

    /** 文件扩展名（pdf / doc / docx / zip） */
    private final String fileType;

    /** 文件大小（字节） */
    private final Long fileSize;

    /** 上传者用户 ID */
    private final Long uploaderUserId;

    public FileUploadedEvent(Object source, Long resourceId, String fileKey,
                             String originalFileName, String fileType, Long fileSize, Long uploaderUserId) {
        super(source);
        this.resourceId = resourceId;
        this.fileKey = fileKey;
        this.originalFileName = originalFileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.uploaderUserId = uploaderUserId;
    }
}
