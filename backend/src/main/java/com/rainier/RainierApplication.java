/* (C) 2026 Rainier — internal use only. */
package com.rainier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Entry point for the Rainier PM backend. */
@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableScheduling
public class RainierApplication {

  public static void main(String[] args) {
    SpringApplication.run(RainierApplication.class, args);
  }
}
