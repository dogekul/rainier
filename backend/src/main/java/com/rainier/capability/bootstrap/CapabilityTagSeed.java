/* (C) 2026 Rainier — internal use only. */
package com.rainier.capability.bootstrap;

import com.rainier.capability.domain.CapabilityTag;
import com.rainier.capability.repository.CapabilityTagRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * v0.0.85 (C5) — demo seed for the capability tag dictionary so the「我的档案」能力栏目有数据
 * 可选。Gated on {@code app.demo.capability-seed.enabled} (false in the test profile so tests get a
 * clean table) and idempotent (only seeds when {@code rainier_capability_tag} is empty).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CapabilityTagSeed implements CommandLineRunner {

  private static final Logger LOG = LoggerFactory.getLogger(CapabilityTagSeed.class);

  private final boolean enabled;
  private final CapabilityTagRepository repo;

  public CapabilityTagSeed(
      @Value("${app.demo.capability-seed.enabled:true}") boolean enabled,
      CapabilityTagRepository repo) {
    this.enabled = enabled;
    this.repo = repo;
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (!enabled) {
      return;
    }
    if (repo.count() > 0) {
      return; // idempotent
    }
    List<CapabilityTag> seeds = new ArrayList<CapabilityTag>();
    for (String n : Arrays.asList("Java", "Frontend", "K8s", "SQL")) {
      seeds.add(make(n, "TECH"));
    }
    for (String n : Arrays.asList("用户研究", "需求分析", "数据分析")) {
      seeds.add(make(n, "PRODUCT"));
    }
    for (String n : Arrays.asList("沟通", "跨团队协作", "带人")) {
      seeds.add(make(n, "SOFT"));
    }
    repo.saveAll(seeds);
    LOG.info("CapabilityTagSeed: seeded {} capability tags", seeds.size());
  }

  private static CapabilityTag make(String name, String category) {
    CapabilityTag t = new CapabilityTag();
    t.setName(name);
    t.setCategory(category);
    return t;
  }
}
