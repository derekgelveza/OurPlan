package com.derekgelvez.user.repository;

import com.derekgelvez.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // findById(userId) is inherited from JpaRepository.

    /**
     * Finds a user by email. Used by shareCalendar() to find the invitee.
     * Emails are stored in lower case, so pass a lower-cased email.
     */
    Optional<User> findByEmail(String email);
}
