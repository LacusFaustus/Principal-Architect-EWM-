package ru.practicum.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.handler.ErrorHandler;
import ru.practicum.user.service.UserService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AdminUserController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ErrorHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void createUser_ShouldReturnCreated() throws Exception {
        NewUserRequest request = new NewUserRequest();
        request.setEmail("test@example.com");
        request.setName("Test User");

        UserDto response = new UserDto();
        response.setId(1L);
        response.setEmail("test@example.com");
        response.setName("Test User");

        when(userService.createUser(any(NewUserRequest.class))).thenReturn(response);

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void deleteUser_ShouldReturnNoContent() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/admin/users/1"))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(1L);
    }

    @Test
    void getUsers_ShouldReturnList() throws Exception {
        List<Long> ids = List.of(1L, 2L);
        PageRequest pageable = PageRequest.of(0, 10);

        UserDto user1 = new UserDto();
        user1.setId(1L);
        user1.setName("User 1");
        user1.setEmail("user1@example.com");

        UserDto user2 = new UserDto();
        user2.setId(2L);
        user2.setName("User 2");
        user2.setEmail("user2@example.com");

        Page<UserDto> page = new PageImpl<>(List.of(user1, user2));

        when(userService.getUsersByIds(any(), any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/admin/users")
                        .param("ids", "1,2")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L));
    }

    @Test
    void getUsers_WithPagination_ShouldReturnList() throws Exception {
        PageRequest pageable = PageRequest.of(0, 5);
        UserDto user1 = new UserDto();
        user1.setId(1L);
        user1.setName("User 1");

        Page<UserDto> page = new PageImpl<>(List.of(user1));

        when(userService.getUsersByIds(any(), any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/admin/users")
                        .param("from", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }
}