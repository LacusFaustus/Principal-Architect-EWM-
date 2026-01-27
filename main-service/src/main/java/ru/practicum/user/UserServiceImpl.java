package ru.practicum.user;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.handler.NotFoundException;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.mapper.UserMapper;
import ru.practicum.user.model.User;

import java.util.List;


@Slf4j
@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserDto postUser(NewUserRequest newUserRequest) {
        log.info("POST user: {}", newUserRequest);

        User user = userMapper.mapToUser(newUserRequest);
        log.debug("MAP user: {}", user);

        User savedUser = userRepository.save(user);
        log.debug("SAVED user: {}", savedUser);

        return userMapper.mapToUserDto(savedUser);
    }


    @Override
    @Transactional
    public void deleteUser(Long userId) {
        checkUserExists(userId);
        userRepository.deleteById(userId);

        log.info("DELETE user: id={}", userId);
    }

    @Override
    public Page<UserDto> getUsers(List<Long> ids, Pageable pageable) {
        log.info("GET users with pageable");

        Page<UserDto> users = userRepository.findUserByIdIn(ids, pageable)
                .map(userMapper::mapToUserDto);

        log.info("FIND users with pageable: size={}", users.getTotalElements());

        return users;
    }

    private void checkUserExists(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User {} not found", userId);
                    return new NotFoundException("User ID=" + userId + " not found");
                });
    }
}

