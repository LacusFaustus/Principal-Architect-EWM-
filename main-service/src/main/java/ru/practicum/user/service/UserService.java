package ru.practicum.user.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;

import java.util.List;

public interface UserService {
    UserDto postUser(NewUserRequest newUserRequest);

    void deleteUser(Long userId);

    Page<UserDto> getUsers(List<Long> ids, Pageable pageable);

    Page<UserDto> getAllUsers(Pageable pageable);

}
