/* (C) 2026 Rainier — internal use only. */
package com.rainier.me.controller;

import com.rainier.auth.controller.AuthController;
import com.rainier.authz.AiAuthLevel;
import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.common.exception.UnauthorizedException;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import javax.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * v0.0.69 (A5): {@code POST /api/me/ai-auth-level} — 当前用户写入自己的 AI 授权级别。Token-gated
 * (not admin-gated)：用户对自己授权级别的写权属于自服务。
 */
@RestController
@RequestMapping("/api/me")
public class MeAiAuthLevelController {

  private final UserRepository userRepo;

  public MeAiAuthLevelController(UserRepository userRepo) {
    this.userRepo = userRepo;
  }

  @PostMapping(path = "/ai-auth-level", consumes = "application/json", produces = "application/json")
  @Transactional
  public AiAuthLevelResponse setLevel(
      @RequestBody AiAuthLevelRequest req, HttpServletRequest request) {
    if (req == null || req.getLevel() == null || req.getLevel().trim().isEmpty()) {
      throw new BadRequestException("level is required");
    }
    String level = req.getLevel().trim();
    if (!AiAuthLevel.ALL.contains(level)) {
      throw new BadRequestException("invalid level: " + level);
    }
    String username = currentUsername(request);
    User user =
        userRepo
            .findByLoginName(username)
            .orElseThrow(() -> new NotFoundException("user not found: " + username));
    user.setAiAuthLevel(level);
    userRepo.saveAndFlush(user);
    return new AiAuthLevelResponse(user.getAiAuthLevel());
  }

  private static String currentUsername(HttpServletRequest request) {
    Object username = request.getAttribute(AuthController.ATTR_USERNAME);
    if (!(username instanceof String) || ((String) username).isEmpty()) {
      throw new UnauthorizedException("Missing or invalid token");
    }
    return (String) username;
  }

  /** Request body. */
  public static class AiAuthLevelRequest {
    private String level;

    public String getLevel() {
      return level;
    }

    public void setLevel(String level) {
      this.level = level;
    }
  }

  /** Response body — echoes the persisted level. */
  public static class AiAuthLevelResponse {
    private final String aiAuthLevel;

    public AiAuthLevelResponse(String aiAuthLevel) {
      this.aiAuthLevel = aiAuthLevel;
    }

    public String getAiAuthLevel() {
      return aiAuthLevel;
    }
  }
}
