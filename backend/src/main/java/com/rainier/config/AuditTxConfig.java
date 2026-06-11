/* (C) 2026 Rainier — internal use only. */
package com.rainier.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * v0.0.15: raises the transaction advisor to the OUTERMOST advice so the {@code AuditAspect}
 * (default order, inner) runs while the business transaction is still open. The audit row then
 * joins that transaction and rolls back together with the business write.
 *
 * <p>Spring Boot's auto-configured transaction management defaults to {@code LOWEST_PRECEDENCE}
 * (innermost), which would push the aspect outside the transaction. Declaring
 * {@code @EnableTransactionManagement(order = HIGHEST_PRECEDENCE)} here makes the proxy wrap the
 * aspect instead. There are no other custom aspects, so this has no other ordering effect.
 */
@Configuration
@EnableTransactionManagement(order = Ordered.HIGHEST_PRECEDENCE)
public class AuditTxConfig {}
