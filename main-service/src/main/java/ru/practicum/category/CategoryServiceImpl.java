package ru.practicum.category;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.dto.NewCategoryRequest;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.UpdateCategoryDto;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.category.model.Category;
import ru.practicum.handler.exception.NotFoundException;

@Slf4j
@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryDto postCategory(NewCategoryRequest newCategoryRequest) {
        log.info("POST category: {}", newCategoryRequest);

        Category category = categoryMapper.mapToCategory(newCategoryRequest);
        log.debug("MAP category: {}", category);

        Category savedCategory = categoryRepository.save(category);
        log.debug("SAVED category: {}", savedCategory);

        return categoryMapper.mapToCategoryDto(savedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(Long catId) {
        checkCategoryExists(catId);
        categoryRepository.deleteById(catId);

        log.info("DELETE category: id={}", catId);
    }

    @Override
    @Transactional
    public CategoryDto patchCategory(Long catId, UpdateCategoryDto updateCategoryDto) {
        Category category = checkCategoryExists(catId);
        log.info("PATCH category: id={}, new name: {}", catId, updateCategoryDto);

        category.setName(updateCategoryDto.getName());
        Category patchedCategory = categoryRepository.save(category);
        log.debug("PATCHED category: {}", patchedCategory);

        return categoryMapper.mapToCategoryDto(patchedCategory);
    }

    @Override
    public CategoryDto getCategory(Long catId) {
        log.info("GET category: id={}", catId);
        Category category = checkCategoryExists(catId);

        log.debug("FIND category: {}", category);

        CategoryDto categoryDto = categoryMapper.mapToCategoryDto(category);
        log.debug("MAP category: {}", categoryDto);

        return categoryDto;
    }

    @Override
    public Page<CategoryDto> getCategories(Pageable pageable) {
        log.info("GET categories");

        Page<CategoryDto> categories = categoryRepository.findAll(pageable)
                .map(categoryMapper::mapToCategoryDto);

        log.info("FIND categories: size={}", categories.getTotalElements());

        return categories;
    }

    private Category checkCategoryExists(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> {
                    log.error("Category {} not found", catId);
                    return new NotFoundException("Category ID=" + catId + " not found");
                });
    }
}

