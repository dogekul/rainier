/* (C) 2026 Rainier — internal use only. */
package com.rainier.weekly.repository;

import com.rainier.weekly.domain.WeeklyDraft;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for {@link WeeklyDraft} (v0.0.71). */
@Repository
public interface WeeklyDraftRepository extends JpaRepository<WeeklyDraft, Long> {

  Page<WeeklyDraft> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);
}
