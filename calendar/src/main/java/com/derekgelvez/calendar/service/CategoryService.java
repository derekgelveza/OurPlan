package com.derekgelvez.calendar.service;

import com.derekgelvez.calendar.dto.CategoryDTO;
import com.derekgelvez.calendar.dto.CategoryResponseDTO;
import com.derekgelvez.calendar.exception.CalendarAccessDeniedException;
import com.derekgelvez.calendar.exception.CannotDeleteDefaultCategoryException;
import com.derekgelvez.calendar.exception.CannotModifyDefaultCategoryException;
import com.derekgelvez.calendar.exception.CategoryAlreadyExistsException;
import com.derekgelvez.calendar.exception.ColorInUseException;
import com.derekgelvez.calendar.exception.ResourceNotFoundException;
import com.derekgelvez.calendar.model.Category;
import com.derekgelvez.calendar.repository.CategoryRepository;
import com.derekgelvez.calendar.repository.EventExceptionRepository;
import com.derekgelvez.calendar.repository.EventRepository;
import com.derekgelvez.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Category CRUD. Categories belong to the user who created them; names and colours are
 * unique per owner, except that any number of categories may use the default grey.
 * The service never asks the user anything: on a colour conflict it throws
 * ColorInUseException and the app resends with reassignColor=true if the user agrees.
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final EventExceptionRepository eventExceptionRepository;
    private final UserLookup userLookup;

    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> getCategories(Long userId) {
        return categoryRepository.findByOwnerId(userId).stream()
                .sorted(Comparator.comparing(Category::isDefault).reversed().thenComparing(Category::getName))
                .map(CategoryResponseDTO::from)
                .toList();
    }

    @Transactional
    public CategoryResponseDTO createCategory(Long userId, CategoryDTO dto, boolean reassignColor) {
        User owner = userLookup.getUser(userId);
        getOrCreateDefaultCategory(owner);
        String name = dto.name().trim();
        if (categoryRepository.existsByOwnerIdAndNameIgnoreCase(userId, name)) {
            throw new CategoryAlreadyExistsException("A category named \"" + name + "\" already exists");
        }
        String color = Category.normalizeColor(dto.colour());
        releaseColor(userId, color, null, reassignColor);
        Category category = categoryRepository.save(new Category(owner, name, color));
        return CategoryResponseDTO.from(category);
    }

    @Transactional
    public CategoryResponseDTO updateCategory(Long userId, Long categoryId, CategoryDTO dto, boolean reassignColor) {
        Category category = getOwnedCategory(userId, categoryId);
        String name = dto.name().trim();
        String color = Category.normalizeColor(dto.colour());
        if (category.isDefault()) {
            if (!category.getName().equals(name) || !category.getColor().equals(color)) {
                throw new CannotModifyDefaultCategoryException("The \"Uncategorized\" category cannot be renamed or recoloured");
            }
            return CategoryResponseDTO.from(category);
        }
        if (!category.getName().equalsIgnoreCase(name)
                && categoryRepository.existsByOwnerIdAndNameIgnoreCase(userId, name)) {
            throw new CategoryAlreadyExistsException("A category named \"" + name + "\" already exists");
        }
        if (!category.getColor().equals(color)) {
            releaseColor(userId, color, categoryId, reassignColor);
        }
        category.setName(name);
        category.setColor(color);
        return CategoryResponseDTO.from(categoryRepository.saveAndFlush(category));
    }

    /** Deletes the category; its events (and single-occurrence overrides) move to "Uncategorized". */
    @Transactional
    public void deleteCategory(Long userId, Long categoryId) {
        Category category = getOwnedCategory(userId, categoryId);
        if (category.isDefault()) {
            throw new CannotDeleteDefaultCategoryException("The \"Uncategorized\" category cannot be deleted");
        }
        Category uncategorized = getOrCreateDefaultCategory(category.getOwner());
        eventRepository.reassignCategory(categoryId, uncategorized.getId());
        eventExceptionRepository.reassignCategory(categoryId, uncategorized.getId());
        categoryRepository.deleteById(categoryId);
    }

    /** Every user gets one "Uncategorized" category in the default grey. */
    @Transactional
    public Category getOrCreateDefaultCategory(User owner) {
        return categoryRepository.findByOwnerIdAndIsDefaultTrue(owner.getId())
                .orElseGet(() -> categoryRepository.save(Category.uncategorized(owner)));
    }

    /**
     * If another of the user's categories uses the colour: throw ColorInUseException, or, when
     * the user agreed (reassignColor), move that category to the default grey first. Runs in
     * the caller's transaction so the swap is atomic.
     */
    private void releaseColor(Long userId, String color, Long categoryIdBeingUpdated, boolean reassignColor) {
        if (Category.DEFAULT_COLOR.equals(color)) {
            return; // the default grey may be shared
        }
        categoryRepository.findByOwnerIdAndColor(userId, color)
                .filter(conflict -> !conflict.getId().equals(categoryIdBeingUpdated))
                .ifPresent(conflict -> {
                    if (!reassignColor) {
                        throw new ColorInUseException(conflict.getId());
                    }
                    conflict.setColor(Category.DEFAULT_COLOR);
                    categoryRepository.saveAndFlush(conflict);
                });
    }

    private Category getOwnedCategory(Long userId, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category " + categoryId + " not found"));
        if (!category.getOwner().getId().equals(userId)) {
            throw new CalendarAccessDeniedException("Only the owner can change this category");
        }
        return category;
    }
}
