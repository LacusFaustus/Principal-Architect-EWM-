package ru.practicum.user.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.handler.ErrorHandler;
import ru.practicum.user.service.UserService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PublicUserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private PublicUserController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ErrorHandler())
                .build();
    }

    @Test
    void getUserById_ShouldReturnUser() throws Exception {
        UserDto user = new UserDto();
        user.setId(1L);
        user.setName("Test User");
        user.setEmail("test@example.com");

        when(userService.getUserById(1L)).thenReturn(user);

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Test User"));
    }

    @Test
    void getUsersByIds_ShouldReturnList() throws Exception {
        UserDto user1 = new UserDto();
        user1.setId(1L);
        user1.setName("User 1");

        UserDto user2 = new UserDto();
        user2.setId(2L);
        user2.setName("User 2");

        when(userService.getUsersByIdsList(any())).thenReturn(List.of(user1, user2));

        mockMvc.perform(post("/users/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1, 2]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L));
    }
}