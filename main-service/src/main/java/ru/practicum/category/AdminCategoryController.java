package ru.practicum.category;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
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
    public ResponseEntity<CategoryDto> postCategory(@Valid @RequestBody NewCategoryRequest newCategoryRequest) throws BadRequestException {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.postCategory(newCategoryRequest));
        } catch (RuntimeException ex) {
            throw new BadRequestException();
        }
    }

    @DeleteMapping("/{catId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable("catId") Long catId) throws BadRequestException {
        try {
            categoryService.deleteCategory(catId);
        } catch (RuntimeException ex) {
            throw new BadRequestException();
        }

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{catId}")
    public ResponseEntity<CategoryDto> patchCategory(@PathVariable("catId") Long catId,
                                                     @RequestBody UpdateCategoryDto updateCategoryDto) throws BadRequestException {
        try {
            return ResponseEntity.ok().body(categoryService.patchCategory(catId, updateCategoryDto));
        } catch (RuntimeException ex) {
            throw new BadRequestException();
        }
    }
}

