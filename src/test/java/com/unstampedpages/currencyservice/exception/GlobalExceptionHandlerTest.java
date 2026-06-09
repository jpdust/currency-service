package com.unstampedpages.currencyservice.exception;

import com.unstampedpages.currencyservice.service.CurrencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 *
 * <p>Both handler methods are exercised through the real MVC exception-handling
 * pipeline ({@code @RestControllerAdvice} → {@code DispatcherServlet} →
 * response). The service is mocked to throw the target exception so the
 * test stays focused on the handler's output contract.
 *
 * <p>Each test asserts:
 * <ul>
 *   <li>HTTP status</li>
 *   <li>Content-Type (RFC 9457 {@code application/problem+json})</li>
 *   <li>{@code title}, {@code detail}, and {@code type} Problem Detail fields</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = {
        "currency.api.key=test-key",
        "currency.api.base-url=https://api.example.com"
})
class GlobalExceptionHandlerTest {

    @Autowired
    private WebApplicationContext wac;

    @MockitoBean
    private CurrencyService currencyService;

    private RestTestClient restTestClient;

    @BeforeEach
    void setUp() {
        restTestClient = RestTestClient.bindToApplicationContext(wac).build();
    }

    // ── ExternalApiException handler ──────────────────────────────────────────

    @Test
    void handleExternalApiException_returns503() {
        when(currencyService.fetchRates())
                .thenThrow(new ExternalApiException("Upstream returned HTTP 503", 503));

        restTestClient.get().uri("/api/rates")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void handleExternalApiException_responseIsProblemJson() {
        when(currencyService.fetchRates())
                .thenThrow(new ExternalApiException("Upstream returned HTTP 500", 500));

        restTestClient.get().uri("/api/rates")
                .exchange()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void handleExternalApiException_bodyHasCorrectTitle() {
        when(currencyService.fetchRates())
                .thenThrow(new ExternalApiException("Upstream returned HTTP 500", 500));

        restTestClient.get().uri("/api/rates")
                .exchange()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Upstream API Error");
    }

    @Test
    void handleExternalApiException_bodyHasCorrectDetail() {
        when(currencyService.fetchRates())
                .thenThrow(new ExternalApiException("Upstream returned HTTP 500", 500));

        restTestClient.get().uri("/api/rates")
                .exchange()
                .expectBody()
                .jsonPath("$.detail")
                .isEqualTo("The currency rates provider returned an error. Please retry shortly.");
    }

    @Test
    void handleExternalApiException_bodyHasCorrectType() {
        when(currencyService.fetchRates())
                .thenThrow(new ExternalApiException("Upstream returned HTTP 500", 500));

        restTestClient.get().uri("/api/rates")
                .exchange()
                .expectBody()
                .jsonPath("$.type").isEqualTo("urn:currency-service:upstream-error");
    }

    // ── ResourceAccessException handler ──────────────────────────────────────

    @Test
    void handleNetworkException_returns503() {
        when(currencyService.fetchRates())
                .thenThrow(new ResourceAccessException("Connection refused"));

        restTestClient.get().uri("/api/rates")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void handleNetworkException_responseIsProblemJson() {
        when(currencyService.fetchRates())
                .thenThrow(new ResourceAccessException("Connection refused"));

        restTestClient.get().uri("/api/rates")
                .exchange()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void handleNetworkException_bodyHasCorrectTitle() {
        when(currencyService.fetchRates())
                .thenThrow(new ResourceAccessException("Connection refused"));

        restTestClient.get().uri("/api/rates")
                .exchange()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Upstream Unreachable");
    }

    @Test
    void handleNetworkException_bodyHasCorrectDetail() {
        when(currencyService.fetchRates())
                .thenThrow(new ResourceAccessException("Connection refused"));

        restTestClient.get().uri("/api/rates")
                .exchange()
                .expectBody()
                .jsonPath("$.detail")
                .isEqualTo("Unable to connect to the currency rates provider. Please retry shortly.");
    }

    @Test
    void handleNetworkException_bodyHasCorrectType() {
        when(currencyService.fetchRates())
                .thenThrow(new ResourceAccessException("Connection refused"));

        restTestClient.get().uri("/api/rates")
                .exchange()
                .expectBody()
                .jsonPath("$.type").isEqualTo("urn:currency-service:network-error");
    }
}
