package ru.practicum.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.exception.ConflictException;
import ru.practicum.user.exception.NotFoundException;
import ru.practicum.user.mapper.UserMapper;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserDto createUser(NewUserRequest newUserRequest) {
        log.info("Creating new user with email: {}", newUserRequest.getEmail());

        if (userRepository.findByEmail(newUserRequest.getEmail()).isPresent()) {
            log.error("User with email {} already exists", newUserRequest.getEmail());
            throw new ConflictException("User with email " + newUserRequest.getEmail() + " already exists");
        }

        User user = userMapper.mapToUser(newUserRequest);
        User savedUser = userRepository.save(user);
        log.info("User created with id: {}", savedUser.getId());

        return userMapper.mapToUserDto(savedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        log.info("Deleting user with id: {}", userId);

        if (!userRepository.existsById(userId)) {
            log.error("User with id {} not found", userId);
            throw new NotFoundException("User with id=" + userId + " was not found");
        }

        userRepository.deleteById(userId);
        log.info("User deleted with id: {}", userId);
    }

    @Override
    public Page<UserDto> getUsersByIds(List<Long> ids, Pageable pageable) {
        log.info("Getting users by ids: {}", ids);

        Page<User> users;
        if (ids == null || ids.isEmpty()) {
            users = userRepository.findAll(pageable);
        } else {
            users = userRepository.findByIdIn(ids, pageable);
        }

        return users.map(userMapper::mapToUserDto);
    }

    @Override
    public Page<UserDto> getAllUsers(Pageable pageable) {
        log.info("Getting all users");
        return userRepository.findAll(pageable).map(userMapper::mapToUserDto);
    }

    @Override
    public UserDto getUserById(Long userId) {
        log.info("Getting user by id: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User with id {} not found", userId);
                    return new NotFoundException("User with id=" + userId + " was not found");
                });

        return userMapper.mapToUserDto(user);
    }

    @Override
    public List<UserDto> getUsersByIdsList(List<Long> ids) {
        log.info("Getting users list by ids: {}", ids);

        List<User> users = userRepository.findAllById(ids);
        return users.stream()
                .map(userMapper::mapToUserDto)
                .toList();
    }
}