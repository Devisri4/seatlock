package com.devisri.seatlock.ratelimit;

import com.devisri.seatlock.FixedClock;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The rate limit and CORS over real HTTP: 3 requests per minute here. Visitors are told apart by the
 * X-Forwarded-For header that Render's proxy adds (the test client on 127.0.0.1 plays the proxy).
 * "Now" is 2030-01-01T00:00:00Z, the start of a minute, so Retry-After is 60.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "seatlock.demo-data.enabled=false",
        "seatlock.rate-limit.requests-per-minute=3",
        "seatlock.cors.allowed-origins=https://seatlock.example.com"})
@Import(FixedClock.class)
class RateLimitTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void theFourthRequestInAMinuteIs429WithRetryAfter() {
        for (int i = 0; i < 3; i++) {
            assertThat(get("/api/shows", "203.0.113.1", null).getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        ResponseEntity<JsonNode> blocked = get("/api/shows", "203.0.113.1", null);

        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(blocked.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("60");
        assertThat(blocked.getBody().get("detail").asText()).isEqualTo("Too many requests. Please wait 60 seconds and try again.");
    }

    @Test
    void eachVisitorHasTheirOwnLimit() {
        for (int i = 0; i < 4; i++) {
            get("/api/shows", "203.0.113.2", null);
        }

        assertThat(get("/api/shows", "203.0.113.3", null).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void addingFakeAddressesToTheHeaderDoesNotGetAroundTheLimit() {
        for (int i = 0; i < 3; i++) {
            get("/api/shows", "203.0.113.4", null);
        }

        // The real client is the last address the proxy saw; what the client wrote before it is ignored.
        assertThat(get("/api/shows", "198.51.100.77, 203.0.113.4", null).getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void theHealthCheckIsNeverLimited() {
        for (int i = 0; i < 5; i++) {
            assertThat(get("/actuator/health", "203.0.113.5", null).getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Test
    void theSiteMayCallTheApiAndEvenA429TellsTheBrowserWhyAndWhen() {
        for (int i = 0; i < 3; i++) {
            get("/api/shows", "203.0.113.6", "https://seatlock.example.com");
        }

        ResponseEntity<JsonNode> blocked = get("/api/shows", "203.0.113.6", "https://seatlock.example.com");

        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(blocked.getHeaders().getAccessControlAllowOrigin()).isEqualTo("https://seatlock.example.com");
        assertThat(blocked.getHeaders().getAccessControlExposeHeaders()).contains("Retry-After");
    }

    @Test
    void preflightAllowsTheSiteAndItsCustomHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin("https://seatlock.example.com");
        headers.setAccessControlRequestMethod(HttpMethod.POST);
        headers.setAccessControlRequestHeaders(java.util.List.of("Content-Type", "X-Guest-Id", "Idempotency-Key"));

        ResponseEntity<String> response = rest.exchange("/api/shows/1/bookings", HttpMethod.OPTIONS, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getAccessControlAllowOrigin()).isEqualTo("https://seatlock.example.com");
        assertThat(response.getHeaders().getAccessControlAllowHeaders()).contains("X-Guest-Id", "Idempotency-Key");
    }

    @Test
    void otherWebsitesMayNotCallTheApi() {
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin("https://evil.example.org");
        headers.setAccessControlRequestMethod(HttpMethod.POST);

        ResponseEntity<String> response = rest.exchange("/api/shows/1/bookings", HttpMethod.OPTIONS, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private ResponseEntity<JsonNode> get(String path, String forwardedFor, String origin) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Forwarded-For", forwardedFor);
        if (origin != null) {
            headers.setOrigin(origin);
        }
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
    }
}
