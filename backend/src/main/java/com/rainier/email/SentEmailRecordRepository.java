/* (C) 2026 Rainier — internal use only. */
package com.rainier.email;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** v0.0.92 (D4) — {@link SentEmailRecord} 仓库。 */
public interface SentEmailRecordRepository extends JpaRepository<SentEmailRecord, Long> {

  Page<SentEmailRecord> findAllByOrderBySentAtDescIdDesc(Pageable pageable);
}
