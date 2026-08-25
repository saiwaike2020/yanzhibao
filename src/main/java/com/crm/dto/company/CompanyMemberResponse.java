package com.crm.dto.company;

import com.crm.common.enums.CompanyMemberRole;
import com.crm.common.enums.MemberStatus;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 企业成员响应（company_members）。
 */
@Data
public class CompanyMemberResponse {

    /** 成员关系 ID */
    private Long memberId;

    /** 企业 ID */
    private Long companyId;

    /** 用户 ID */
    private Long userId;

    /** 用户编号 */
    private String userNo;

    /** 昵称 */
    private String nickname;

    /** 手机号（掩码展示） */
    private String phoneMasked;

    /** 企业内角色 */
    private CompanyMemberRole role;

    /** 成员状态 */
    private MemberStatus status;

    /** 加入时间 */
    private LocalDateTime joinedAt;
}
