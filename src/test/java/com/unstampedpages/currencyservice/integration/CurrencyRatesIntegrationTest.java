package com.unstampedpages.currencyservice.integration;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.unstampedpages.currencyservice.controller.CurrencyController;
import com.unstampedpages.currencyservice.service.RatesCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * Full-stack integration test.
 *
 * <p>A real Spring Boot server starts on a random port; outbound calls to
 * the upstream currency API are intercepted by a WireMock server.
 * {@link RestTestClient#bindToServer()} drives assertions over real HTTP.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CurrencyRatesIntegrationTest {

    private static final String MOCK_RATES_JSON = """
            [
              {"rate": 0.9235, "source": "USD", "target": "EUR", "time": "2026-06-09T04:33:00+0000"},
              {"rate": 0.7885, "source": "USD", "target": "GBP", "time": "2026-06-09T04:33:00+0000"},
              {"rate": 154.32, "source": "USD", "target": "JPY", "time": "2026-06-09T04:33:00+0000"}
            ]
            """;

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    /**
     * Redirect the application's RestClient base URL to WireMock before the
     * Spring context is created. The supplier form ensures WireMock's port is
     * resolved lazily after the server has actually started.
     */
    @DynamicPropertySource
    static void overrideApiProperties(DynamicPropertyRegistry registry) {
        registry.add("currency.api.base-url", wireMock::baseUrl);
        registry.add("currency.api.key", () -> "integration-test-key");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private RatesCache ratesCache;

    private RestTestClient restTestClient;

    @BeforeEach
    void setUp() {
        ratesCache.clear();
        restTestClient = RestTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    void getRates_returnsOkWithRatesBody() {
        wireMock.stubFor(
                get(urlPathEqualTo("/api/v1/rates"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(MOCK_RATES_JSON))
        );

        restTestClient.get().uri("/api/rates")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.source").isEqualTo("USD")
                .jsonPath("$.rates.EUR").isEqualTo(0.9235f);
    }

    @Test
    void getRates_includesCacheControlHeader() {
        wireMock.stubFor(
                get(urlPathEqualTo("/api/v1/rates"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(MOCK_RATES_JSON))
        );

        restTestClient.get().uri("/api/rates")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.CACHE_CONTROL, "public, max-age=300, stale-if-error=86400");
    }

    @Test
    void getRates_returns503WhenUpstreamFailsAndCacheEmpty() {
        wireMock.stubFor(
                get(urlPathEqualTo("/api/v1/rates"))
                        .willReturn(aResponse().withStatus(500))
        );

        restTestClient.get().uri("/api/rates")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void getRates_forwardsSourceAndAuthorizationToUpstream() {
        wireMock.stubFor(
                get(urlPathEqualTo("/api/v1/rates"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(MOCK_RATES_JSON))
        );

        restTestClient.get().uri("/api/rates").exchange();

        wireMock.verify(getRequestedFor(urlPathEqualTo("/api/v1/rates"))
                .withQueryParam("source", equalTo("USD"))
                .withHeader("Authorization", equalTo("Bearer integration-test-key")));
    }

    @Test
    void getRates_returnsStaleRatesWithHeaderWhenUpstreamFailsAndCachePopulated() {
        wireMock.stubFor(
                get(urlPathEqualTo("/api/v1/rates"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(MOCK_RATES_JSON))
        );
        restTestClient.get().uri("/api/rates").exchange(); // populate cache

        wireMock.resetAll();
        wireMock.stubFor(
                get(urlPathEqualTo("/api/v1/rates"))
                        .willReturn(aResponse().withStatus(500))
        );

        restTestClient.get().uri("/api/rates")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(CurrencyController.HEADER_RATES_STALE, "true")
                .expectBody()
                .jsonPath("$.rates.EUR").isEqualTo(0.9235f);
    }
}
