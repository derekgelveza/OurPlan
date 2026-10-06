package com.derekgelvez.calendar.model;

import com.derekgelvez.user.model.User;
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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Locale;

/**
 * A user's event category, e.g. "School" in red. Names and colours are unique per owner;
 * the default grey may be shared by any number of categories.
 * <p>
 * {@code nameKey} and {@code colorKey} exist only so the database can enforce those
 * invariants with plain unique constraints (case-insensitive names, and colours unique
 * except for the default grey, whose key is null).
 */
@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
public class Category {

    public static final String DEFAULT_COLOR = "#9E9E9E";
    public static final String DEFAULT_NAME = "Uncategorized";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 50)
    private String name;

    @Setter(lombok.AccessLevel.NONE)
    @Column(name = "name_key", nullable = false, length = 50)
    private String nameKey;

    @Column(nullable = false, length = 7)
    private String color;

    @Setter(lombok.AccessLevel.NONE)
    @Column(name = "color_key", length = 7)
    private String colorKey;

    /** True only for the "Uncategorized" category, which cannot be deleted or renamed. */
    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Category(User owner, String name, String color) {
        this.owner = owner;
        setName(name);
        setColor(color);
    }

    public static Category uncategorized(User owner) {
        Category category = new Category(owner, DEFAULT_NAME, DEFAULT_COLOR);
        category.setDefault(true);
        return category;
    }

    public void setName(String name) {
        this.name = name.trim();
        this.nameKey = this.name.toLowerCase(Locale.ROOT);
    }

    public void setColor(String color) {
        this.color = normalizeColor(color);
        this.colorKey = DEFAULT_COLOR.equals(this.color) ? null : this.color;
    }

    public static String normalizeColor(String color) {
        return color.trim().toUpperCase(Locale.ROOT);
    }
}
