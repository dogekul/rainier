/* (C) 2026 Rainier — internal use only. */
package com.rainier.projectimplementation.service;

import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.projectimplementation.domain.ProjectImplementation;
import com.rainier.projectimplementation.dto.ProjectImplementationDetail;
import com.rainier.projectimplementation.dto.ProjectImplementationUpsertRequest;
import com.rainier.projectimplementation.repository.ProjectImplementationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * v0.0.89 — Project 立项后施工内容表单的服务。Upsert 按 projectId 唯一；GET 不存在 → 404。
 */
@Service
@Transactional(readOnly = true)
public class ProjectImplementationService {

  private final ProjectImplementationRepository repo;
  private final ProjectRepository projectRepo;

  public ProjectImplementationService(
      ProjectImplementationRepository repo, ProjectRepository projectRepo) {
    this.repo = repo;
    this.projectRepo = projectRepo;
  }

  public ProjectImplementationDetail findByProjectId(Long projectId) {
    ProjectImplementation p =
        repo.findByProjectId(projectId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "project implementation not found: projectId=" + projectId));
    return ProjectImplementationDetail.from(p);
  }

  @Transactional
  public ProjectImplementationDetail createOrUpdate(
      Long projectId, ProjectImplementationUpsertRequest req) {
    if (!projectRepo.existsById(projectId)) {
      throw new BadRequestException("project not found: id=" + projectId);
    }
    ProjectImplementation entity =
        repo.findByProjectId(projectId)
            .orElseGet(
                () -> {
                  ProjectImplementation fresh = new ProjectImplementation();
                  fresh.setProjectId(projectId);
                  return fresh;
                });
    entity.setScopeMarkdown(req.getScopeMarkdown());
    entity.setEstimatedManDays(req.getEstimatedManDays());
    entity.setRiskNotes(req.getRiskNotes());
    entity.setKeyMilestonesJson(req.getKeyMilestonesJson());
    return ProjectImplementationDetail.from(repo.saveAndFlush(entity));
  }
}
