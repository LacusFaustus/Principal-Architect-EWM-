package ru.practicum.category.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.category.service.CategoryService;
import ru.practicum.category.dto.NewCategoryRequest;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.UpdateCategoryDto;

@Validated
@RestController
@AllArgsConstructor
@RequestMapping(path = "/admin/categories")
public class AdminCategoryController {
    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CategoryDto> postCategory(
            @Valid @RequestBody NewCategoryRequest newCategoryRequest) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.postCategory(newCategoryRequest));
    }

    @DeleteMapping("/{catId}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable("catId") Long catId) {

        categoryService.deleteCategory(catId);

        return ResponseEntity.noContent()
                .build();
    }

    @PatchMapping("/{catId}")
    public ResponseEntity<CategoryDto> patchCategory(
            @PathVariable("catId") Long catId,
            @Valid @RequestBody UpdateCategoryDto updateCategoryDto) {

        return ResponseEntity.ok()
                .body(categoryService.patchCategory(catId, updateCategoryDto));
    }
}