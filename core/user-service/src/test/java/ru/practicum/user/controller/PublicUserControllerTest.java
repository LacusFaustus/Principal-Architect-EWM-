package ru.practicum.user.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.service.UserService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicUserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private PublicUserController controller;

    @Test
    void getUserById_ShouldReturnUser() {
        Long userId = 1L;
        UserDto expected = new UserDto();
        expected.setId(userId);
        expected.setName("Test User");
        expected.setEmail("test@example.com");

        when(userService.getUserById(userId)).thenReturn(expected);

        ResponseEntity<UserDto> response = controller.getUserById(userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Test User", response.getBody().getName());
    }

    @Test
    void getUsersByIds_ShouldReturnList() {
        List<Long> ids = List.of(1L, 2L);

        UserDto user1 = new UserDto();
        user1.setId(1L);
        user1.setName("User 1");

        UserDto user2 = new UserDto();
        user2.setId(2L);
        user2.setName("User 2");

        List<UserDto> expected = List.of(user1, user2);

        when(userService.getUsersByIdsList(ids)).thenReturn(expected);

        ResponseEntity<List<UserDto>> response = controller.getUsersByIds(ids);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
    }
}