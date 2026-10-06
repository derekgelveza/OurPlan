package com.derekgelvez.calendar.repository;

import com.derekgelvez.calendar.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** A user's categories. An event's category comes from the Event → Category relationship, not from here. */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByOwnerId(Long ownerId);

    /** The category already using a colour (colour conflict rule). Not used for the shared default grey. */
    Optional<Category> findByOwnerIdAndColor(Long ownerId, String color);

    boolean existsByOwnerIdAndNameIgnoreCase(Long ownerId, String name);

    /** The user's "Uncategorized" category. */
    Optional<Category> findByOwnerIdAndIsDefaultTrue(Long ownerId);
}
