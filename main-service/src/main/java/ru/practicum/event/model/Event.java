package ru.practicum.event.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.category.model.Category;
import ru.practicum.location.model.LocationEntity;
import ru.practicum.user.model.User;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String annotation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private LocalDateTime createdOn;

    @Column(length = 7000)
    private String description;

    @Column(nullable = false)
    private LocalDateTime eventDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiator_id", nullable = false)
    private User initiator;

    @Embedded
    private LocationEntity location;

    @Column(nullable = false)
    private Boolean paid;

    @Column(nullable = false)
    private Integer participantLimit;

    private LocalDateTime publishedOn;

    @Column(nullable = false)
    private Boolean requestModeration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventState state;

    @Column(nullable = false, length = 120)
    private String title;

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        Event event = (Event) object;
        return id != 0 && id.equals(event.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
