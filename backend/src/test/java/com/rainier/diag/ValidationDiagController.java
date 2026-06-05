/* (C) 2026 Rainier — internal use only. */
package com.rainier.diag;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only endpoint exercising Bean Validation so {@code GlobalExceptionHandler} can be observed
 * translating {@code MethodArgumentNotValidException} into a 400 JSON with {@code fieldErrors[]}.
 *
 * <p>Active only in the {@code test} profile.
 */
@RestController
@Profile("test")
public class ValidationDiagController {

  @PostMapping("/api/_diag/echo")
  public EchoRequest echo(@Valid @RequestBody EchoRequest body) {
    return body;
  }

  public static class EchoRequest {
    @NotBlank private String name;

    @Size(min = 2, max = 20)
    private String nickname;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getNickname() {
      return nickname;
    }

    public void setNickname(String nickname) {
      this.nickname = nickname;
    }
  }
}
