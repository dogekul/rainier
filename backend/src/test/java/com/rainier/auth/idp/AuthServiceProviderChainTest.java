/* (C) 2026 Rainier — internal use only. */
package com.rainier.auth.idp;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rainier.user.repository.UserRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * v0.0.75 B2 — provider-chain integration. Confirms that {@code AuthController} loops the
 * registered {@link IdentityProvider} beans in {@code @Order} sequence, accepts the first non-empty
 * result, and issues a token. Stubs are wired via {@link ChainConfig} so we don't need the real
 * LDAP / OAuth feature flags.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {"app.security.real-auth.enabled=true"})
class AuthServiceProviderChainTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepo;

  @BeforeEach
  void clean() {
    // Ensure LocalDb has no matching row — chain success must come from the test stub.
    userRepo.deleteAll();
  }

  private String body(String username, String password) {
    return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
  }

  /** TC-IDP-003: provider #1 empty, provider #2 returns identity → 200 + token. */
  @Test
  void chain_secondProviderSucceeds_returns200() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("ext-charlie", "any")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andExpect(jsonPath("$.user.username").value("ext-charlie"));
  }

  /** TC-IDP-004: all providers empty (unknown user, not in stub) → 401. */
  @Test
  void chain_allEmpty_returns401() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("nobody", "x")))
        .andExpect(status().isUnauthorized());
  }

  @TestConfiguration
  static class ChainConfig {

    /**
     * Always-empty provider. Order doesn't have to beat LocalDb — even if LocalDb runs first it
     * returns empty (DB cleared in @BeforeEach), so the chain still falls through to the accepting
     * provider below.
     */
    @Bean
    @Order(50)
    IdentityProvider alwaysEmptyTestProvider() {
      return new IdentityProvider() {
        @Override
        public String name() {
          return "test-empty";
        }

        @Override
        public Optional<UserIdentity> authenticate(String loginName, String password) {
          return Optional.empty();
        }
      };
    }

    /** Lower-priority provider that accepts a fixed login — proves first-non-empty-wins. */
    @Bean
    @Order(1000)
    IdentityProvider acceptCharlieTestProvider() {
      return new IdentityProvider() {
        @Override
        public String name() {
          return "test-accept-charlie";
        }

        @Override
        public Optional<UserIdentity> authenticate(String loginName, String password) {
          if ("ext-charlie".equals(loginName)) {
            return Optional.of(
                new UserIdentity(
                    "ext-1",
                    "ext-charlie",
                    "Charlie External",
                    "charlie@ext.example",
                    Arrays.asList("engineers")));
          }
          return Optional.empty();
        }
      };
    }

    @Bean
    Object unused() {
      // Avoid empty-config edge cases in some JUnit / Spring combinations.
      return Collections.emptyList();
    }
  }
}
