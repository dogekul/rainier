/* (C) 2026 Rainier — internal use only. */
package com.rainier.authz;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * v0.0.77 B4 — 方法级细粒度权限注解。
 *
 * <p>由 {@link PermissionInterceptor} 在 {@code AdminAuthorizationInterceptor} 之后扫描：
 *
 * <ul>
 *   <li>方法未标注 → 放行（按 AdminPaths 兜底逻辑生效）
 *   <li>已标注但请求无 token → 401
 *   <li>已标注但 token 用户不满足 {@link #level()} 条件 → 403
 * </ul>
 *
 * {@link Level#ANY} (缺省) — value 中任一权限点命中即通过；{@link Level#ALL} — 全部命中才通过。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {

  PermissionPoint[] value();

  Level level() default Level.ANY;

  enum Level {
    ANY,
    ALL
  }
}
