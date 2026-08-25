package com.crm.entity.converter;

import com.crm.common.enums.ResourceStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * {@link ResourceStatus} 与数据库 SMALLINT 的转换（resources.status）。
 * 1-正常 ACTIVE, 2-已归档 ARCHIVED, 3-已删除 DELETED。
 */
@Converter(autoApply = true)
public class ResourceStatusConverter implements AttributeConverter<ResourceStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(ResourceStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case ACTIVE -> 1;
            case ARCHIVED -> 2;
            case DELETED -> 3;
        };
    }

    @Override
    public ResourceStatus convertToEntityAttribute(Integer code) {
        if (code == null) {
            return null;
        }
        return switch (code) {
            case 1 -> ResourceStatus.ACTIVE;
            case 2 -> ResourceStatus.ARCHIVED;
            case 3 -> ResourceStatus.DELETED;
            default -> throw new IllegalArgumentException("未知资源状态码: " + code);
        };
    }
}
