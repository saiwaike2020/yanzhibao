package com.crm.security.jwt;

import com.crm.common.enums.SystemRole;
import com.crm.security.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT 生成与解析工具（HS256）。
 *
 * <p>Token 载荷：subject=userId，自定义 claim 包含 userNo、phone、systemRole。
 */
@Component
public class JwtTokenProvider {

    @Value("${crm.jwt.secret}")
    private String secret;

    @Value("${crm.jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 生成 JWT Token */
    public String generateToken(LoginUser user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(String.valueOf(user.getUserId()))
                .claim("userNo", user.getUserNo())
                .claim("phone", user.getPhone())
                .claim("systemRole", user.getSystemRole() == null ? SystemRole.NONE.name() : user.getSystemRole().name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    /** 解析并校验 Token，返回登录用户信息；失败抛出 JwtException */
    public LoginUser parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Long userId = Long.valueOf(claims.getSubject());
        String userNo = claims.get("userNo", String.class);
        String phone = claims.get("phone", String.class);
        SystemRole systemRole = SystemRole.valueOf(claims.get("systemRole", String.class));
        return new LoginUser(userId, userNo, phone, systemRole);
    }

    /** Token 有效期（秒），用于响应中的 expiresIn 字段 */
    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }
}
