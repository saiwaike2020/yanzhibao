package com.crm.dto.company;

import com.crm.common.enums.CompanyStatus;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 企业信息响应（companies）。
 */
@Data
public class CompanyResponse {

    /** 企业 ID */
    private Long companyId;

    /** 企业编号 */
    private String companyNo;

    /** 企业名称 */
    private String name;

    /** 企业 Logo URL */
    private String logoUrl;

    /** 所有者用户 ID */
    private Long ownerUserId;

    /** 企业状态 */
    private CompanyStatus status;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
