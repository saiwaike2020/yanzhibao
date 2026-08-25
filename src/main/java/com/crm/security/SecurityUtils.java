package com.crm.security;

import com.crm.common.exception.BusinessException;
import com.crm.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具：获取当前登录用户。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** 获取当前登录用户，未认证时抛出 401 */
    public static LoginUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }

    /** 获取当前登录用户 ID */
    public static Long getCurrentUserId() {
        return getCurrentUser().getUserId();
    }
}
