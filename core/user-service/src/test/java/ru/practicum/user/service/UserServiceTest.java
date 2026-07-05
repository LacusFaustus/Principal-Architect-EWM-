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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
    void getUserById_ShouldReturnUserDto() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.mapToUserDto(any(User.class))).thenReturn(userDto);

        UserDto result = userService.getUserById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("Test User", result.getName());
        verify(userRepository).findById(1L);
        verify(userMapper).mapToUserDto(user);
    }

    @Test
    void getUserById_NotFound_ThrowsNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.getUserById(999L));
        verify(userRepository).findById(999L);
        verify(userMapper, never()).mapToUserDto(any(User.class));
    }

    @Test
    void deleteUser_ShouldDelete() {
        when(userRepository.existsById(1L)).thenReturn(true);
        doNothing().when(authServiceClient).deleteUserCredentials(1L);

        userService.deleteUser(1L);

        verify(userRepository).deleteById(1L);
        verify(authServiceClient).deleteUserCredentials(1L);
    }

    @Test
    void deleteUser_NotFound_ThrowsNotFoundException() {
        when(userRepository.existsById(999L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> userService.deleteUser(999L));
        verify(userRepository, never()).deleteById(anyLong());
        verify(authServiceClient, never()).deleteUserCredentials(anyLong());
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
        verify(userRepository).findAll(pageable);
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
        assertEquals("test@example.com", result.getContent().get(0).getEmail());
        verify(userRepository).findByIdIn(ids, pageable);
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
        verify(userRepository).findAll(pageable);
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
        assertEquals("test@example.com", result.get(0).getEmail());
        verify(userRepository).findAllById(ids);
    }

    @Test
    void getUsersByIdsList_EmptyList_ShouldReturnEmpty() {
        List<UserDto> result = userService.getUsersByIdsList(Collections.emptyList());

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRepository, never()).findAllById(any());
    }

    @Test
    void getUsersByIdsList_NullList_ShouldReturnEmpty() {
        List<UserDto> result = userService.getUsersByIdsList(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRepository, never()).findAllById(any());
    }

    @Test
    void createUser_EmailAlreadyExists_ThrowsConflictException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

        assertThrows(ConflictException.class, () -> userService.createUser(newUserRequest));
        verify(userRepository, never()).save(any(User.class));
        verify(authServiceClient, never()).createUserCredentials(anyLong(), anyString(), anyString());
    }
}