package com.crm.entity.converter;

import com.crm.common.enums.MemberStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * {@link MemberStatus} 与数据库 SMALLINT 的转换（company_members.status）。
 * 0-已邀请待接受 INVITED, 1-正常 ACTIVE, 2-已禁用 DISABLED, 3-已退出 EXITED。
 */
@Converter(autoApply = true)
public class MemberStatusConverter implements AttributeConverter<MemberStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(MemberStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case INVITED -> 0;
            case ACTIVE -> 1;
            case DISABLED -> 2;
            case EXITED -> 3;
        };
    }

    @Override
    public MemberStatus convertToEntityAttribute(Integer code) {
        if (code == null) {
            return null;
        }
        return switch (code) {
            case 0 -> MemberStatus.INVITED;
            case 1 -> MemberStatus.ACTIVE;
            case 2 -> MemberStatus.DISABLED;
            case 3 -> MemberStatus.EXITED;
            default -> throw new IllegalArgumentException("未知成员状态码: " + code);
        };
    }
}
