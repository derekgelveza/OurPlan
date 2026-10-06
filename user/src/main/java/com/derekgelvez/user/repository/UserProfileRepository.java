package com.derekgelvez.user.repository;

import com.derekgelvez.user.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    /** Finds a user's profile. */
    Optional<UserProfile> findByUserId(Long userId);
}
