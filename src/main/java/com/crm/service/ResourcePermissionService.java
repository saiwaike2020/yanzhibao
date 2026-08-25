package com.crm.service;

import com.crm.dto.resource.GrantPermissionRequest;
import com.crm.dto.resource.ResourcePermissionResponse;
import com.crm.dto.resource.UpdatePermissionRequest;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 资源权限服务（授权主体支持分组与用户，权限级别 READ / WRITE / OWNER）。
 */
@Service
public class ResourcePermissionService {

    /** 资源权限列表 */
    public List<ResourcePermissionResponse> listPermissions(Long resourceId) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 分配资源权限给分组 (UC-011) */
    public void grantToGroup(Long resourceId, Long operatorUserId, GrantPermissionRequest request) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 分配资源权限给用户 (UC-012) */
    public void grantToUser(Long resourceId, Long operatorUserId, GrantPermissionRequest request) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 修改权限级别 */
    public void updatePermission(Long resourceId, Long permissionId, UpdatePermissionRequest request) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 撤销资源权限 (UC-016) */
    public void revokePermission(Long resourceId, Long permissionId) {
        throw new UnsupportedOperationException("TODO");
    }
}
