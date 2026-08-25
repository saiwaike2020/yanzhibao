package com.crm.dto.message;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

/**
 * 批量标记消息已读请求。
 */
@Data
public class MarkMessagesReadRequest {

    /** 待标记已读的消息 ID 列表 */
    @NotEmpty(message = "消息 ID 列表不能为空")
    private List<Long> messageIds;
}
