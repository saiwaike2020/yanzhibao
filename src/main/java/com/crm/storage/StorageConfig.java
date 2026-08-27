package com.crm.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 存储策略装配（策略模式 + 条件装配）：
 * <ul>
 *   <li>{@code crm.storage.strategy=local}（默认）：{@link LocalStorageImpl} 本地文件系统；</li>
 *   <li>{@code crm.storage.strategy=oss}：{@link OssStorageImpl} 云存储（未来接入）。</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    @Bean
    @ConditionalOnProperty(name = "crm.storage.strategy", havingValue = "local", matchIfMissing = true)
    public StorageService localStorageService(StorageProperties props) {
        return new LocalStorageImpl(props.getLocal().getRoot());
    }

    @Bean
    @ConditionalOnProperty(name = "crm.storage.strategy", havingValue = "oss")
    public StorageService ossStorageService() {
        return new OssStorageImpl();
    }
}
