package ru.practicum.user.mapper;

import org.junit.jupiter.api.Test;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.dto.UserShortDto;
import ru.practicum.user.model.User;

import static org.junit.jupiter.api.Assertions.*;

class UserMapperTest {

    private final UserMapper userMapper = new UserMapperImpl();

    @Test
    void mapToUser_ShouldMapFromNewUserRequest() {
        NewUserRequest request = new NewUserRequest();
        request.setEmail("test@example.com");
        request.setName("Test User");

        User user = userMapper.mapToUser(request);

        assertNotNull(user);
        assertNull(user.getId());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("Test User", user.getName());
    }

    @Test
    void mapToUser_WithNullRequest_ShouldReturnNull() {
        User user = userMapper.mapToUser(null);
        assertNull(user);
    }

    @Test
    void mapToUserDto_ShouldMapFromUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setName("Test User");

        UserDto dto = userMapper.mapToUserDto(user);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("test@example.com", dto.getEmail());
        assertEquals("Test User", dto.getName());
    }

    @Test
    void mapToUserDto_WithNullUser_ShouldReturnNull() {
        UserDto dto = userMapper.mapToUserDto(null);
        assertNull(dto);
    }

    @Test
    void mapToUserShortDto_ShouldMapFromUser() {
        User user = new User();
        user.setId(1L);
        user.setName("Test User");

        UserShortDto dto = userMapper.mapToUserShortDto(user);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("Test User", dto.getName());
    }

    @Test
    void mapToUserShortDto_WithNullUser_ShouldReturnNull() {
        UserShortDto dto = userMapper.mapToUserShortDto(null);
        assertNull(dto);
    }
}