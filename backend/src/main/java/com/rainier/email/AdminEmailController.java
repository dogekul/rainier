/* (C) 2026 Rainier — internal use only. */
package com.rainier.email;

import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only 邮件发送记录查询（v0.0.92, D4）。
 *
 * <p>路径 {@code /api/emails} 由 {@link com.rainier.authz.AdminPaths} TIER_A 拦截：GET 也需要 admin。
 */
@RestController
@RequestMapping("/api/emails")
public class AdminEmailController {

  private final SentEmailRecordRepository repo;

  public AdminEmailController(SentEmailRecordRepository repo) {
    this.repo = repo;
  }

  @GetMapping(produces = "application/json")
  public PageResponse<SentEmailDetail> list(@Valid PageParams pageParams) {
    PageRequest pr = PageRequest.of(pageParams.getPage(), pageParams.getSize());
    Page<SentEmailRecord> page = repo.findAllByOrderBySentAtDescIdDesc(pr);
    return PageResponse.of(
        page.getContent().stream().map(SentEmailDetail::from).collect(Collectors.toList()),
        pageParams.getPage(),
        pageParams.getSize(),
        page.getTotalElements());
  }
}
