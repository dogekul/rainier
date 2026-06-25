/* (C) 2026 Rainier — internal use only. */
package com.rainier.capability.controller;

import com.rainier.auth.controller.AuthController;
import com.rainier.capability.dto.UserCapabilityDto;
import com.rainier.capability.service.CapabilityService;
import com.rainier.common.exception.ForbiddenException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.common.exception.UnauthorizedException;
import com.rainier.me.service.MeProfileService;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * v0.0.85 (C5) — {@code GET /api/users/{id}/capabilities}. Reuses the C3 authz rule: caller must be
 * the target user OR a direct manager (see {@link MeProfileService#isDirectManagerOf(Long, Long)}).
 *
 * <p>Intentionally NOT mounted on {@code UserController} (which owns {@code /api/users/{id}}); the
 * sibling {@code UserProfileController} uses the same trick.
 */
@RestController
public class UserCapabilityController {

  private final CapabilityService service;
  private final MeProfileService profileService;
  private final UserRepository userRepo;

  public UserCapabilityController(
      CapabilityService service, MeProfileService profileService, UserRepository userRepo) {
    this.service = service;
    this.profileService = profileService;
    this.userRepo = userRepo;
  }

  @GetMapping(path = "/api/users/{id}/capabilities", produces = "application/json")
  public List<UserCapabilityDto> ofUser(
      @PathVariable("id") Long targetId, HttpServletRequest request) {
    String username = currentUsername(request);
    User caller = userRepo.findByLoginName(username).orElse(null);
    if (caller == null) {
      throw new ForbiddenException("Caller has no matching user record");
    }
    User target =
        userRepo.findById(targetId).orElseThrow(() -> new NotFoundException("user not found"));
    if (caller.getId().equals(target.getId())) {
      return service.listUserCapabilities(target.getId());
    }
    if (profileService.isDirectManagerOf(caller.getId(), target.getId())) {
      return service.listUserCapabilities(target.getId());
    }
    throw new ForbiddenException("Not allowed to view this user's capabilities");
  }

  private static String currentUsername(HttpServletRequest request) {
    Object username = request.getAttribute(AuthController.ATTR_USERNAME);
    if (!(username instanceof String) || ((String) username).isEmpty()) {
      throw new UnauthorizedException("Missing or invalid token");
    }
    return (String) username;
  }
}
