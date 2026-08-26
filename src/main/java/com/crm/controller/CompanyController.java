package com.crm.controller;

import com.crm.dto.common.ApiResponse;
import com.crm.dto.company.CompanyResponse;
import com.crm.dto.company.CreateCompanyRequest;
import com.crm.dto.company.TransferCompanyRequest;
import com.crm.dto.company.UpdateCompanyRequest;
import com.crm.security.SecurityUtils;
import com.crm.service.CompanyService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 企业管理接口（创建企业、企业信息、所有权转让、解散）。
 */
@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    /** 创建企业，创建者默认成为 OWNER (UC-005) */
    @PostMapping
    public ApiResponse<CompanyResponse> createCompany(@Valid @RequestBody CreateCompanyRequest request) {
        return ApiResponse.ok(companyService.createCompany(SecurityUtils.getCurrentUserId(), request));
    }

    /** 当前用户加入的企业列表 */
    @GetMapping
    public ApiResponse<List<CompanyResponse>> listMyCompanies() {
        return ApiResponse.ok(companyService.listMyCompanies(SecurityUtils.getCurrentUserId()));
    }

    /** 企业详情 */
    @GetMapping("/{companyId}")
    public ApiResponse<CompanyResponse> getCompanyById(@PathVariable Long companyId) {
        return ApiResponse.ok(companyService.getCompanyById(companyId));
    }

    /** 更新企业信息（仅企业管理员） */
    @PutMapping("/{companyId}")
    public ApiResponse<CompanyResponse> updateCompany(
            @PathVariable Long companyId, @Valid @RequestBody UpdateCompanyRequest request) {
        return ApiResponse.ok(companyService.updateCompany(companyId, request));
    }

    /** 发起企业所有权转让申请（企业所有者发起，系统管理员/有权限审计人员审批，UC-033） */
    @PostMapping("/{companyId}/transfer")
    public ApiResponse<Void> applyTransferOwnership(
            @PathVariable Long companyId, @Valid @RequestBody TransferCompanyRequest request) {
        companyService.applyTransferOwnership(companyId, SecurityUtils.getCurrentUserId(), request);
        return ApiResponse.ok();
    }

    /** 发起企业注销申请（企业所有者发起，系统管理员/有权限审计人员审批，UC-018/UC-033） */
    @PostMapping("/{companyId}/dissolve")
    public ApiResponse<Void> applyDissolveCompany(@PathVariable Long companyId) {
        companyService.applyDissolveCompany(companyId, SecurityUtils.getCurrentUserId());
        return ApiResponse.ok();
    }
}
