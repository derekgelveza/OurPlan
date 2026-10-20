package com.derekgelvez.user.repository;

import com.derekgelvez.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Account lookups. Emails are stored lower-case, so callers normalize an email before findByEmail. */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByVerificationCode(String verificationCode);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

}
