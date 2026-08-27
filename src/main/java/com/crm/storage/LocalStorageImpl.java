package com.crm.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import lombok.extern.slf4j.Slf4j;

/**
 * 本地文件系统存储实现（当前默认存储策略）。
 *
 * <p>物理文件存放于配置的根目录（{@code crm.storage.local.root}，默认 {@code ./storage}）下，
 * 以相对 {@code fileKey} 定位；数据库仅记录 {@code fileKey}。
 */
@Slf4j
public class LocalStorageImpl implements StorageService {

    private final Path root;

    public LocalStorageImpl(String root) {
        this.root = Paths.get(root).toAbsolutePath().normalize();
        log.info("[本地存储] 初始化根目录: {}", this.root);
    }

    @Override
    public void save(String fileKey, InputStream inputStream) {
        Path target = resolve(fileKey);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("保存文件失败: " + fileKey, e);
        }
    }

    @Override
    public InputStream load(String fileKey) {
        Path target = resolve(fileKey);
        try {
            return Files.newInputStream(target);
        } catch (IOException e) {
            throw new IllegalStateException("读取文件失败: " + fileKey, e);
        }
    }

    @Override
    public void delete(String fileKey) {
        Path target = resolve(fileKey);
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new IllegalStateException("删除文件失败: " + fileKey, e);
        }
    }

    @Override
    public boolean exists(String fileKey) {
        return Files.exists(resolve(fileKey));
    }

    /** 规范化 fileKey 并防止路径穿越（Zip Slip 等） */
    private Path resolve(String fileKey) {
        Path path = root.resolve(fileKey).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("非法的文件标识: " + fileKey);
        }
        return path;
    }
}
