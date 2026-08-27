package com.crm.process;

import com.crm.common.enums.ResourceStatus;
import com.crm.entity.Resource;
import com.crm.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Mock 文件处理器：仅输出日志并标记资源处理完成。
 *
 * <p>当前用于验证「上传 → 异步处理 → 状态流转」全链路；
 * 未来接入真实解析逻辑时替换本实现即可。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MockFileProcessor implements FileProcessor {

    private final ResourceRepository resourceRepository;

    @Override
    public void process(Resource resource) {
        log.info("[Mock文件处理] 开始处理 resourceId={}, fileKey={}, name={}, type={}",
                resource.getResourceId(), resource.getFileKey(), resource.getName(), resource.getFileType());

        // 标记处理完成
        resource.setStatus(ResourceStatus.PROCESSED);
        resourceRepository.save(resource);

        log.info("[Mock文件处理] 处理完成 resourceId={}, 状态已更新为 PROCESSED", resource.getResourceId());
    }
}
