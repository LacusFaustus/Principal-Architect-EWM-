package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;
import ru.practicum.dto.NewEndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class StatClient {
    private final DiscoveryClient discoveryClient;
    private final RestClient restClient;
    private final RetryTemplate retryTemplate;
    private final String statsServiceId;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StatClient(DiscoveryClient discoveryClient, RestClient restClient,
                      RetryTemplate retryTemplate, String statsServiceId) {
        this.discoveryClient = discoveryClient;
        this.restClient = restClient;
        this.retryTemplate = retryTemplate;
        this.statsServiceId = statsServiceId;
    }

    private URI makeUri(String path) {
        ServiceInstance instance = retryTemplate.execute(context -> getInstance());
        return URI.create("http://" + instance.getHost() + ":" + instance.getPort() + path);
    }

    private ServiceInstance getInstance() {
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(statsServiceId);
            if (instances.isEmpty()) {
                throw new RuntimeException("No instances found for service: " + statsServiceId);
            }
            return instances.getFirst();
        } catch (Exception exception) {
            throw new RuntimeException("Ошибка обнаружения адреса сервиса статистики с id: " + statsServiceId, exception);
        }
    }

    public void saveHit(NewEndpointHitDto hitDto) {
        try {
            URI uri = makeUri("/hit");
            ResponseEntity<Void> response = restClient.post()
                    .uri(uri)
                    .body(hitDto)
                    .retrieve()
                    .toBodilessEntity();

            if (response.getStatusCode().isError()) {
                throw new RuntimeException("Failed to save hit: " + response.getStatusCode());
            }
            log.debug("Successfully saved hit to stats service");
        } catch (Exception e) {
            log.error("Error while saving hit to stats service: {}", e.getMessage());
            throw new RuntimeException("Error while saving hit to stats service", e);
        }
    }

    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end,
                                       List<String> uris, boolean unique) {
        try {
            String startStr = start.format(FORMATTER);
            String endStr = end.format(FORMATTER);

            URI uri = makeUri("/stats");

            ResponseEntity<ViewStatsDto[]> response = restClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/stats")
                                .queryParam("start", startStr)
                                .queryParam("end", endStr)
                                .queryParam("unique", unique);

                        if (uris != null && !uris.isEmpty()) {
                            uriBuilder.queryParam("uris", String.join(",", uris));
                        }

                        return uriBuilder.build();
                    })
                    .retrieve()
                    .toEntity(ViewStatsDto[].class);

            return Arrays.asList(response.getBody() != null ? response.getBody() : new ViewStatsDto[0]);
        } catch (Exception e) {
            log.error("Error while getting stats from stats service: {}", e.getMessage());
            throw new RuntimeException("Error while getting stats from stats service", e);
        }
    }
}