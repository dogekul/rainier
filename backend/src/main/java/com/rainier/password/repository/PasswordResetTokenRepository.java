/* (C) 2026 Rainier — internal use only. */
package com.rainier.password.repository;

import com.rainier.password.domain.PasswordResetToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for {@link PasswordResetToken}. */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

  Optional<PasswordResetToken> findByToken(String token);
}
