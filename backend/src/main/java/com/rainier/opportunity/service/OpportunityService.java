/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.service;

import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.ConflictException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.opportunity.domain.ArtifactType;
import com.rainier.opportunity.domain.GateDecision;
import com.rainier.opportunity.domain.Opportunity;
import com.rainier.opportunity.domain.OpportunityArtifact;
import com.rainier.opportunity.domain.OpportunityStage;
import com.rainier.opportunity.domain.OpportunityStatus;
import com.rainier.opportunity.domain.TransitionArtifactRules;
import com.rainier.opportunity.dto.ArtifactInput;
import com.rainier.opportunity.dto.OpportunityCreateRequest;
import com.rainier.opportunity.dto.OpportunityDetail;
import com.rainier.opportunity.dto.OpportunityInitiateRequest;
import com.rainier.opportunity.dto.OpportunityUpdateRequest;
import com.rainier.customer.domain.Customer;
import com.rainier.customer.repository.CustomerRepository;
import com.rainier.opportunity.repository.OpportunityArtifactRepository;
import com.rainier.opportunity.repository.OpportunityRepository;
import com.rainier.product.repository.ProductRepository;
import com.rainier.project.domain.Project;
import com.rainier.project.domain.ProjectType;
import com.rainier.project.dto.ProjectCreateRequest;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.project.service.ProjectService;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pre-sales opportunity pipeline (v0.0.44). Stage machine LEAD→CONTRACT gated at 商机/投标/合同; WON at
 * 合同签订, LOST on any gate REJECT; 立项 (initiate) links the delivery Project once WON.
 */
@Service
@Transactional(readOnly = true)
public class OpportunityService {

  private final OpportunityRepository repo;
  private final UserRepository userRepo;
  private final ProjectRepository projectRepo;
  private final OpportunityArtifactRepository artifactRepo;
  private final ProductRepository productRepo;
  private final CustomerRepository customerRepo;
  private final ProjectService projectService;

  public OpportunityService(
      OpportunityRepository repo,
      UserRepository userRepo,
      ProjectRepository projectRepo,
      OpportunityArtifactRepository artifactRepo,
      ProductRepository productRepo,
      CustomerRepository customerRepo,
      ProjectService projectService) {
    this.repo = repo;
    this.userRepo = userRepo;
    this.projectRepo = projectRepo;
    this.artifactRepo = artifactRepo;
    this.productRepo = productRepo;
    this.customerRepo = customerRepo;
    this.projectService = projectService;
  }

  @Transactional
  public OpportunityDetail create(OpportunityCreateRequest req) {
    validateOwners(
        req.getCommercialOwnerUserId(),
        req.getSolutionOwnerUserId(),
        req.getPmUserId(),
        req.getOpsOwnerUserId());
    validateProduct(req.getProductId());
    Opportunity o = new Opportunity();
    applyCustomer(o, req.getCustomerId(), req.getCustomerName());
    o.setTitle(req.getTitle());
    o.setNote(req.getNote());
    o.setAmount(req.getAmount());
    o.setStage(OpportunityStage.LEAD);
    o.setStatus(OpportunityStatus.OPEN);
    o.setStageEnteredAt(Instant.now()); // v0.0.47 — 创建即进入 LEAD
    o.setCommercialOwnerUserId(req.getCommercialOwnerUserId());
    o.setSolutionOwnerUserId(req.getSolutionOwnerUserId());
    o.setPmUserId(req.getPmUserId());
    o.setOpsOwnerUserId(req.getOpsOwnerUserId());
    o.setProductId(req.getProductId());
    return enrich(repo.saveAndFlush(o));
  }

  public OpportunityDetail findById(Long id) {
    return enrich(getOrThrow(id));
  }

