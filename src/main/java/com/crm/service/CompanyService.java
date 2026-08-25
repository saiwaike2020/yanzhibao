package com.crm.service;

import com.crm.dto.company.CompanyResponse;
import com.crm.dto.company.CreateCompanyRequest;
import com.crm.dto.company.TransferCompanyRequest;
import com.crm.dto.company.UpdateCompanyRequest;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 企业服务（创建企业、企业信息、所有权转让、解散）。
 */
@Service
public class CompanyService {

    /** 创建企业，创建者默认成为 OWNER (UC-005) */
    public CompanyResponse createCompany(Long userId, CreateCompanyRequest request) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 当前用户加入的企业列表 */
    public List<CompanyResponse> listMyCompanies(Long userId) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 企业详情 */
    public CompanyResponse getCompanyById(Long companyId) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 更新企业信息（仅企业管理员） */
    public CompanyResponse updateCompany(Long companyId, UpdateCompanyRequest request) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 转让企业所有权（仅企业所有者） */
    public void transferOwnership(Long companyId, TransferCompanyRequest request) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 解散企业（仅企业所有者，UC-018） */
    public void dissolveCompany(Long companyId) {
        throw new UnsupportedOperationException("TODO");
    }
}
