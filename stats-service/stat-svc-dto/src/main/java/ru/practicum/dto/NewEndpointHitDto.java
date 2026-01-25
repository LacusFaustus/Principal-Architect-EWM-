package ru.practicum.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewEndpointHitDto {
    @NotBlank(message = "app is blank")
    private String app;

    @NotBlank(message = "uri is blank")
    private String uri;

    @NotBlank(message = "ip is blank")
    private String ip;

    @PastOrPresent(message = "timestamp is future")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
}
