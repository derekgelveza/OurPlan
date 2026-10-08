package com.derekgelvez.user.repository;

import com.derekgelvez.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // findById(userId) is inherited from JpaRepository.

    /** Finds an account by its normalized email for authentication. */
    Optional<User> findByEmail(String email);

}
