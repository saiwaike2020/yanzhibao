package com.crm.service;

import com.crm.dto.delegation.CreateDelegationRequest;
import com.crm.dto.delegation.DelegationResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 分级授权服务（企业管理员将指定分组的管理权授予其他成员，使其成为分组管理员）。
 */
@Service
public class ManagementDelegationService {

    /** 授予成员指定分组的管理权（设置分组管理员，短信验证，UC-008 / UC-015） */
    public DelegationResponse createDelegation(Long companyId, CreateDelegationRequest request) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 管理授权列表 */
    public List<DelegationResponse> listDelegations(Long companyId) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 授权详情 */
    public DelegationResponse getDelegation(Long companyId, Long delegationId) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 撤销授权（移除分组管理员） */
    public void revokeDelegation(Long companyId, Long delegationId) {
        throw new UnsupportedOperationException("TODO");
    }
}
