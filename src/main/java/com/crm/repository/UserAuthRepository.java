package com.crm.repository;

import com.crm.common.enums.AuthType;
import com.crm.entity.UserAuth;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 认证标识表仓储。
 */
public interface UserAuthRepository extends JpaRepository<UserAuth, Long> {

    Optional<UserAuth> findByAuthTypeAndIdentifier(AuthType authType, String identifier);

    List<UserAuth> findByUserId(Long userId);

    boolean existsByAuthTypeAndIdentifier(AuthType authType, String identifier);

    long countByUserId(Long userId);
}
