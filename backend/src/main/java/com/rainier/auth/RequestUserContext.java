/* (C) 2026 Rainier — internal use only. */
package com.rainier.auth;

/**
 * v0.0.79 (B6) — per-thread current loginName, set by {@code SecurityFilter} after a successful
 * Bearer token parse and cleared at the end of the filter chain.
 *
 * <p>Pure static / ThreadLocal helper so callers outside the servlet stack (audit aspect,
 * background services that propagate context, transaction-template callbacks) can read the real
 * actor without depending on Spring's {@code RequestContextHolder}.
 *
 * <p>Callers MUST clear in a {@code finally} block to avoid leaking identity into pooled threads.
 */
public final class RequestUserContext {

  private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

  private RequestUserContext() {}

  /** Returns the loginName set for this thread, or null when unset. */
  public static String get() {
    return CURRENT.get();
  }

  /** Sets the loginName for this thread. Null clears. */
  public static void set(String loginName) {
    if (loginName == null || loginName.isEmpty()) {
      CURRENT.remove();
    } else {
      CURRENT.set(loginName);
    }
  }

  /** Clears the loginName for this thread; safe to call when unset. */
  public static void clear() {
    CURRENT.remove();
  }
}
