package ru.practicum.user.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.service.UserService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AdminUserController controller;

    @Test
    void createUser_ShouldReturnCreated() {
        NewUserRequest request = new NewUserRequest();
        request.setEmail("test@example.com");
        request.setName("Test User");

        UserDto expected = new UserDto();
        expected.setId(1L);
        expected.setEmail("test@example.com");
        expected.setName("Test User");

        when(userService.createUser(any(NewUserRequest.class))).thenReturn(expected);

        ResponseEntity<UserDto> response = controller.createUser(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("test@example.com", response.getBody().getEmail());
    }

    @Test
    void deleteUser_ShouldReturnNoContent() {
        Long userId = 1L;

        doNothing().when(userService).deleteUser(userId);

        ResponseEntity<Void> response = controller.deleteUser(userId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(userService).deleteUser(userId);
    }

    @Test
    void getUsers_ShouldReturnList() {
        List<Long> ids = List.of(1L, 2L);
        int from = 0;
        int size = 10;

        UserDto user1 = new UserDto();
        user1.setId(1L);
        user1.setName("User 1");

        UserDto user2 = new UserDto();
        user2.setId(2L);
        user2.setName("User 2");

        Page<UserDto> page = new PageImpl<>(List.of(user1, user2));

        when(userService.getUsersByIds(eq(ids), any(PageRequest.class))).thenReturn(page);

        ResponseEntity<List<UserDto>> response = controller.getUsers(ids, from, size);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
    }
}