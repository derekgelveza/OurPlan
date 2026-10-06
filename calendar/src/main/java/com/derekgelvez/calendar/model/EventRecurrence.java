package com.derekgelvez.calendar.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * The repeat rule for one event. Individual occurrences are not stored; they are
 * calculated by the event service.
 */
@Entity
@Table(name = "event_recurrences")
@Getter
@Setter
@NoArgsConstructor
public class EventRecurrence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, unique = true)
    private Event event;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private RecurrenceFrequency frequency;

    /** Repeat every N days/weeks/months/years. */
    @Column(name = "repeat_interval", nullable = false)
    private int interval = 1;

    /** Used by WEEKLY and CUSTOM. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "event_recurrence_days", joinColumns = @JoinColumn(name = "recurrence_id"))
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "day_of_week", nullable = false, length = 9)
    private Set<DayOfWeek> daysOfWeek = new HashSet<>();

    /** Last date the event repeats (inclusive). */
    @Column(name = "until_date")
    private LocalDate until;
}
