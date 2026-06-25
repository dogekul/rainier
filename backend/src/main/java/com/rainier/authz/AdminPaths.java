/* (C) 2026 Rainier — internal use only. */
package com.rainier.authz;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Classifies which {@code /api/*} endpoints require an elevated (admin) caller.
 *
 * <ul>
 *   <li><b>Tier A</b> — all methods require admin (no all-users page reads these).
 *   <li><b>Tier B</b> — writes require admin, GET stays open: all-users pages read these (the
 *       {@code users} owner/assignee dropdowns, {@code features}/{@code product-modules} in the sprint
 *       feature panel).
 * </ul>
 *
 * Everything else (projects/sprints/tasks/demands/requirements/stories/milestones/sprint-features/auth)
 * is all-users and never gated here.
 */
public final class AdminPaths {

  private AdminPaths() {}

  static final List<String> TIER_A =
      Collections.unmodifiableList(
          Arrays.asList(
              "/api/organizations",
              "/api/user-organizations",
              "/api/positions",
              "/api/roles",
              "/api/user-roles",
              "/api/audit-logs",
              // v0.0.41 read; v0.0.80 B7 also covers POST /api/compliance/users/{id}/revoke-roles
              // and POST /api/compliance/disable-user/{id} — both ride this Tier A prefix.
              "/api/compliance",
              "/api/products",
              // v0.0.76 B3: admin-only password reset (/api/admin/users/{id}/reset-password) +
              // future /api/admin/** endpoints — Tier A means even GETs require elevation.
              "/api/admin",
              // v0.0.92 D4: admin-only sent email log read (GET /api/emails) — Tier A so even GET
              // requires elevation; the forward endpoint /api/me/notifications/{id}/email is NOT
              // gated here (it's under /api/me/* and uses owner check).
              "/api/emails"));

  static final List<String> TIER_B =
      Collections.unmodifiableList(
          Arrays.asList(
              "/api/users",
              "/api/features",
              "/api/product-modules",
              "/api/ai/errors"));

  /** Exact base or a sub-path ({@code base/...}) — never a sibling like products vs product-modules. */
  static boolean matches(String uri, String base) {
    return uri.equals(base) || uri.startsWith(base + "/");
  }

  static boolean isMutating(String method) {
    return "POST".equalsIgnoreCase(method)
        || "PUT".equalsIgnoreCase(method)
        || "PATCH".equalsIgnoreCase(method)
        || "DELETE".equalsIgnoreCase(method);
  }

  /** Whether (uri, method) requires an elevated caller. Tier A: always; Tier B: only mutating. */
  public static boolean requiresAdmin(String uri, String method) {
    if (uri == null) {
      return false;
    }
    for (String base : TIER_A) {
      if (matches(uri, base)) {
        return true;
      }
    }
    if (isMutating(method)) {
      for (String base : TIER_B) {
        if (matches(uri, base)) {
          return true;
        }
      }
    }
    return false;
  }
}
