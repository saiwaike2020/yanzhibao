package com.crm.process;

import com.crm.entity.Resource;

/**
 * 文件处理抽象接口。
 *
 * <p>当前实现 {@link MockFileProcessor}（仅输出日志，标记处理完成）；
 * 未来可替换为真实解析（如 PDF/Word 转 Markdown、简历信息抽取等），
 * 业务侧（监听器）无需改动。
 */
public interface FileProcessor {

    /** 处理单个文件资源，处理后标记资源状态为 PROCESSED */
    void process(Resource resource);
}
