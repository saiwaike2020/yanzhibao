package com.crm.repository;

import com.crm.entity.UserMessage;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 用户消息中心表仓储。
 */
public interface UserMessageRepository extends JpaRepository<UserMessage, Long> {

    /** 分页查询某用户的消息（调用方按 created_at 倒序排序） */
    Page<UserMessage> findByUserId(Long userId, Pageable pageable);

    /** 未读消息数 */
    long countByUserIdAndIsRead(Long userId, Integer isRead);

    /** 查询某用户的某条消息（校验归属） */
    Optional<UserMessage> findByMessageIdAndUserId(Long messageId, Long userId);

    /** 将某用户全部未读消息标记为已读，返回受影响行数 */
    @Modifying
    @Query("UPDATE UserMessage m SET m.isRead = 1, m.readAt = CURRENT_TIMESTAMP "
            + "WHERE m.userId = :userId AND m.isRead = 0")
    int markAllRead(@Param("userId") Long userId);
}