  public PageResponse<OpportunityDetail> list(
      String stage, String status, Long ownerUserId, PageParams page) {
    Specification<Opportunity> spec =
        (root, q, cb) -> {
          Predicate p = cb.conjunction();
          if (stage != null) {
            p = cb.and(p, cb.equal(root.get("stage"), stage));
          }
          if (status != null) {
            p = cb.and(p, cb.equal(root.get("status"), status));
          }
          if (ownerUserId != null) {
            p =
                cb.and(
                    p,
                    cb.or(
                        cb.equal(root.get("commercialOwnerUserId"), ownerUserId),
                        cb.equal(root.get("solutionOwnerUserId"), ownerUserId),
                        cb.equal(root.get("pmUserId"), ownerUserId),
                        cb.equal(root.get("opsOwnerUserId"), ownerUserId)));
          }
          return p;
        };
    PageRequest pr =
        PageRequest.of(
            page.getPage(),
            page.getSize(),
            Sort.by(Sort.Direction.DESC, "createTime").and(Sort.by(Sort.Direction.DESC, "id")));
    Page<Opportunity> result = repo.findAll(spec, pr);
    return PageResponse.of(
        result.stream().map(this::enrich).collect(Collectors.toList()),
        page.getPage(),
        page.getSize(),
        result.getTotalElements());
  }

  @Transactional
  public OpportunityDetail update(Long id, OpportunityUpdateRequest req) {
    Opportunity o = getOrThrow(id);
    validateOwners(
        req.getCommercialOwnerUserId(),
        req.getSolutionOwnerUserId(),
        req.getPmUserId(),
        req.getOpsOwnerUserId());
    validateProduct(req.getProductId());
    applyCustomer(o, req.getCustomerId(), req.getCustomerName());
    o.setTitle(req.getTitle());
    o.setNote(req.getNote());
    o.setAmount(req.getAmount());
    o.setCommercialOwnerUserId(req.getCommercialOwnerUserId());
    o.setSolutionOwnerUserId(req.getSolutionOwnerUserId());
    o.setPmUserId(req.getPmUserId());
    o.setOpsOwnerUserId(req.getOpsOwnerUserId());
    o.setProductId(req.getProductId());
    return enrich(repo.saveAndFlush(o));
  }

  @Transactional
  public void delete(Long id) {
    repo.delete(getOrThrow(id));
  }

  /**
   * Advance one stage along the full 售前→实施 journey. Gate stages (商机/投标/合同/立项) require a
   * PASS/REJECT decision; a 售前 gate REJECT loses the deal (LOST), a 立项 REJECT just holds at 立项;
   * 合同签订 PASS wins the deal (WON) and enters 实施; 验收 is terminal. A LOST deal cannot advance.
   */
  @Transactional
  public OpportunityDetail advance(
      Long id, String decision, String note, String decidedBy, ArtifactInput artifact) {
    Opportunity o = getOrThrow(id);
    if (OpportunityStatus.LOST.equals(o.getStatus())) {
      throw new ConflictException("opportunity is LOST");
    }
    String stage = o.getStage();
    int idx = OpportunityStage.STAGE_ORDER.indexOf(stage);
    if (idx >= OpportunityStage.STAGE_ORDER.size() - 1) {
      throw new ConflictException("opportunity already at the final stage (验收)");
    }
    if (OpportunityStage.GATE_STAGES.contains(stage)) {
      if (decision == null || !GateDecision.ALL.contains(decision)) {
        throw new BadRequestException("decision required at gate (PASS|REJECT): stage=" + stage);
      }
      o.setGateDecidedBy(decidedBy);
    }
    // v0.0.45 产出物门禁：若该来源阶段要求产出物，校验 + 同事务创建（关口通过/否决都留痕）。
    persistRequiredArtifact(o, stage, decision, artifact);
    // 关口否决：售前 → 丢单，立项 → 停留可重审。
    if (OpportunityStage.GATE_STAGES.contains(stage) && GateDecision.REJECT.equals(decision)) {
      if (OpportunityStage.PRESALES_GATES.contains(stage)) {
        o.setStatus(OpportunityStatus.LOST);
      }
      return enrich(repo.saveAndFlush(o));
    }
    // PASS (or a non-gate stage): advance to the next node.
    o.setStage(OpportunityStage.STAGE_ORDER.get(idx + 1));
    o.setStageEnteredAt(Instant.now()); // v0.0.47 — 阶段变更刷新「进入阶段时间」(停留计时归零)
    if (OpportunityStage.CONTRACT.equals(stage)) {
      o.setStatus(OpportunityStatus.WON); // 合同签订 PASS → 赢单，进入实施
    }
    return enrich(repo.saveAndFlush(o));
  }

