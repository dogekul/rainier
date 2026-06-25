/* (C) 2026 Rainier — internal use only. */
package com.rainier.notification.controller;

import com.rainier.auth.controller.AuthController;
import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.common.exception.UnauthorizedException;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.email.EmailMessage;
import com.rainier.email.EmailSender;
import com.rainier.email.SendResult;
import com.rainier.notification.domain.Notification;
import com.rainier.notification.dto.NotificationDetail;
import com.rainier.notification.repository.NotificationRepository;
import com.rainier.notification.service.NotificationService;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站内通知中心 read/write endpoints (v0.0.72, A8). 全员 token-gated；用户只能访问自己的通知。
 */
@RestController
@RequestMapping("/api/me")
public class MeNotificationsController {

  private final NotificationService service;
  private final UserRepository userRepo;
  private final NotificationRepository notifRepo;
  private final EmailSender emailSender;

  public MeNotificationsController(
      NotificationService service,
      UserRepository userRepo,
      NotificationRepository notifRepo,
      EmailSender emailSender) {
    this.service = service;
    this.userRepo = userRepo;
    this.notifRepo = notifRepo;
    this.emailSender = emailSender;
  }

  @GetMapping(path = "/notifications", produces = "application/json")
  public PageResponse<NotificationDetail> list(
      HttpServletRequest request,
      @RequestParam(name = "onlyUnread", required = false, defaultValue = "false")
          boolean onlyUnread,
      @Valid PageParams pageParams) {
    Long userId = currentUserId(request);
    PageRequest pr = PageRequest.of(pageParams.getPage(), pageParams.getSize());
    Page<Notification> page = service.listFor(userId, pr, onlyUnread);
    return PageResponse.of(
        page.getContent().stream().map(NotificationDetail::from).collect(Collectors.toList()),
        pageParams.getPage(),
        pageParams.getSize(),
        page.getTotalElements());
  }

  @PostMapping(path = "/notifications/{id}/read", produces = "application/json")
  public NotificationDetail markRead(HttpServletRequest request, @PathVariable("id") Long id) {
    Long userId = currentUserId(request);
    return NotificationDetail.from(service.markRead(id, userId));
  }

  @PostMapping(path = "/notifications/read-all", produces = "application/json")
  public Map<String, Object> markAllRead(HttpServletRequest request) {
    Long userId = currentUserId(request);
    int updated = service.markAllRead(userId);
    Map<String, Object> out = new HashMap<String, Object>();
    out.put("updated", updated);
    return out;
  }

  /**
   * v0.0.92 (D4) — 把当前用户的某条通知通过 {@link EmailSender} 转发到自己的 emailAddress。 仅能转发自己的通知（与
   * markRead 同样的归属校验）；user.emailAddress 缺失 → 400。
   */
  @PostMapping(path = "/notifications/{id}/email", produces = "application/json")
  public Map<String, Object> forwardToEmail(
      HttpServletRequest request, @PathVariable("id") Long id) {
    User me = currentUser(request);
    if (me.getEmailAddress() == null || me.getEmailAddress().trim().isEmpty()) {
      throw new BadRequestException("current user has no emailAddress");
    }
    Notification n =
        notifRepo
            .findById(id)
            .orElseThrow(() -> new NotFoundException("notification not found: id=" + id));
    if (!n.getUserId().equals(me.getId())) {
      throw new NotFoundException("notification not found: id=" + id);
    }
    EmailMessage msg = new EmailMessage();
    msg.setFrom("noreply@rainier.local");
    msg.setTo(Arrays.asList(me.getEmailAddress()));
    msg.setSubject("[Rainier] " + (n.getTitle() == null ? "(no title)" : n.getTitle()));
    msg.setBodyText(n.getBody());
    SendResult r = emailSender.send(msg);
    Map<String, Object> out = new HashMap<String, Object>();
    out.put("success", r.isSuccess());
    out.put("providerId", r.getProviderId());
    out.put("errorMessage", r.getErrorMessage());
    return out;
  }

  private User currentUser(HttpServletRequest request) {
    Object username = request.getAttribute(AuthController.ATTR_USERNAME);
    if (!(username instanceof String) || ((String) username).isEmpty()) {
      throw new UnauthorizedException("Missing or invalid token");
    }
    return userRepo
        .findByLoginName((String) username)
        .orElseThrow(() -> new UnauthorizedException("Unknown user: " + username));
  }

  private Long currentUserId(HttpServletRequest request) {
    Object username = request.getAttribute(AuthController.ATTR_USERNAME);
    if (!(username instanceof String) || ((String) username).isEmpty()) {
      throw new UnauthorizedException("Missing or invalid token");
    }
    User me =
        userRepo
            .findByLoginName((String) username)
            .orElseThrow(() -> new UnauthorizedException("Unknown user: " + username));
    return me.getId();
  }
}
