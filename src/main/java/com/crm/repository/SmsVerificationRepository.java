package com.crm.repository;

import com.crm.common.enums.SmsScene;
import com.crm.entity.SmsVerification;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 短信验证码记录表仓储。
 */
public interface SmsVerificationRepository extends JpaRepository<SmsVerification, Long> {

    /** 查询某手机号某场景最近一条验证码记录 */
    Optional<SmsVerification> findTopByPhoneAndSceneOrderByCreatedAtDesc(String phone, SmsScene scene);

    /** 统计某手机号某场景在指定时间之后的发送次数（用于 60 秒限频） */
    long countByPhoneAndSceneAndCreatedAtAfter(String phone, SmsScene scene, LocalDateTime after);
}
