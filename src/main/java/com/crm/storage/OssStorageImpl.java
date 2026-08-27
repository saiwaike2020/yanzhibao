package com.crm.storage;

import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;

/**
 * 云存储（OSS）实现占位（未来接入）。
 *
 * <p>通过配置 {@code crm.storage.strategy=oss} 切换后启用；
 * 接入时实现直传 / 签名 URL 等能力，业务侧无需改动。
 */
@Slf4j
public class OssStorageImpl implements StorageService {

    public OssStorageImpl() {
        log.warn("[OSS 存储] 云存储策略已选择但尚未接入，请实现 OssStorageImpl");
    }

    @Override
    public void save(String fileKey, InputStream inputStream) {
        throw new UnsupportedOperationException("OSS 存储尚未接入，请使用 local 策略");
    }

    @Override
    public InputStream load(String fileKey) {
        throw new UnsupportedOperationException("OSS 存储尚未接入，请使用 local 策略");
    }

    @Override
    public void delete(String fileKey) {
        throw new UnsupportedOperationException("OSS 存储尚未接入，请使用 local 策略");
    }

    @Override
    public boolean exists(String fileKey) {
        throw new UnsupportedOperationException("OSS 存储尚未接入，请使用 local 策略");
    }
}
