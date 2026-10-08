package com.derekgelvez.calendar.model;

import com.derekgelvez.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * A temporary invite to view or edit a calendar. The owner copies and shares
 * the generated link through the channel of their choice.
 */
@Entity
@Table(name = "calendar_invites")
@Getter
@Setter
@NoArgsConstructor
public class CalendarInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "calendar_id", nullable = false)
    private Calendar calendar;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inviter_id", nullable = false)
    private User inviter;

    /** Unique token used in the invite link. */
    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "access_level", nullable = false, length = 10)
    private AccessLevel accessLevel;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private InviteStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
