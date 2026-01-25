package ru.practicum.statservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "endpoint_hits")
public class EndpointHit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "APP", nullable = false)
    private String app;

    @Column(name = "URI", nullable = false, length = 2048)
    private String uri;

    @Column(name = "IP", nullable = false, length = 45)
    private String ip;

    @Column(name = "TIMESTAMP", nullable = false)
    private LocalDateTime timestamp;

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        EndpointHit endpointHit = (EndpointHit) object;
        return id != 0 && id.equals(endpointHit.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
