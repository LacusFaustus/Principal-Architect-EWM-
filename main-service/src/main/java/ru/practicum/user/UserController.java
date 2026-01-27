package ru.practicum.user;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping(path = "/users")
public class UserController {
    private final UserService userService;
}
