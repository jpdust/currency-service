package com.unstampedpages.currencyservice.controller;

import com.unstampedpages.currencyservice.exception.ExternalApiException;
import com.unstampedpages.currencyservice.model.CurrencyRatesResponse;
import com.unstampedpages.currencyservice.model.RatesFetchResult;
import com.unstampedpages.currencyservice.service.CurrencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.Map;

import static org.mockito.Mockito.when;

/**
 * Controller-layer unit test.
 *
 * <p>Spring Boot 4's test slice for MVC is bootstrapped via
 * {@code webEnvironment = MOCK} so there is no running port.
 * {@link RestTestClient} backed by the {@link WebApplicationContext}
 * replaces the legacy {@code MockMvc} DSL.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = {
        "currency.api.key=test-key",
        "currency.api.base-url=https://api.example.com"
})
class CurrencyControllerTest {

    @Autowired
    private WebApplicationContext wac;

    @MockitoBean
    private CurrencyService currencyService;

    private RestTestClient restTestClient;

    private static final CurrencyRatesResponse SAMPLE_RESPONSE = new CurrencyRatesResponse(
            true,
            "USD",
            "2026-06-09T04:33:00+0000",
            Map.of(
                    "EUR", new BigDecimal("0.9235"),
                    "GBP", new BigDecimal("0.7885"),
                    "JPY", new BigDecimal("154.32")
            )
    );

    @BeforeEach
    void setUp() {
        restTestClient = RestTestClient.bindToApplicationContext(wac).build();
    }

    @Test
    void getRates_returns200WithRatesBody() {
        when(currencyService.fetchRates()).thenReturn(new RatesFetchResult(SAMPLE_RESPONSE, false));

        restTestClient.get().uri("/api/rates")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.source").isEqualTo("USD")
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.rates.EUR").isEqualTo(0.9235f);
    }

    @Test
    void getRates_includesCacheControlHeader() {
        when(currencyService.fetchRates()).thenReturn(new RatesFetchResult(SAMPLE_RESPONSE, false));

        restTestClient.get().uri("/api/rates")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.CACHE_CONTROL, "public, max-age=300, stale-if-error=86400");
    }

    @Test
    void getRates_doesNotIncludeStaleHeaderWhenFresh() {
        when(currencyService.fetchRates()).thenReturn(new RatesFetchResult(SAMPLE_RESPONSE, false));

        restTestClient.get().uri("/api/rates")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().doesNotExist(CurrencyController.HEADER_RATES_STALE);
    }

    @Test
    void getRates_includesStaleHeaderWhenServingCachedRates() {
        when(currencyService.fetchRates()).thenReturn(new RatesFetchResult(SAMPLE_RESPONSE, true));

        restTestClient.get().uri("/api/rates")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(CurrencyController.HEADER_RATES_STALE, "true");
    }

    @Test
    void getRates_returns503WhenUpstreamFailsAndCacheEmpty() {
        when(currencyService.fetchRates())
                .thenThrow(new ExternalApiException("Upstream returned HTTP 500", 500));

        restTestClient.get().uri("/api/rates")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Upstream API Error");
    }
}
