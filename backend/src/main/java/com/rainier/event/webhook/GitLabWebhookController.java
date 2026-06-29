/* (C) 2026 Rainier — internal use only. */
package com.rainier.event.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rainier.event.domain.Event;
import com.rainier.event.service.EventService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * v0.0.101 F2 — real GitLab webhook ingress. Authenticates via {@code X-Gitlab-Token} (constant-time
 * comparison against {@code app.gitlab.webhook-secret}); on success records the raw payload as an
 * {@link Event} (sourceType=GITLAB) and immediately drains {@link EventService#process(int)} so the
 * GitLabAdapter → StatusSyncService → AiWorkLog PROPOSED chain (A2/A3, shipped) runs synchronously.
 *
 * <p>The path is whitelisted in {@link com.rainier.config.SecurityWhitelistPaths} — webhooks carry
 * their own header token and never a Bearer JWT. Out-of-scope: HMAC signatures (GitLab's model is
 * plain secret-token), delivery-id dedupe, retry policy.
 */
@RestController
@RequestMapping("/api/webhooks")
public class GitLabWebhookController {

  private static final String EVENT_KIND_COMMIT = "COMMIT";
  private static final String EVENT_KIND_PR_MERGE = "PR_MERGE";
  private static final String EVENT_KIND_OTHER = "OTHER";
  private static final String SOURCE_TYPE = "GITLAB";

  private final EventService eventService;
  private final ObjectMapper mapper;
  private final byte[] expectedSecretBytes;

  public GitLabWebhookController(
      EventService eventService,
      ObjectMapper mapper,
      @Value("${app.gitlab.webhook-secret:changeme}") String secret) {
    this.eventService = eventService;
    this.mapper = mapper;
    // Pre-compute bytes once; constant-time compare uses MessageDigest.isEqual on each request.
    this.expectedSecretBytes =
        secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
  }

  @PostMapping("/gitlab")
  public ResponseEntity<?> receive(
      @RequestHeader(value = "X-Gitlab-Token", required = false) String token,
      @RequestHeader(value = "X-Gitlab-Event", required = false) String gitlabEvent,
      @RequestBody(required = false) String rawBody) {

    if (!tokenMatches(token)) {
      Map<String, String> err = new HashMap<String, String>();
      err.put("message", "Invalid or missing X-Gitlab-Token");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
    }

    String payload = rawBody == null ? "" : rawBody;
    String objectKind = null;
    String afterSha = null;
    String mrAction = null;
    String mrIid = null;
    try {
      if (!payload.isEmpty()) {
        JsonNode root = mapper.readTree(payload);
        if (root != null && root.isObject()) {
          if (root.hasNonNull("object_kind")) {
            objectKind = root.get("object_kind").asText();
          }
          if (root.hasNonNull("after")) {
            afterSha = root.get("after").asText();
          }
          JsonNode attrs = root.get("object_attributes");
          if (attrs != null && attrs.isObject()) {
            if (attrs.hasNonNull("action")) {
              mrAction = attrs.get("action").asText();
            }
            if (attrs.hasNonNull("iid")) {
              mrIid = attrs.get("iid").asText();
            }
          }
        }
      }
    } catch (Exception parseEx) {
      // Tolerate malformed JSON — still record the raw event with eventKind=OTHER so the AI error
      // dashboard can surface bad payloads (v0.0.93 A4 plumbing).
      objectKind = null;
    }

    String eventKind;
    String sourceId;
    if ("merge_request".equals(objectKind)) {
      eventKind = EVENT_KIND_PR_MERGE;
      sourceId = mrIid;
    } else if ("push".equals(objectKind)) {
      eventKind = EVENT_KIND_COMMIT;
      sourceId = afterSha;
    } else {
      eventKind = EVENT_KIND_OTHER;
      sourceId = gitlabEvent;
    }

    Event saved =
        eventService.record(SOURCE_TYPE, sourceId, eventKind, payload, LocalDateTime.now());
    int processed = eventService.process(1);

    Map<String, Object> body = new HashMap<String, Object>();
    body.put("eventId", saved.getId());
    body.put("processed", processed);
    body.put("eventKind", eventKind);
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(body);
  }

  /** Constant-time token comparison; rejects null / empty / mismatched length tokens. */
  private boolean tokenMatches(String headerToken) {
    if (headerToken == null || headerToken.isEmpty() || expectedSecretBytes.length == 0) {
      return false;
    }
    byte[] provided = headerToken.getBytes(StandardCharsets.UTF_8);
    return MessageDigest.isEqual(provided, expectedSecretBytes);
  }
}
