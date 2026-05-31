package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.UserInfoDto;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class UserFeignClientFallback implements UserFeignClient {

    @Override
    public UserInfoDto getUserById(Long userId) {
        log.warn("Fallback: User service unavailable for getUserById({})", userId);
        UserInfoDto defaultUser = new UserInfoDto();
        defaultUser.setId(userId);
        defaultUser.setName("Unknown User");
        defaultUser.setEmail("unknown@example.com");
        return defaultUser;
    }

    @Override
    public List<UserInfoDto> getUsersByIds(List<Long> ids) {
        log.warn("Fallback: User service unavailable for getUsersByIds({})", ids);
        List<UserInfoDto> defaultUsers = new ArrayList<>();
        for (Long id : ids) {
            UserInfoDto defaultUser = new UserInfoDto();
            defaultUser.setId(id);
            defaultUser.setName("Unknown User");
            defaultUser.setEmail("unknown@example.com");
            defaultUsers.add(defaultUser);
        }
        return defaultUsers;
    }
}