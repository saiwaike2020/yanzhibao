package com.crm.entity.converter;

import com.crm.common.enums.UserStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * {@link UserStatus} 与数据库 SMALLINT 的转换（sys_users.status）。
 * 1-正常 ACTIVE, 2-禁用 DISABLED, 3-已注销 CANCELLED。
 */
@Converter(autoApply = true)
public class UserStatusConverter implements AttributeConverter<UserStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(UserStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case ACTIVE -> 1;
            case DISABLED -> 2;
            case CANCELLED -> 3;
        };
    }

    @Override
    public UserStatus convertToEntityAttribute(Integer code) {
        if (code == null) {
            return null;
        }
        return switch (code) {
            case 1 -> UserStatus.ACTIVE;
            case 2 -> UserStatus.DISABLED;
            case 3 -> UserStatus.CANCELLED;
            default -> throw new IllegalArgumentException("未知用户状态码: " + code);
        };
    }
}
