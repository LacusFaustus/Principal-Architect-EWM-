package ru.practicum.category;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.practicum.category.dto.NewCategoryRequest;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.UpdateCategoryDto;

public interface CategoryService {
    CategoryDto postCategory(NewCategoryRequest newCategoryRequest);

    void deleteCategory(Long catId);

    CategoryDto patchCategory(Long catId, UpdateCategoryDto updateCategoryDto);

    CategoryDto getCategory(Long catId);

    Page<CategoryDto> getCategories(Pageable pageable);

}
