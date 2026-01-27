package ru.practicum.user;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;

import java.util.List;

@Validated
@RestController
@AllArgsConstructor
@RequestMapping(path = "/admin/users")
public class AdminUserController {
    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserDto> postUser(@Valid @RequestBody NewUserRequest newUserRequest) throws BadRequestException {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(userService.postUser(newUserRequest));
        } catch (RuntimeException ex) {
            throw new BadRequestException();
        }
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable("userId") Long userId) throws BadRequestException {
        try {
            userService.deleteUser(userId);
        } catch (RuntimeException ex) {
            throw new BadRequestException();
        }

        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> getUsers(@RequestParam List<Long> ids,
                                                  @RequestParam(name = "from", defaultValue = "0") Integer from,
                                                  @RequestParam(name = "size", defaultValue = "10") Integer size) throws BadRequestException {
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id").descending());

        try {
            return ResponseEntity.ok().body(userService.getUsers(ids, pageable).getContent());
        } catch (RuntimeException ex) {
            throw new BadRequestException();
        }
    }
}

