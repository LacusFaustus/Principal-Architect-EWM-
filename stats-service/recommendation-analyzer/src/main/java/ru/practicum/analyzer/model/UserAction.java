package ru.practicum.analyzer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_actions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "weight", nullable = false)
    private Double weight;

    @Column(name = "action_type", nullable = false)
    private String actionType;

    @Column(name = "last_action_time", nullable = false)
    private LocalDateTime lastActionTime;
}