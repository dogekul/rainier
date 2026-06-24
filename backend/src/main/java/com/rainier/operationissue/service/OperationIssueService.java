/* (C) 2026 Rainier — internal use only. */
package com.rainier.operationissue.service;

import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.operation.repository.OperationRepository;
import com.rainier.operationissue.domain.IssueSeverity;
import com.rainier.operationissue.domain.IssueStatus;
import com.rainier.operationissue.domain.OperationIssue;
import com.rainier.operationissue.dto.OperationIssueCreateRequest;
import com.rainier.operationissue.dto.OperationIssueDetail;
import com.rainier.operationissue.dto.OperationIssueUpdateRequest;
import com.rainier.operationissue.repository.OperationIssueRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** v0.0.58 — 运营问题清单业务（创建/列表/编辑/删除）。状态切换通过 update 字段进行。 */
@Service
@Transactional(readOnly = true)
public class OperationIssueService {

  private final OperationIssueRepository repo;
  private final OperationRepository opRepo;
  private final UserRepository userRepo;

  public OperationIssueService(
      OperationIssueRepository repo, OperationRepository opRepo, UserRepository userRepo) {
    this.repo = repo;
    this.opRepo = opRepo;
    this.userRepo = userRepo;
  }

  @Transactional
  public OperationIssueDetail create(Long operationId, OperationIssueCreateRequest req) {
    if (!opRepo.existsById(operationId)) {
      throw new BadRequestException("operation not found: id=" + operationId);
    }
    if (!userRepo.existsById(req.getReporterUserId())) {
      throw new BadRequestException("reporter user not found: id=" + req.getReporterUserId());
    }
    if (req.getAssigneeUserId() != null && !userRepo.existsById(req.getAssigneeUserId())) {
      throw new BadRequestException("assignee user not found: id=" + req.getAssigneeUserId());
    }
    String severity = req.getSeverity() == null ? IssueSeverity.MEDIUM : req.getSeverity();
    if (!IssueSeverity.ALL.contains(severity)) {
      throw new BadRequestException("invalid severity: " + severity);
    }
    OperationIssue i = new OperationIssue();
    i.setOperationId(operationId);
    i.setTitle(req.getTitle());
    i.setDescription(req.getDescription());
    i.setSeverity(severity);
    i.setStatus(IssueStatus.OPEN);
    i.setReporterUserId(req.getReporterUserId());
    i.setAssigneeUserId(req.getAssigneeUserId());
    return enrich(repo.saveAndFlush(i));
  }

  public List<OperationIssueDetail> listByOperation(Long operationId) {
    return repo.findByOperationIdOrderByIdDesc(operationId).stream()
        .map(this::enrich)
        .collect(Collectors.toList());
  }

  public OperationIssueDetail findById(Long id) {
    return enrich(getOrThrow(id));
  }

  @Transactional
  public OperationIssueDetail update(Long id, OperationIssueUpdateRequest req) {
    OperationIssue i = getOrThrow(id);
    if (req.getSeverity() != null && !IssueSeverity.ALL.contains(req.getSeverity())) {
      throw new BadRequestException("invalid severity: " + req.getSeverity());
    }
    if (req.getStatus() != null && !IssueStatus.ALL.contains(req.getStatus())) {
      throw new BadRequestException("invalid status: " + req.getStatus());
    }
    if (req.getAssigneeUserId() != null && !userRepo.existsById(req.getAssigneeUserId())) {
      throw new BadRequestException("assignee user not found: id=" + req.getAssigneeUserId());
    }
    i.setTitle(req.getTitle());
    i.setDescription(req.getDescription());
    if (req.getSeverity() != null) {
      i.setSeverity(req.getSeverity());
    }
    if (req.getStatus() != null) {
      i.setStatus(req.getStatus());
    }
    i.setAssigneeUserId(req.getAssigneeUserId());
    if (req.getCloseReason() != null) {
      i.setCloseReason(req.getCloseReason());
    }
    return enrich(repo.saveAndFlush(i));
  }

  @Transactional
  public void delete(Long id) {
    repo.delete(getOrThrow(id));
  }

  private OperationIssue getOrThrow(Long id) {
    return repo.findById(id)
        .orElseThrow(() -> new NotFoundException("operation issue not found: id=" + id));
  }

  /** Enrich with reporter/assignee user names (best-effort, no failure if user soft-deleted). */
  private OperationIssueDetail enrich(OperationIssue i) {
    OperationIssueDetail d = OperationIssueDetail.from(i);
    if (i.getReporterUserId() != null) {
      userRepo
          .findById(i.getReporterUserId())
          .map(User::getName)
          .ifPresent(d::setReporterName);
    }
    if (i.getAssigneeUserId() != null) {
      userRepo
          .findById(i.getAssigneeUserId())
          .map(User::getName)
          .ifPresent(d::setAssigneeName);
    }
    return d;
  }
}
