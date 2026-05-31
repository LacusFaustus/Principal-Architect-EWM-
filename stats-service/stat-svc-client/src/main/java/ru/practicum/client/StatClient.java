package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.practicum.dto.NewEndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class StatClient {
    private final RestTemplate restTemplate;
    private final String serverUrl;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StatClient(@Value("${stats-server.url:http://localhost:9090}") String serverUrl) {
        this.serverUrl = serverUrl;
        this.restTemplate = new RestTemplate();
    }

    public void saveHit(NewEndpointHitDto hitDto) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<NewEndpointHitDto> entity = new HttpEntity<>(hitDto, headers);

            restTemplate.exchange(
                    serverUrl + "/hit",
                    HttpMethod.POST,
                    entity,
                    Void.class
            );
            log.debug("Hit saved successfully: {}", hitDto);
        } catch (Exception e) {
            log.error("Error while saving hit to stats service: {}", e.getMessage());
        }
    }

    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end,
                                       List<String> uris, boolean unique) {
        try {
            String startStr = start.format(FORMATTER);
            String endStr = end.format(FORMATTER);

            StringBuilder urlBuilder = new StringBuilder(serverUrl + "/stats?start=" + startStr + "&end=" + endStr + "&unique=" + unique);

            if (uris != null && !uris.isEmpty()) {
                for (String uri : uris) {
                    urlBuilder.append("&uris=").append(uri);
                }
            }

            ResponseEntity<ViewStatsDto[]> response = restTemplate.getForEntity(
                    urlBuilder.toString(),
                    ViewStatsDto[].class
            );

            ViewStatsDto[] body = response.getBody();
            return body != null ? Arrays.asList(body) : Collections.emptyList();
        } catch (Exception e) {
            log.error("Error while getting stats from stats service: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}