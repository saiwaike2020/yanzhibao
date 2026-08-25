package com.crm.dto.user;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新个人资料请求。
 */
@Data
public class UpdateProfileRequest {

    /** 昵称 */
    @Size(max = 64, message = "昵称不能超过 64 个字符")
    private String nickname;

    /** 头像 URL */
    @Size(max = 255, message = "头像 URL 过长")
    private String avatarUrl;
}
