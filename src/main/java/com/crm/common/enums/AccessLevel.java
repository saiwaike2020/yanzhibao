package com.crm.common.enums;

/**
 * 原所有者访问级别（v3.6：所有权分享给企业后，企业管理员调整原用户的访问权限）。
 */
public enum AccessLevel {
    /** 无权：不授予任何权限 */
    NONE,
    /** 只读：查看、下载 */
    READ,
    /** 可写：新增、编辑、上传（含读权限） */
    WRITE
}
