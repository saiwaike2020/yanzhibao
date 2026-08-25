package com.crm.repository;

import com.crm.common.enums.SystemRole;
import com.crm.entity.SysUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 用户主表仓储。
 */
public interface SysUserRepository extends JpaRepository<SysUser, Long> {

    Optional<SysUser> findByPhone(String phone);

    Optional<SysUser> findByUserNo(String userNo);

    boolean existsByPhone(String phone);

    boolean existsByUserNo(String userNo);

    List<SysUser> findBySystemRole(SystemRole systemRole);

    /** 关键字搜索（昵称 / 用户编号 / 手机号），用于系统管理员用户管理 */
    @Query("""
            SELECT u FROM SysUser u
            WHERE :keyword IS NULL
               OR u.nickname LIKE %:keyword%
               OR u.userNo LIKE %:keyword%
               OR u.phone LIKE %:keyword%
            """)
    Page<SysUser> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
