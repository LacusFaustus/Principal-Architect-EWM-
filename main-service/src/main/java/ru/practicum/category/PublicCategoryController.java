package ru.practicum.category;

import lombok.AllArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.handler.exception.NotFoundException;

import java.util.List;

@Validated
@RestController
@AllArgsConstructor
@RequestMapping(path = "/categories")
public class PublicCategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryDto>> getCategories(@RequestParam(name = "from", defaultValue = "0") Integer from,
                                                          @RequestParam(name = "size", defaultValue = "10") Integer size) throws BadRequestException {
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id").descending());

        try {
            return ResponseEntity.ok().body(categoryService.getCategories(pageable).getContent());
        } catch (RuntimeException ex) {
            throw new BadRequestException();
        }
    }

    @GetMapping("/{catId}")
    public ResponseEntity<CategoryDto> getCategory(@PathVariable("catId") Long catId) throws NotFoundException {
        try {
            return ResponseEntity.ok().body(categoryService.getCategory(catId));
        } catch (RuntimeException ex) {
            throw new NotFoundException(ex.getMessage());
        }
    }
}

