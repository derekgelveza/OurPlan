package com.derekgelvez.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * An OurPlan account, and also the principal Spring Security works with (it implements
 * {@link UserDetails}). The calendar module references it by id for ownership, sharing and
 * access checks.
 *
 * <p>Verification and password-reset state live on the same row: the pending 6-digit code with
 * its expiry, and a SHA-256 hash (never the raw value) of the current reset token with its expiry.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    /** Unique; always stored in lower case so lookups by email are consistent. */
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    /** False until the email is verified; login is refused while it is false. */
    private boolean enabled;

    @Column(name = "verification_code")
    private String verificationCode;

    @Column(name = "verification_expiration")
    private LocalDateTime verificationCodeExpiresAt;

    @Column(name = "password_reset_token_hash")
    private String passwordResetTokenHash;

    @Column(name = "password_reset_expiration")
    private LocalDateTime passwordResetExpiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public User(String username, String email, String password) {
        this.username = username;
        setEmail(email);
        this.password = password;
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Spring Security's login identifier. OurPlan signs in by email, and the JWT subject and
     * the UserDetailsService lookup both use it, so this returns the email, not the handle.
     */
    @Override
    public String getUsername() {
        return email;
    }

    /** The public handle chosen at sign-up (the {@code username} column). */
    public String getHandle() {
        return username;
    }

    /** No roles yet: every signed-in user has the same permissions. */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities(){
        return List.of();
    }

    // Account expiry, locking and credential expiry aren't modelled; repeated failed logins are
    // throttled by LoginAttemptService instead.
    @Override
    public boolean isAccountNonExpired(){
        return true;
    }

    @Override
    public boolean isAccountNonLocked(){
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired(){
        return true;
    }

    @Override
    public boolean isEnabled(){
        return enabled;
    }
}
