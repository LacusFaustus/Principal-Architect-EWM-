package ru.practicum.category.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryRequest;
import ru.practicum.category.dto.UpdateCategoryDto;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.handler.exception.ConflictException;
import ru.practicum.handler.exception.NotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category category;
    private CategoryDto categoryDto;
    private NewCategoryRequest newCategoryRequest;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setId(1L);
        category.setName("Concert");

        categoryDto = new CategoryDto(1L, "Concert");

        newCategoryRequest = new NewCategoryRequest();
        newCategoryRequest.setName("Concert");
    }

    @Test
    void postCategory_ShouldReturnCategoryDto() {
        when(categoryRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(categoryMapper.mapToCategory(any(NewCategoryRequest.class))).thenReturn(category);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);
        when(categoryMapper.mapToCategoryDto(any(Category.class))).thenReturn(categoryDto);

        CategoryDto result = categoryService.postCategory(newCategoryRequest);

        assertNotNull(result);
        assertEquals("Concert", result.getName());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void postCategory_DuplicateName_ThrowsConflictException() {
        when(categoryRepository.findByName("Concert")).thenReturn(Optional.of(category));

        assertThrows(ConflictException.class, () -> categoryService.postCategory(newCategoryRequest));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void deleteCategory_ShouldDelete() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        categoryService.deleteCategory(1L);

        verify(categoryRepository).deleteById(1L);
    }

    @Test
    void deleteCategory_NotFound_ThrowsNotFoundException() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> categoryService.deleteCategory(999L));
        verify(categoryRepository, never()).deleteById(anyLong());
    }

    @Test
    void patchCategory_ShouldUpdate() {
        UpdateCategoryDto updateDto = new UpdateCategoryDto();
        updateDto.setName("Music Festival");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.findByName("Music Festival")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenReturn(category);
        when(categoryMapper.mapToCategoryDto(any(Category.class))).thenReturn(new CategoryDto(1L, "Music Festival"));

        CategoryDto result = categoryService.patchCategory(1L, updateDto);

        assertNotNull(result);
        assertEquals("Music Festival", result.getName());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void patchCategory_DuplicateName_ThrowsConflictException() {
        UpdateCategoryDto updateDto = new UpdateCategoryDto();
        updateDto.setName("Existing Category");

        Category existingCategory = new Category();
        existingCategory.setId(2L);
        existingCategory.setName("Existing Category");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.findByName("Existing Category")).thenReturn(Optional.of(existingCategory));

        assertThrows(ConflictException.class, () -> categoryService.patchCategory(1L, updateDto));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void getCategory_ShouldReturnCategoryDto() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryMapper.mapToCategoryDto(any(Category.class))).thenReturn(categoryDto);

        CategoryDto result = categoryService.getCategory(1L);

        assertNotNull(result);
        assertEquals("Concert", result.getName());
    }
}