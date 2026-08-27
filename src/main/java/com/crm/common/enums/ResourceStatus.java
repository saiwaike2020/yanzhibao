package com.crm.common.enums;

/**
 * 资源状态（resources.status）。
 */
public enum ResourceStatus {
    /** 正常 */
    ACTIVE,
    /** 已归档 */
    ARCHIVED,
    /** 已删除 */
    DELETED,
    /** 已上传，待处理（文件上传完成，异步处理未执行，v3.8） */
    UPLOADED,
    /** 已处理完成（文件内容处理完成，可正常使用，v3.8） */
    PROCESSED
}
