package com.crm.common.enums;

/**
 * 资源权限级别（resource_permissions.permission_level）。
 * 优先级：OWNER &gt; WRITE &gt; READ，WRITE 包含 READ，OWNER 包含 WRITE。
 */
public enum PermissionLevel {
    /** 读权限：查看、下载 */
    READ,
    /** 写权限：新增、编辑、上传（包含读权限） */
    WRITE,
    /** 所有权：全部操作权限，包括删除、分享（包含读和写权限） */
    OWNER
}
