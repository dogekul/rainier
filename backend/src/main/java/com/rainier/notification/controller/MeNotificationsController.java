/* (C) 2026 Rainier — internal use only. */
package com.rainier.notification.controller;

import com.rainier.auth.controller.AuthController;
import com.rainier.common.exception.UnauthorizedException;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.notification.domain.Notification;
import com.rainier.notification.dto.NotificationDetail;
import com.rainier.notification.service.NotificationService;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
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

  public MeNotificationsController(NotificationService service, UserRepository userRepo) {
    this.service = service;
    this.userRepo = userRepo;
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
