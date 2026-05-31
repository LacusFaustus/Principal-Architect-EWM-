package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.dto.UserInfoDto;

import java.util.List;

@FeignClient(name = "user-service", fallback = UserFeignClientFallback.class)
public interface UserFeignClient {

    @GetMapping("/users/{userId}")
    UserInfoDto getUserById(@PathVariable("userId") Long userId);

    @PostMapping("/users/batch")
    List<UserInfoDto> getUsersByIds(@RequestBody List<Long> ids);
}