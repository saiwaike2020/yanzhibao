package com.crm.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 存储配置（crm.storage.*）。
 *
 * <p>策略化：通过 {@code crm.storage.strategy} 切换存储实现
 * （当前 {@code local} 本地文件系统，未来 {@code oss} 云存储）。
 */
@Data
@ConfigurationProperties(prefix = "crm.storage")
public class StorageProperties {

    /** 存储策略：local（默认）/ oss（未来） */
    private String strategy = "local";

    /** 本地存储配置 */
    private Local local = new Local();

    @Data
    public static class Local {

        /** 本地文件存储根目录（可用环境变量 STORAGE_ROOT 覆盖） */
        private String root = "./storage";
    }
}