  /**
   * Enforce the 流转产出物 required to advance FROM {@code stage} (no-op if none). Single-required-type
   * stages (线索/商机) accept the artifact inline in the advance form and create it here; multi-type
   * stages (推介/POC) require the artifacts to already exist (submitted independently via
   * {@code POST /artifacts}). Either way, ALL required types must be present or it throws 400.
   */
  private void persistRequiredArtifact(
      Opportunity o, String stage, String decision, ArtifactInput artifact) {
    List<String> required = TransitionArtifactRules.requiredFor(stage);
    if (required.isEmpty()) {
      return;
    }
    // v0.0.46 门禁仅前进强制：关口否决（丢单）不要求前进交付物；唯商机决策《纪要》通过/否决都留痕。
    if (OpportunityStage.GATE_STAGES.contains(stage)
        && GateDecision.REJECT.equals(decision)
        && !TransitionArtifactRules.requiredOnReject(stage)) {
      return;
    }
    Set<String> have =
        artifactRepo.findByOpportunityIdOrderByIdDesc(o.getId()).stream()
            .map(OpportunityArtifact::getType)
            .collect(Collectors.toCollection(HashSet::new));
    // 单一产出物的转换（线索/商机）：advance 表单内联提交即建。仅报告类内联，链接类单一规则（投标文件）走预提交。
    if (required.size() == 1
        && !ArtifactType.isLink(required.get(0))
        && artifact != null
        && !isBlank(artifact.getTitle())
        && !isBlank(artifact.getContent())) {
      String type = required.get(0);
      OpportunityArtifact art = new OpportunityArtifact();
      art.setOpportunityId(o.getId());
      art.setType(type);
      art.setStageFrom(stage);
      art.setTitle(artifact.getTitle().trim());
      art.setContent(artifact.getContent());
      art.setDecision(decision);
      artifactRepo.save(art);
      have.add(type);
    }
    List<String> missing = new ArrayList<>();
    for (String t : required) {
      if (!have.contains(t)) {
        missing.add("《" + ArtifactType.label(t) + "》");
      }
    }
    if (!missing.isEmpty()) {
      throw new BadRequestException("此转换需先提交产出物：" + String.join("、", missing));
    }
  }

