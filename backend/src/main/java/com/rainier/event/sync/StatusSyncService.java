/* (C) 2026 Rainier — internal use only. */
package com.rainier.event.sync;

import com.rainier.aiworklog.domain.AiWorkLog;
import com.rainier.aiworklog.domain.AiWorkLogStatus;
import com.rainier.aiworklog.repository.AiWorkLogRepository;
import com.rainier.event.domain.Event;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * v0.0.67 — bridges the v0.0.65/0.0.66 event pipeline to the v0.0.43 AI work log flywheel.
 *
 * <p>When an {@link Event} has been processed and its extractor wrote back a TASK ref AND the event
 * is a {@code PR_MERGE}, this service writes a {@link AiWorkLog} PROPOSED suggesting the task be
 * marked DONE. The actual {@code Task.status} mutation is NOT performed here — a human must accept
 * the proposal via {@code POST /api/ai-work-logs/{id}/decision} (existing v0.0.43 endpoint, not
 * touched by this sub-change). All AI-driven state changes therefore remain auditable + reversible.
 *
 * <p>Out of scope (A3): non-PR_MERGE events, non-TASK extractions, real Task entity mutation on
 * accept.
 */
@Service
@Transactional
public class StatusSyncService {

  static final String AGENT_TYPE = "STATUS_SYNC";
  static final String ACTION = "UPDATE_TASK_STATUS";
  static final String TARGET_TYPE_TASK = "TASK";
  static final String EVENT_KIND_PR_MERGE = "PR_MERGE";

  private final AiWorkLogRepository aiWorkLogRepo;

  public StatusSyncService(AiWorkLogRepository aiWorkLogRepo) {
    this.aiWorkLogRepo = aiWorkLogRepo;
  }

  /**
   * Inspect an event whose extraction has been written back; create exactly one PROPOSED AiWorkLog
   * when the event is a PR_MERGE pointing at a TASK. No-op otherwise.
   */
  public void applyExtraction(Event event) {
    if (event == null) {
      return;
    }
    if (!Boolean.TRUE.equals(event.getProcessed())) {
      return;
    }
    if (!EVENT_KIND_PR_MERGE.equals(event.getEventKind())) {
      return;
    }
    if (!TARGET_TYPE_TASK.equals(event.getExtractedEntityType())) {
      return;
    }
    if (event.getExtractedEntityId() == null) {
      return;
    }

    AiWorkLog log = new AiWorkLog();
    log.setAgentType(AGENT_TYPE);
    log.setAction(ACTION);
    log.setTargetType(TARGET_TYPE_TASK);
    log.setTargetId(event.getExtractedEntityId());
    log.setSummary(
        "AI 提议：基于 "
            + safe(event.getSourceType())
            + " "
            + EVENT_KIND_PR_MERGE
            + " 将任务 #"
            + event.getExtractedEntityId()
            + " 标记为 DONE");
    log.setEvidence(buildEvidence(event));
    log.setStatus(AiWorkLogStatus.PROPOSED);
    aiWorkLogRepo.saveAndFlush(log);
  }

  private String buildEvidence(Event event) {
    StringBuilder sb = new StringBuilder(128);
    sb.append("{")
        .append("\"eventId\":")
        .append(event.getId() == null ? "null" : event.getId().toString())
        .append(",\"sourceType\":\"")
        .append(escape(safe(event.getSourceType())))
        .append("\",\"sourceId\":\"")
        .append(escape(safe(event.getSourceId())))
        .append("\",\"eventKind\":\"")
        .append(escape(safe(event.getEventKind())))
        .append("\"}");
    return sb.toString();
  }

  private static String safe(String s) {
    return s == null ? "" : s;
  }

  private static String escape(String s) {
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
