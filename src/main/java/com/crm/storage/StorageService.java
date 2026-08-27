package com.crm.storage;

import java.io.InputStream;

/**
 * 物理文件存储抽象接口（依赖倒置原则 DIP / 策略模式）。
 *
 * <p>当前实现：{@link LocalStorageImpl}（本地文件系统）；
 * 未来可新增 {@link OssStorageImpl}（云存储）并通过配置 {@code crm.storage.strategy} 无缝切换。
 *
 * <p><b>约定</b>：所有方法接收 / 返回的是相对抽象标识符 {@code fileKey}
 * （如 {@code USR123/2026/08/26/a1b2.pdf}），数据库也只存储该标识，不存储绝对路径。
 */
public interface StorageService {

    /** 保存文件到 {@code fileKey} 对应位置（自动创建父目录，覆盖已存在文件） */
    void save(String fileKey, InputStream inputStream);

    /** 读取 {@code fileKey} 对应的文件内容 */
    InputStream load(String fileKey);

    /** 删除 {@code fileKey} 对应的物理文件 */
    void delete(String fileKey);

    /** 判断 {@code fileKey} 对应的文件是否存在 */
    boolean exists(String fileKey);
}
