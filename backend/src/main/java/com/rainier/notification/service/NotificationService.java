/* (C) 2026 Rainier — internal use only. */
package com.rainier.notification.service;

import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.notification.domain.Notification;
import com.rainier.notification.repository.NotificationRepository;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 站内通知 service（v0.0.72, A8）。最小内核：send / listFor(onlyUnread) / markRead / markAllRead。
 *
 * <p>NOTE：本服务仍保持通用 send 语义；风险通知去重由 RiskService 基于 rule/entity 指纹处理。
 */
@Service
@Transactional(readOnly = true)
public class NotificationService {

  private final NotificationRepository repo;

  public NotificationService(NotificationRepository repo) {
    this.repo = repo;
  }

  /** 写入一条新通知。userId / title / level 必填。 */
  @Transactional
  public Notification send(
      Long userId,
      String title,
      String body,
      String level,
      String entityType,
      Long entityId) {
    if (userId == null) {
      throw new BadRequestException("userId is required");
    }
    if (title == null || title.trim().isEmpty()) {
      throw new BadRequestException("title is required");
    }
    if (level == null || level.trim().isEmpty()) {
      throw new BadRequestException("level is required");
    }
    Notification n = new Notification();
    n.setUserId(userId);
    n.setTitle(title);
    n.setBody(body);
    n.setLevel(level);
    n.setEntityType(entityType);
    n.setEntityId(entityId);
    n.setCreatedAt(LocalDateTime.now());
    n.setReadAt(null);
    return repo.saveAndFlush(n);
  }

  /** 当前用户的通知列表；onlyUnread=true 时仅返回 readAt IS NULL。 */
  public Page<Notification> listFor(Long userId, Pageable pageable, boolean onlyUnread) {
    if (userId == null) {
      throw new BadRequestException("userId is required");
    }
    if (onlyUnread) {
      return repo.findByUserIdAndReadAtIsNullOrderByCreatedAtDescIdDesc(userId, pageable);
    }
    return repo.findByUserIdOrderByCreatedAtDescIdDesc(userId, pageable);
  }

  /** 单条标记已读；若已读则保持 readAt 不变（幂等）。 */
  @Transactional
  public Notification markRead(Long id, Long userId) {
    Notification n =
        repo.findById(id)
            .orElseThrow(() -> new NotFoundException("notification not found: id=" + id));
    if (!n.getUserId().equals(userId)) {
      throw new NotFoundException("notification not found: id=" + id);
    }
    if (n.getReadAt() == null) {
      n.setReadAt(LocalDateTime.now());
      n = repo.saveAndFlush(n);
    }
    return n;
  }

  /** 把当前用户所有未读置已读，返回受影响行数。 */
  @Transactional
  public int markAllRead(Long userId) {
    if (userId == null) {
      throw new BadRequestException("userId is required");
    }
    return repo.markAllReadForUser(userId, LocalDateTime.now());
  }
}
