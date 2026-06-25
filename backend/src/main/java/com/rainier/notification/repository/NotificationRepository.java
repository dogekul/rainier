/* (C) 2026 Rainier — internal use only. */
package com.rainier.notification.repository;

import com.rainier.notification.domain.Notification;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repository for {@link Notification} (v0.0.72, A8). */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

  Page<Notification> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

  Page<Notification> findByUserIdAndReadAtIsNullOrderByCreatedAtDescIdDesc(
      Long userId, Pageable pageable);

  @Modifying
  @Query(
      "UPDATE Notification n SET n.readAt = :readAt "
          + "WHERE n.userId = :userId AND n.readAt IS NULL")
  int markAllReadForUser(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);
}
