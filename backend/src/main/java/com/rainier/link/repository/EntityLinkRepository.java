/* (C) 2026 Rainier — internal use only. */
package com.rainier.link.repository;

import com.rainier.link.domain.EntityLink;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for {@link EntityLink} (v0.0.31). */
@Repository
public interface EntityLinkRepository extends JpaRepository<EntityLink, Long> {

  List<EntityLink> findByTargetTypeAndTargetIdOrderByCreateTimeAsc(String targetType, Long targetId);

  long countByTargetTypeAndTargetId(String targetType, Long targetId);
}
