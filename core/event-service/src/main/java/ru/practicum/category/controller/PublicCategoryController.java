package ru.practicum.category.controller;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.category.service.CategoryService;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.handler.exception.BadRequestException;
import ru.practicum.handler.exception.NotFoundException;

import java.util.List;

@Validated
@RestController
@AllArgsConstructor
@RequestMapping(path = "/categories")
public class PublicCategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryDto>> getCategories(
            @RequestParam(name = "from", defaultValue = "0") Integer from,
            @RequestParam(name = "size", defaultValue = "10") Integer size) {

        if (from < 0 || size <= 0) {
            throw new BadRequestException("Invalid pagination parameters");
        }

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id").descending());

        return ResponseEntity.ok()
                .body(categoryService.getCategories(pageable).getContent());
    }

    @GetMapping("/{catId}")
    public ResponseEntity<CategoryDto> getCategory(@PathVariable("catId") Long catId) {
        CategoryDto category = categoryService.getCategory(catId);

        if (category == null) {
            throw new NotFoundException("Category with id=" + catId + " was not found");
        }

        return ResponseEntity.ok().body(category);
    }
}