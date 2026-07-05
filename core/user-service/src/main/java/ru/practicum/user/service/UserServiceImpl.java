package ru.practicum.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.saga.SagaOrchestrator;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.exception.ConflictException;
import ru.practicum.user.exception.NotFoundException;
import ru.practicum.user.mapper.UserMapper;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@Primary  // 👈 Добавляем эту аннотацию
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final SagaOrchestrator sagaOrchestrator;
    private final AuthServiceClient authServiceClient;

    @Override
    @Transactional
    public UserDto createUser(NewUserRequest newUserRequest) {
        log.info("Creating user with email: {} using Saga", newUserRequest.getEmail());

        if (userRepository.findByEmail(newUserRequest.getEmail()).isPresent()) {
            throw new ConflictException("User with email " + newUserRequest.getEmail() + " already exists");
        }

        Map<String, Object> context = new HashMap<>();
        context.put("email", newUserRequest.getEmail());
        context.put("name", newUserRequest.getName());

        String sagaId = sagaOrchestrator.startSaga("user_creation", context);
        log.info("Saga started: {}", sagaId);

        try {
            SagaOrchestrator.SagaStep createUserProfileStep = new CreateUserProfileStep(newUserRequest);
            Map<String, Object> userParams = new HashMap<>();
            userParams.put("email", newUserRequest.getEmail());
            userParams.put("name", newUserRequest.getName());
            sagaOrchestrator.executeStep(sagaId, createUserProfileStep, userParams);

            Long userId = getCurrentUserId(sagaId);

            SagaOrchestrator.SagaStep createAuthCredentialsStep = new CreateAuthCredentialsStep(authServiceClient);
            Map<String, Object> authParams = new HashMap<>();
            authParams.put("userId", userId);
            authParams.put("email", newUserRequest.getEmail());
            authParams.put("password", generateSecurePassword());
            sagaOrchestrator.executeStep(sagaId, createAuthCredentialsStep, authParams);

            sagaOrchestrator.completeSaga(sagaId);

            return getCreatedUserDto(sagaId);

        } catch (Exception e) {
            log.error("Saga execution failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create user: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public void deleteUser(Long userId) {
        log.info("Deleting user: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User not found: " + userId);
        }

        userRepository.deleteById(userId);

        try {
            authServiceClient.deleteUserCredentials(userId);
            log.info("User credentials deleted: {}", userId);
        } catch (Exception e) {
            log.error("Failed to delete auth credentials: {}", e.getMessage());
        }
    }

    @Override
    @Cacheable(value = "users", key = "#userId", unless = "#result == null")
    public UserDto getUserById(Long userId) {
        log.info("Getting user: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        return userMapper.mapToUserDto(user);
    }

    @Override
    public Page<UserDto> getUsersByIds(List<Long> ids, Pageable pageable) {
        log.info("Getting users by ids: {}", ids);

        if (ids == null || ids.isEmpty()) {
            return userRepository.findAll(pageable).map(userMapper::mapToUserDto);
        }

        return userRepository.findByIdIn(ids, pageable).map(userMapper::mapToUserDto);
    }

    @Override
    public Page<UserDto> getAllUsers(Pageable pageable) {
        log.info("Getting all users");
        return userRepository.findAll(pageable).map(userMapper::mapToUserDto);
    }

    @Override
    public List<UserDto> getUsersByIdsList(List<Long> ids) {
        log.info("Getting users list by ids: {}", ids);

        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        return userRepository.findAllById(ids).stream()
                .map(userMapper::mapToUserDto)
                .collect(Collectors.toList());
    }

    private Long getCurrentUserId(String sagaId) {
        SagaOrchestrator.SagaExecution saga = sagaOrchestrator.getSagaStatus(sagaId);
        return saga.getSteps().stream()
                .filter(step -> "create_user_profile".equals(step.getStepName()))
                .findFirst()
                .map(step -> (Long) step.getResult().get("userId"))
                .orElseThrow(() -> new RuntimeException("User ID not found in saga"));
    }

    private UserDto getCreatedUserDto(String sagaId) {
        Long userId = getCurrentUserId(sagaId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return userMapper.mapToUserDto(user);
    }

    private String generateSecurePassword() {
        return java.util.UUID.randomUUID().toString()
                + java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    @RequiredArgsConstructor
    private class CreateUserProfileStep implements SagaOrchestrator.SagaStep {
        private final NewUserRequest newUserRequest;

        @Override
        public String getName() {
            return "create_user_profile";
        }

        @Override
        public SagaOrchestrator.SagaStepResult execute(Map<String, Object> params) {
            try {
                User user = userMapper.mapToUser(newUserRequest);
                User savedUser = userRepository.save(user);

                Map<String, Object> result = new HashMap<>();
                result.put("userId", savedUser.getId());
                result.put("userName", savedUser.getName());
                result.put("userEmail", savedUser.getEmail());

                return SagaOrchestrator.SagaStepResult.success(result);

            } catch (Exception e) {
                log.error("Failed to create user profile", e);
                return SagaOrchestrator.SagaStepResult.failure(e.getMessage());
            }
        }
    }

    @RequiredArgsConstructor
    private class CreateAuthCredentialsStep implements SagaOrchestrator.SagaStep {
        private final AuthServiceClient authServiceClient;

        @Override
        public String getName() {
            return "create_user_credentials";
        }

        @Override
        public SagaOrchestrator.SagaStepResult execute(Map<String, Object> params) {
            try {
                Long userId = (Long) params.get("userId");
                String email = (String) params.get("email");
                String password = (String) params.get("password");

                authServiceClient.createUserCredentials(userId, email, password);

                Map<String, Object> result = new HashMap<>();
                result.put("userId", userId);
                result.put("credentialsCreated", true);

                return SagaOrchestrator.SagaStepResult.success(result);

            } catch (Exception e) {
                log.error("Failed to create auth credentials", e);
                return SagaOrchestrator.SagaStepResult.failure(e.getMessage());
            }
        }
    }
}