package ru.practicum.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.practicum.saga.SagaOrchestrator;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.exception.ConflictException;
import ru.practicum.user.exception.NotFoundException;
import ru.practicum.user.mapper.UserMapper;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private SagaOrchestrator sagaOrchestrator;

    @Mock
    private AuthServiceClient authServiceClient;

    @InjectMocks
    private UserServiceSagaImpl userService;

    private NewUserRequest newUserRequest;
    private User user;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        newUserRequest = new NewUserRequest();
        newUserRequest.setEmail("test@example.com");
        newUserRequest.setName("Test User");

        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setName("Test User");

        userDto = new UserDto();
        userDto.setId(1L);
        userDto.setEmail("test@example.com");
        userDto.setName("Test User");
    }

    @Test
    void createUser_EmailAlreadyExists_ThrowsConflictException() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

        // Act & Assert
        assertThrows(ConflictException.class, () -> userService.createUser(newUserRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUserById_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.mapToUserDto(any(User.class))).thenReturn(userDto);

        // Act
        UserDto result = userService.getUserById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("Test User", result.getName());
    }

    @Test
    void getUserById_NotFound_ThrowsNotFoundException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> userService.getUserById(1L));
    }

    @Test
    void deleteUser_Success() {
        // Arrange
        when(userRepository.existsById(1L)).thenReturn(true);
        doNothing().when(userRepository).deleteById(1L);
        doNothing().when(authServiceClient).deleteUserCredentials(1L);

        // Act
        userService.deleteUser(1L);

        // Assert
        verify(userRepository).deleteById(1L);
        verify(authServiceClient).deleteUserCredentials(1L);
    }

    @Test
    void deleteUser_NotFound_ThrowsNotFoundException() {
        // Arrange
        when(userRepository.existsById(1L)).thenReturn(false);

        // Act & Assert
        assertThrows(NotFoundException.class, () -> userService.deleteUser(1L));
    }

    @Test
    void getUsersByIdsList_Success() {
        // Arrange
        List<Long> ids = List.of(1L, 2L);
        List<User> users = List.of(user);
        when(userRepository.findAllById(ids)).thenReturn(users);
        when(userMapper.mapToUserDto(any(User.class))).thenReturn(userDto);

        // Act
        List<UserDto> result = userService.getUsersByIdsList(ids);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("test@example.com", result.get(0).getEmail());
    }

    @Test
    void getUsersByIdsList_EmptyList() {
        // Act
        List<UserDto> result = userService.getUsersByIdsList(null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getUsersByIdsList_WithEmptyList() {
        // Arrange
        List<Long> ids = List.of();

        // Act
        List<UserDto> result = userService.getUsersByIdsList(ids);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRepository, never()).findAllById(any());
    }

    @Test
    void getAllUsers_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.findAll(pageable)).thenReturn(page);
        when(userMapper.mapToUserDto(any(User.class))).thenReturn(userDto);

        // Act
        Page<UserDto> result = userService.getAllUsers(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("test@example.com", result.getContent().get(0).getEmail());
    }

    @Test
    void getUsersByIds_WithIds_Success() {
        // Arrange
        List<Long> ids = List.of(1L, 2L);
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.findByIdIn(ids, pageable)).thenReturn(page);
        when(userMapper.mapToUserDto(any(User.class))).thenReturn(userDto);

        // Act
        Page<UserDto> result = userService.getUsersByIds(ids, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("test@example.com", result.getContent().get(0).getEmail());
    }

    @Test
    void getUsersByIds_WithoutIds_Success() {
        // Arrange
        List<Long> ids = null;
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.findAll(pageable)).thenReturn(page);
        when(userMapper.mapToUserDto(any(User.class))).thenReturn(userDto);

        // Act
        Page<UserDto> result = userService.getUsersByIds(ids, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("test@example.com", result.getContent().get(0).getEmail());
    }
}