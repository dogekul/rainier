/* (C) 2026 Rainier — internal use only. */
package com.rainier.link.service;

import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.link.domain.EntityLink;
import com.rainier.link.domain.LinkTargetType;
import com.rainier.link.domain.LinkType;
import com.rainier.link.dto.LinkCreateRequest;
import com.rainier.link.dto.LinkDetail;
import com.rainier.link.repository.EntityLinkRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business operations for {@link EntityLink} (v0.0.31). */
@Service
@Transactional(readOnly = true)
public class EntityLinkService {

  private final EntityLinkRepository repo;

  public EntityLinkService(EntityLinkRepository repo) {
    this.repo = repo;
  }

  @Transactional
  public LinkDetail create(LinkCreateRequest req) {
    if (!LinkTargetType.ALL.contains(req.getTargetType())) {
      throw new BadRequestException("invalid targetType: " + req.getTargetType());
    }
    if (!LinkType.ALL.contains(req.getLinkType())) {
      throw new BadRequestException("invalid linkType: " + req.getLinkType());
    }
    EntityLink l = new EntityLink();
    l.setTargetType(req.getTargetType());
    l.setTargetId(req.getTargetId());
    l.setLinkType(req.getLinkType());
    l.setLabel(req.getLabel());
    l.setUrl(req.getUrl());
    return LinkDetail.from(repo.saveAndFlush(l));
  }

  public List<LinkDetail> list(String targetType, Long targetId) {
    return repo.findByTargetTypeAndTargetIdOrderByCreateTimeAsc(targetType, targetId).stream()
        .map(LinkDetail::from)
        .collect(Collectors.toList());
  }

  @Transactional
  public void delete(Long id) {
    EntityLink l = repo.findById(id).orElseThrow(() -> new NotFoundException("link not found: id=" + id));
    repo.delete(l);
  }
}
