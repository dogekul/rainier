/* (C) 2026 Rainier — internal use only. */
package com.rainier.weekly.controller;

import com.rainier.auth.controller.AuthController;
import com.rainier.common.exception.NotFoundException;
import com.rainier.common.exception.UnauthorizedException;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import com.rainier.weekly.domain.WeeklyDraft;
import com.rainier.weekly.dto.GenerateWeeklyDraftRequest;
import com.rainier.weekly.dto.WeeklyDraftResponse;
import com.rainier.weekly.service.WeeklyDraftService;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Self-scoped weekly draft endpoints (v0.0.71). Token-gated, NOT admin-gated — every user can draft
 * their own weekly. Reading / writing someone else's draft is out of scope (later admin endpoint).
 */
@RestController
@RequestMapping("/api/me/weekly-drafts")
public class MeWeeklyDraftController {

  private final WeeklyDraftService service;
  private final UserRepository userRepo;

  public MeWeeklyDraftController(WeeklyDraftService service, UserRepository userRepo) {
    this.service = service;
    this.userRepo = userRepo;
  }

  @PostMapping("/generate")
  public WeeklyDraftResponse generate(
      @Valid @RequestBody GenerateWeeklyDraftRequest req, HttpServletRequest request) {
    Long userId = currentUserId(request);
    WeeklyDraft d = service.generate(userId, req.getPeriodStart(), req.getPeriodEnd());
    return WeeklyDraftResponse.from(d);
  }

  @GetMapping
  public PageResponse<WeeklyDraftResponse> list(
      @Valid PageParams page, HttpServletRequest request) {
    Long userId = currentUserId(request);
    Page<WeeklyDraft> result =
        service.list(userId, PageRequest.of(page.getPage(), page.getSize()));
    List<WeeklyDraftResponse> rows =
        result.stream().map(WeeklyDraftResponse::from).collect(Collectors.toList());
    return PageResponse.of(rows, page.getPage(), page.getSize(), result.getTotalElements());
  }

  @PostMapping("/{id}/accept")
  public WeeklyDraftResponse accept(@PathVariable Long id, HttpServletRequest request) {
    // identity check is intentionally light here (token-gated only) — A8 will tighten with
    // service-side ownership enforcement once cross-user view is added.
    currentUserId(request);
    return WeeklyDraftResponse.from(service.accept(id));
  }

  private Long currentUserId(HttpServletRequest request) {
    Object u = request.getAttribute(AuthController.ATTR_USERNAME);
    if (!(u instanceof String) || ((String) u).isEmpty()) {
      throw new UnauthorizedException("Missing or invalid token");
    }
    String loginName = (String) u;
    User me =
        userRepo
            .findByLoginName(loginName)
            .orElseThrow(() -> new NotFoundException("user not found: " + loginName));
    return me.getId();
  }
}
