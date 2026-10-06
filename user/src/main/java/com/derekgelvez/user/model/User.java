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

import java.time.Instant;
import java.util.Locale;

/**
 * An OurPlan account. Looked up by id, or by email when a calendar is shared
 * (see {@code UserRepository.findByEmail}).
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique; always stored in lower case so lookups by email are consistent. */
    @Column(nullable = false, unique = true)
    private String email;

    /** Optional; unique when present. Calendar invites may be sent by text. */
    @Column(name = "phone_number", unique = true, length = 32)
    private String phoneNumber;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public User(String email) {
        setEmail(email);
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
