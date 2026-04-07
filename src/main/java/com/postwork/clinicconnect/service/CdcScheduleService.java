package com.postwork.clinicconnect.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.postwork.clinicconnect.exception.CdcApiException;
import com.postwork.clinicconnect.model.VaccineRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;

@Service
public class CdcScheduleService {

    private static final Logger log = LoggerFactory.getLogger(CdcScheduleService.class);

    private final WebClient cdcWebClient;
    private final ObjectMapper objectMapper;

    @Value("${cdc.api.schedule-endpoint}")
    private String scheduleEndpoint;

    @Value("${cdc.api.max-retry-attempts:3}")
    private int maxRetryAttempts;

    @Value("${cdc.api.retry-delay-seconds:2}")
    private int retryDelaySeconds;

    @Value("${cdc.api.use-fallback-only:true}")
    private boolean useFallbackOnly;

    public CdcScheduleService(WebClient cdcWebClient, ObjectMapper objectMapper) {
        this.cdcWebClient = cdcWebClient;
        this.objectMapper = objectMapper;
    }

    public List<VaccineRequirement> fetchImmunizationSchedule() {
        if (useFallbackOnly) {
            log.info("Fallback-only mode enabled. Loading local CDC schedule.");
            return loadFallbackSchedule();
        }

        log.info("Attempting to fetch immunization schedule from CDC remote source");
        try {
            List<VaccineRequirement> schedule = fetchFromRemoteSource();
            log.info("Successfully fetched {} vaccine requirements from remote source",
                    schedule.size());
            return schedule;
        } catch (CdcApiException e) {
            log.warn("Remote CDC source unavailable: {}. Switching to fallback.", e.getMessage());
            return loadFallbackSchedule();
        }
    }

    private List<VaccineRequirement> fetchFromRemoteSource() {
        return cdcWebClient.get()
                .uri(scheduleEndpoint)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        response -> Mono.error(new CdcApiException(
                                "CDC API returned 4xx: " + response.statusCode())))
                .onStatus(HttpStatusCode::is5xxServerError,
                        response -> Mono.error(new CdcApiException(
                                "CDC API returned 5xx: " + response.statusCode())))
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(maxRetryAttempts, Duration.ofSeconds(retryDelaySeconds))
                        .filter(throwable -> throwable instanceof WebClientRequestException)
                        .onRetryExhaustedThrow((spec, signal) ->
                                new CdcApiException("CDC API exhausted after retries")))
                .map(this::parseScheduleResponse)
                .timeout(Duration.ofSeconds(15))
                .onErrorMap(e -> new CdcApiException("Remote fetch failed: " + e.getMessage()))
                .block();
    }

    private List<VaccineRequirement> parseScheduleResponse(String jsonResponse) {
        try {
            if (jsonResponse == null || jsonResponse.isBlank()) {
                throw new CdcApiException("Empty response from CDC API");
            }
            return objectMapper.readValue(jsonResponse,
                    new TypeReference<List<VaccineRequirement>>() {});
        } catch (IOException e) {
            log.error("Malformed JSON from CDC API: {}", e.getMessage());
            throw new CdcApiException("Malformed CDC API response: " + e.getMessage());
        }
    }

    public List<VaccineRequirement> loadFallbackSchedule() {
        log.info("Loading fallback CDC schedule from local resource");
        try {
            ClassPathResource resource = new ClassPathResource("cdc-schedule-fallback.json");
            InputStream inputStream = resource.getInputStream();
            List<VaccineRequirement> schedule = objectMapper.readValue(
                    inputStream, new TypeReference<List<VaccineRequirement>>() {});
            log.info("Fallback schedule loaded with {} vaccines", schedule.size());
            return schedule;
        } catch (IOException e) {
            log.error("Cannot load fallback schedule: {}", e.getMessage());
            throw new CdcApiException("Both remote and fallback schedule sources failed");
        }
    }

    public boolean isRemoteSourceAvailable() {
        if (useFallbackOnly) {
            return false;
        }
        try {
            fetchFromRemoteSource();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}