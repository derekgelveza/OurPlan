package com.derekgelvez.calendar.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A change to a single occurrence of a repeating event without changing the rest of the
 * series. Null override fields mean "use the series' value".
 */
@Entity
@Table(name = "event_exceptions")
@Getter
@Setter
@NoArgsConstructor
public class EventException {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    /** The original date of the occurrence being changed. */
    @Column(name = "occurrence_date", nullable = false)
    private LocalDate occurrenceDate;

    /** True when that occurrence was deleted. */
    @Column(nullable = false)
    private boolean cancelled;

    @Column(length = 100)
    private String name;

    @Column(name = "start_at")
    private LocalDateTime start;

    @Column(name = "end_at")
    private LocalDateTime end;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    public EventException(Event event, LocalDate occurrenceDate) {
        this.event = event;
        this.occurrenceDate = occurrenceDate;
    }
}
