package com.derekgelvez.calendar.controller;

import com.derekgelvez.calendar.config.CurrentUserProvider;
import com.derekgelvez.calendar.dto.CategoryDTO;
import com.derekgelvez.calendar.dto.CategoryResponseDTO;
import com.derekgelvez.calendar.dto.DeleteCategoryDTO;
import com.derekgelvez.calendar.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Exposes category CRUD endpoints and delegates to CategoryService. On COLOR_IN_USE the app
 * asks the user and, if they agree, resends the request with reassignColor=true.
 */
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public List<CategoryResponseDTO> getCategories() {
        return categoryService.getCategories(currentUserProvider.getCurrentUserId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponseDTO createCategory(@Valid @RequestBody CategoryDTO dto,
                                              @RequestParam(defaultValue = "false") boolean reassignColor) {
        return categoryService.createCategory(currentUserProvider.getCurrentUserId(), dto, reassignColor);
    }

    @PutMapping("/{categoryId}")
    public CategoryResponseDTO updateCategory(@PathVariable Long categoryId, @Valid @RequestBody CategoryDTO dto,
                                              @RequestParam(defaultValue = "false") boolean reassignColor) {
        return categoryService.updateCategory(currentUserProvider.getCurrentUserId(), categoryId, dto, reassignColor);
    }

    /** Requires {"confirmed": true}. Events in the category move to "Uncategorized". */
    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long categoryId, @Valid @RequestBody DeleteCategoryDTO dto) {
        categoryService.deleteCategory(currentUserProvider.getCurrentUserId(), categoryId);
    }
}
