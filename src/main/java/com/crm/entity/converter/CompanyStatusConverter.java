package com.crm.entity.converter;

import com.crm.common.enums.CompanyStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * {@link CompanyStatus} 与数据库 SMALLINT 的转换（companies.status）。
 * 1-正常 ACTIVE, 2-禁用 DISABLED, 3-已解散 DISSOLVED。
 */
@Converter(autoApply = true)
public class CompanyStatusConverter implements AttributeConverter<CompanyStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(CompanyStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case ACTIVE -> 1;
            case DISABLED -> 2;
            case DISSOLVED -> 3;
        };
    }

    @Override
    public CompanyStatus convertToEntityAttribute(Integer code) {
        if (code == null) {
            return null;
        }
        return switch (code) {
            case 1 -> CompanyStatus.ACTIVE;
            case 2 -> CompanyStatus.DISABLED;
            case 3 -> CompanyStatus.DISSOLVED;
            default -> throw new IllegalArgumentException("未知企业状态码: " + code);
        };
    }
}
