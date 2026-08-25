package com.crm.dto.company;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新企业信息请求。
 */
@Data
public class UpdateCompanyRequest {

    /** 企业名称 */
    @NotBlank(message = "企业名称不能为空")
    @Size(max = 128, message = "企业名称不能超过 128 个字符")
    private String name;

    /** 企业 Logo URL */
    @Size(max = 255, message = "Logo URL 过长")
    private String logoUrl;
}
