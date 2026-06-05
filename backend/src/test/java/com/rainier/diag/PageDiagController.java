/* (C) 2026 Rainier — internal use only. */
package com.rainier.diag;

import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import java.util.Collections;
import javax.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only endpoint exercising {@link PageParams} validation so {@link
 * com.rainier.common.exception.GlobalExceptionHandler} can be observed translating violations into
 * 400 JSON. Active only in the {@code test} profile.
 */
@RestController
@Profile("test")
public class PageDiagController {

  @GetMapping("/api/_diag/page")
  public PageResponse<String> page(@Valid PageParams params) {
    return PageResponse.of(Collections.emptyList(), params.getPage(), params.getSize(), 0L);
  }
}