  private static boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }

  /**
   * 立项评审 gate: a WON opportunity at 立项 (INITIATION) is initiated into a delivery Project on PASS.
   * v0.0.48 — PASS links an existing 对外-交付 project OR inline-creates one (atomic). v0.0.52 — PASS
   * 同时**推进到下一阶段「现场调研」(SURVEY)** 并刷新 stageEnteredAt：立项移交即完成立项、进入实施，状态可见前进。
   * REJECT 仅记录决策、停留在立项（不链项目、不推进）。
   */
  @Transactional
  public OpportunityDetail initiate(Long id, OpportunityInitiateRequest req, String decidedBy) {
    Opportunity o = getOrThrow(id);
    if (!OpportunityStatus.WON.equals(o.getStatus())) {
      throw new ConflictException("opportunity must be WON to initiate (立项): status=" + o.getStatus());
    }
    if (!OpportunityStage.INITIATION.equals(o.getStage())) {
      throw new ConflictException("opportunity must be at 立项 to initiate: stage=" + o.getStage());
    }
    String decision = req.getDecision();
    if (decision == null || !GateDecision.ALL.contains(decision)) {
      throw new BadRequestException("decision required (PASS|REJECT)");
    }
    o.setGateDecidedBy(decidedBy);
    if (GateDecision.PASS.equals(decision)) {
      o.setProjectId(resolveDeliveryProject(o, req)); // 立项 → 链入实施 Project
      int idx = OpportunityStage.STAGE_ORDER.indexOf(OpportunityStage.INITIATION);
      o.setStage(OpportunityStage.STAGE_ORDER.get(idx + 1)); // 立项通过 → 进入现场调研
      o.setStageEnteredAt(Instant.now());
    }
    return enrich(repo.saveAndFlush(o));
  }

  /**
   * 立项 PASS 的交付项目解析：传 projectId → 校验存在且为 EXTERNAL_DELIVERY 后关联；否则按 name 内联新建一个
   * EXTERNAL_DELIVERY 项目（复用 {@link ProjectService#create}，编号自动生成，同事务）。二者缺一/兼有 → 400。
   */
  private Long resolveDeliveryProject(Opportunity o, OpportunityInitiateRequest req) {
    boolean hasCreate = !isBlank(req.getProjectName());
    if (req.getProjectId() != null) {
      if (hasCreate) {
        throw new BadRequestException("提供 projectId 或新建项目信息，二者只能其一");
      }
      Project p =
          projectRepo
              .findById(req.getProjectId())
              .orElseThrow(() -> new BadRequestException("project not found: id=" + req.getProjectId()));
      if (!ProjectType.EXTERNAL_DELIVERY.equals(p.getProjectType())) {
        throw new BadRequestException("立项只能关联「对外-交付」项目：projectId=" + req.getProjectId());
      }
      return req.getProjectId();
    }
    if (isBlank(req.getProjectName())) {
      throw new BadRequestException("立项需提供 projectId，或新建交付项目的名称");
    }
    Long ownerUserId = req.getProjectOwnerUserId() != null ? req.getProjectOwnerUserId() : o.getPmUserId();
    if (ownerUserId == null) {
      throw new BadRequestException("新建交付项目需指定负责人（projectOwnerUserId），或商机先设项目经理");
    }
    ProjectCreateRequest pcr = new ProjectCreateRequest();
    pcr.setName(req.getProjectName().trim());
    pcr.setOwnerUserId(ownerUserId);
    pcr.setProjectType(ProjectType.EXTERNAL_DELIVERY); // code 自动生成
    return projectService.create(pcr).getId();
  }

  private void validateOwners(Long... ownerIds) {
    for (Long ownerId : ownerIds) {
      if (ownerId != null && !userRepo.existsById(ownerId)) {
        throw new BadRequestException("owner user not found: id=" + ownerId);
      }
    }
  }

  /** v0.0.45 — the optional 产品标签 must reference an existing Product when set. */
  private void validateProduct(Long productId) {
    if (productId != null && !productRepo.existsById(productId)) {
      throw new BadRequestException("product not found: id=" + productId);
    }
  }

  /**
   * v0.0.45 — resolve the 客户: a given {@code customerId} links that customer (and adopts its name);
   * otherwise the typed {@code customerName} reuses a same-named customer or creates a new one. Always
   * keeps {@code customerName} as the display label.
   */
  private void applyCustomer(Opportunity o, Long customerId, String customerName) {
    if (customerId != null) {
      Customer c =
          customerRepo
              .findById(customerId)
              .orElseThrow(() -> new BadRequestException("customer not found: id=" + customerId));
      o.setCustomerId(c.getId());
      o.setCustomerName(c.getName());
      return;
    }
    String name = customerName == null ? "" : customerName.trim();
    if (name.isEmpty()) {
      o.setCustomerName(customerName);
      return;
    }
    Customer c =
        customerRepo
            .findFirstByNameIgnoreCase(name)
            .orElseGet(
                () -> {
                  Customer nc = new Customer();
                  nc.setName(name);
                  return customerRepo.save(nc);
                });
    o.setCustomerId(c.getId());
    o.setCustomerName(c.getName());
  }

  private Opportunity getOrThrow(Long id) {
    return repo
        .findById(id)
        .orElseThrow(() -> new NotFoundException("opportunity not found: id=" + id));
  }

  private OpportunityDetail enrich(Opportunity o) {
    OpportunityDetail d = OpportunityDetail.from(o);
    Set<Long> ids = new HashSet<>();
    addId(ids, o.getCommercialOwnerUserId());
    addId(ids, o.getSolutionOwnerUserId());
    addId(ids, o.getPmUserId());
    addId(ids, o.getOpsOwnerUserId());
    if (!ids.isEmpty()) {
      Map<Long, String> names = new HashMap<>();
      userRepo.findAllById(ids).forEach(u -> names.put(u.getId(), nameOf(u)));
      d.setCommercialOwnerName(names.get(o.getCommercialOwnerUserId()));
      d.setSolutionOwnerName(names.get(o.getSolutionOwnerUserId()));
      d.setPmName(names.get(o.getPmUserId()));
      d.setOpsOwnerName(names.get(o.getOpsOwnerUserId()));
    }
    if (o.getProductId() != null) {
      productRepo.findById(o.getProductId()).ifPresent(p -> d.setProductName(p.getName()));
    }
    return d;
  }

  private static void addId(Set<Long> ids, Long id) {
    if (id != null) {
      ids.add(id);
    }
  }

  private static String nameOf(User u) {
    return u == null ? null : u.getName();
  }
}
