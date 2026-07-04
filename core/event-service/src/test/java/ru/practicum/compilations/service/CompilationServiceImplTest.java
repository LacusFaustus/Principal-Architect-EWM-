package ru.practicum.compilations.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.compilations.dto.CompilationDto;
import ru.practicum.compilations.dto.NewCompilationDto;
import ru.practicum.compilations.mapper.CompilationMapper;
import ru.practicum.compilations.mapper.CompilationMapperImpl;
import ru.practicum.compilations.model.Compilation;
import ru.practicum.compilations.repository.CompilationRepository;
import ru.practicum.handler.exception.NotFoundException;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompilationServiceImplTest {

    @Mock
    private CompilationRepository compilationRepository;

    @Spy
    private CompilationMapper compilationMapper = new CompilationMapperImpl();

    @InjectMocks
    private CompilationServiceImpl compilationService;

    private Compilation compilation;
    private NewCompilationDto newCompilationDto;

    @BeforeEach
    void setUp() {
        compilation = new Compilation();
        compilation.setId(1L);
        compilation.setTitle("Top Events");
        compilation.setPinned(true);

        newCompilationDto = new NewCompilationDto();
        newCompilationDto.setTitle("Top Events");
        newCompilationDto.setPinned(true);
        newCompilationDto.setEvents(Collections.emptySet());
    }

    @Test
    void add_ShouldReturnCompilationDto() {
        // Arrange
        when(compilationRepository.save(any(Compilation.class))).thenReturn(compilation);

        // Act
        CompilationDto result = compilationService.add(newCompilationDto);

        // Assert
        assertNotNull(result);
        assertEquals("Top Events", result.getTitle());
        assertTrue(result.getPinned());
        verify(compilationRepository).save(any(Compilation.class));
    }

    @Test
    void update_ShouldUpdateCompilation() {
        // Arrange
        ru.practicum.compilations.dto.UpdateCompilationRequest updateRequest =
                new ru.practicum.compilations.dto.UpdateCompilationRequest();
        updateRequest.setTitle("Updated Title");
        updateRequest.setPinned(false);

        when(compilationRepository.findById(1L)).thenReturn(Optional.of(compilation));
        when(compilationRepository.save(any(Compilation.class))).thenReturn(compilation);

        // Act
        CompilationDto result = compilationService.update(1L, updateRequest);

        // Assert
        assertNotNull(result);
        assertEquals("Top Events", result.getTitle());
        verify(compilationRepository).save(any(Compilation.class));
    }

    @Test
    void update_NotFound_ThrowsNotFoundException() {
        // Arrange
        ru.practicum.compilations.dto.UpdateCompilationRequest updateRequest =
                new ru.practicum.compilations.dto.UpdateCompilationRequest();

        when(compilationRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> compilationService.update(999L, updateRequest));
        verify(compilationRepository, never()).save(any(Compilation.class));
    }

    @Test
    void delete_ShouldDelete() {
        // Arrange
        when(compilationRepository.existsById(1L)).thenReturn(true);

        // Act
        compilationService.delete(1L);

        // Assert
        verify(compilationRepository).deleteById(1L);
    }

    @Test
    void delete_NotFound_ThrowsNotFoundException() {
        // Arrange
        when(compilationRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThrows(NotFoundException.class, () -> compilationService.delete(999L));
        verify(compilationRepository, never()).deleteById(anyLong());
    }

    @Test
    void get_ShouldReturnCompilationDto() {
        // Arrange
        when(compilationRepository.findById(1L)).thenReturn(Optional.of(compilation));

        // Act
        CompilationDto result = compilationService.get(1L);

        // Assert
        assertNotNull(result);
        assertEquals("Top Events", result.getTitle());
    }

    @Test
    void get_NotFound_ThrowsNotFoundException() {
        // Arrange
        when(compilationRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> compilationService.get(999L));
    }
}