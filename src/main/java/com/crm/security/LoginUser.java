package com.crm.security;

import com.crm.common.enums.SystemRole;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 当前登录用户（JWT 解析出的安全主体）。
 */
public class LoginUser implements UserDetails {

    private final Long userId;
    private final String userNo;
    private final String phone;
    private final SystemRole systemRole;

    public LoginUser(Long userId, String userNo, String phone, SystemRole systemRole) {
        this.userId = userId;
        this.userNo = userNo;
        this.phone = phone;
        this.systemRole = systemRole;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserNo() {
        return userNo;
    }

    public String getPhone() {
        return phone;
    }

    public SystemRole getSystemRole() {
        return systemRole;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (systemRole != null) {
            switch (systemRole) {
                case SYSTEM_ADMIN -> authorities.add(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"));
                case AUDITOR -> authorities.add(new SimpleGrantedAuthority("ROLE_AUDITOR"));
                case CUSTOMER_SERVICE -> authorities.add(new SimpleGrantedAuthority("ROLE_CUSTOMER_SERVICE"));
                default -> {
                    // NONE：仅保留基础 ROLE_USER
                }
            }
        }
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return phone;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
