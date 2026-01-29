package ru.practicum.compilations.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.event.model.Event;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "compilations")
@Setter
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Compilation {
    @Id
    @Column(name = "compilation_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "compilations_events",
            joinColumns = {@JoinColumn(name = "compilation_id")},
            inverseJoinColumns = {@JoinColumn(name = "event_id")})
    private Set<Event> events = new HashSet<>();

    @Column(name = "pinned")
    private Boolean pinned = false;

    @Column(name = "title")
    private String title;
}
