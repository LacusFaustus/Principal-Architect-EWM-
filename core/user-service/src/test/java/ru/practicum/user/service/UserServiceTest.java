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
import org.springframework.web.client.RestTemplate;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.exception.ConflictException;
import ru.practicum.user.exception.NotFoundException;
import ru.practicum.user.mapper.UserMapper;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.util.Collections;
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
    private RestTemplate restTemplate;

    @InjectMocks
    private UserServiceImpl userService;

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
    void createUser_ShouldReturnUserDto() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userMapper.mapToUser(any(NewUserRequest.class))).thenReturn(user);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.mapToUserDto(any(User.class))).thenReturn(userDto);

        UserDto result = userService.createUser(newUserRequest);

        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        assertEquals("Test User", result.getName());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_EmailAlreadyExists_ThrowsConflictException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

        assertThrows(ConflictException.class, () -> userService.createUser(newUserRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUserById_ShouldReturnUserDto() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.mapToUserDto(any(User.class))).thenReturn(userDto);

        UserDto result = userService.getUserById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void getUserById_NotFound_ThrowsNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.getUserById(999L));
    }

    @Test
    void deleteUser_ShouldDelete() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteUser(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_NotFound_ThrowsNotFoundException() {
        when(userRepository.existsById(999L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> userService.deleteUser(999L));
    }

    @Test
    void getAllUsers_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(user));

        when(userRepository.findAll(pageable)).thenReturn(page);
        when(userMapper.mapToUserDto(any(User.class))).thenReturn(userDto);

        Page<UserDto> result = userService.getAllUsers(pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("test@example.com", result.getContent().get(0).getEmail());
    }

    @Test
    void getUsersByIds_ShouldReturnPage() {
        List<Long> ids = List.of(1L, 2L);
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(user));

        when(userRepository.findByIdIn(ids, pageable)).thenReturn(page);
        when(userMapper.mapToUserDto(any(User.class))).thenReturn(userDto);

        Page<UserDto> result = userService.getUsersByIds(ids, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getUsersByIds_WithNullIds_ShouldReturnAll() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(user));

        when(userRepository.findAll(pageable)).thenReturn(page);
        when(userMapper.mapToUserDto(any(User.class))).thenReturn(userDto);

        Page<UserDto> result = userService.getUsersByIds(null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getUsersByIds_WithEmptyIds_ShouldReturnAll() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(user));

        when(userRepository.findAll(pageable)).thenReturn(page);
        when(userMapper.mapToUserDto(any(User.class))).thenReturn(userDto);

        Page<UserDto> result = userService.getUsersByIds(Collections.emptyList(), pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(userRepository).findAll(pageable);
    }

    @Test
    void getUsersByIdsList_ShouldReturnList() {
        List<Long> ids = List.of(1L, 2L);
        when(userRepository.findAllById(ids)).thenReturn(List.of(user));
        when(userMapper.mapToUserDto(any(User.class))).thenReturn(userDto);

        List<UserDto> result = userService.getUsersByIdsList(ids);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userRepository).findAllById(ids);
    }

    @Test
    void getUsersByIdsList_EmptyList_ShouldReturnEmpty() {
        // Act
        List<UserDto> result = userService.getUsersByIdsList(Collections.emptyList());

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRepository, never()).findAllById(any());
    }

    @Test
    void getUsersByIdsList_NullList_ShouldReturnEmpty() {
        // Act
        List<UserDto> result = userService.getUsersByIdsList(null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRepository, never()).findAllById(any());
    }

    @Test
    void createUser_WithAuthServiceFailure_ShouldStillCreateUser() {
        // Arrange - когда auth-service недоступен
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userMapper.mapToUser(any(NewUserRequest.class))).thenReturn(user);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.mapToUserDto(any(User.class))).thenReturn(userDto);

        // Мокаем ошибку при вызове auth-service
        doThrow(new RuntimeException("Auth service unavailable"))
                .when(restTemplate).postForEntity(anyString(), any(), eq(Void.class));

        // Act
        UserDto result = userService.createUser(newUserRequest);

        // Assert
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository).save(any(User.class));
    }
}