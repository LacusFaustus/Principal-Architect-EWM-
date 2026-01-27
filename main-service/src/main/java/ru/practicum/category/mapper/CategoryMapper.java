package ru.practicum.category.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.category.dto.NewCategoryRequest;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.model.Category;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    @Mapping(target = "id", ignore = true)
    Category mapToCategory(NewCategoryRequest newCategoryRequest);

    CategoryDto mapToCategoryDto(Category category);
}
