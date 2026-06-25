/* (C) 2026 Rainier — internal use only. */
package com.rainier.user.controller;

import com.rainier.auth.controller.AuthController;
import com.rainier.common.exception.ForbiddenException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.common.exception.UnauthorizedException;
import com.rainier.me.dto.ProfileResponse;
import com.rainier.me.service.MeProfileService;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * v0.0.83 (C3): {@code GET /api/users/{id}/profile} — read the profile of either yourself or a
 * direct subordinate. Same {@link ProfileResponse} shape as {@code /api/me/profile}, but the gate is
 * tighter: caller must equal target OR be an active HEAD of the target's primary org (or that org's
 * direct parent — at most ONE level up).
 *
 * <p>Intentionally NOT mounted on {@link UserController} — that controller owns
 * {@code GET /api/users/{id}} (UserDetail). Spring's path matcher routes {@code /{id}/profile} to
 * this controller via its more specific mapping.
 */
@RestController
public class UserProfileController {

  private final MeProfileService profileService;
  private final UserRepository userRepo;

  public UserProfileController(MeProfileService profileService, UserRepository userRepo) {
    this.profileService = profileService;
    this.userRepo = userRepo;
  }

  @GetMapping(path = "/api/users/{id}/profile", produces = "application/json")
  public ProfileResponse profile(@PathVariable("id") Long targetId, HttpServletRequest request) {
    String username = currentUsername(request);
    User caller = userRepo.findByLoginName(username).orElse(null);
    if (caller == null) {
      // Token valid but no matching user — cannot read anyone else's profile.
      throw new ForbiddenException("Caller has no matching user record");
    }
    User target =
        userRepo.findById(targetId).orElseThrow(() -> new NotFoundException("user not found"));

    if (caller.getId().equals(target.getId())) {
      return profileService.profileOfUserId(target.getId());
    }
    if (profileService.isDirectManagerOf(caller.getId(), target.getId())) {
      return profileService.profileOfUserId(target.getId());
    }
    throw new ForbiddenException("Not allowed to view this user's profile");
  }

  private static String currentUsername(HttpServletRequest request) {
    Object username = request.getAttribute(AuthController.ATTR_USERNAME);
    if (!(username instanceof String) || ((String) username).isEmpty()) {
      throw new UnauthorizedException("Missing or invalid token");
    }
    return (String) username;
  }
}
