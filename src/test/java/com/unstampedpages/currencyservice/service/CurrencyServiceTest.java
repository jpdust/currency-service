package com.unstampedpages.currencyservice.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.unstampedpages.currencyservice.config.CurrencyApiProperties;
import com.unstampedpages.currencyservice.exception.ExternalApiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link CurrencyService}.
 *
 * <p>A {@link WireMockServer} is started on a random port for each test, and a real
 * {@link RestClient} is pointed at it. No Spring context is loaded — this keeps the
 * feedback loop fast while still exercising the actual HTTP round-trip logic.
 */
class CurrencyServiceTest {

    private static final String MOCK_RATES_JSON = """
            {
              "success": true,
              "source": "USD",
              "date": "2024-11-15",
              "rates": {
                "EUR": 0.9235,
                "GBP": 0.7885,
                "JPY": 154.32,
                "CAD": 1.3956
              }
            }
            """;

    private WireMockServer wireMockServer;
    private CurrencyService currencyService;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();

        var props = new CurrencyApiProperties(
                wireMockServer.baseUrl(),   // base-url → WireMock
                "/api/v1/rates",            // path
                "test-api-key",             // key
                "USD",                      // source
                300                         // cache-max-age (not used in service layer)
        );

        RestClient restClient = RestClient.builder()
                .baseUrl(props.baseUrl())
                .build();

        currencyService = new CurrencyService(restClient, props);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void fetchRates_returnsDeserializedResponse() {
        wireMockServer.stubFor(
                get(urlPathEqualTo("/api/v1/rates"))
                        .withQueryParam("source", equalTo("USD"))
                        .withQueryParam("apikey", equalTo("test-api-key"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(MOCK_RATES_JSON))
        );

        var result = currencyService.fetchRates();

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.source()).isEqualTo("USD");
        assertThat(result.date()).isEqualTo("2024-11-15");
        assertThat(result.rates()).containsKey("EUR");
        assertThat(result.rates().get("EUR")).isEqualByComparingTo("0.9235");
    }

    @Test
    void fetchRates_appendsApiKeyAndSourceAsQueryParams() {
        wireMockServer.stubFor(
                get(urlPathEqualTo("/api/v1/rates"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(MOCK_RATES_JSON))
        );

        currencyService.fetchRates();

        wireMockServer.verify(getRequestedFor(urlPathEqualTo("/api/v1/rates"))
                .withQueryParam("source", equalTo("USD"))
                .withQueryParam("apikey", equalTo("test-api-key")));
    }

    @Test
    void fetchRates_throwsExternalApiExceptionOn5xx() {
        wireMockServer.stubFor(
                get(urlPathEqualTo("/api/v1/rates"))
                        .willReturn(aResponse().withStatus(500))
        );

        assertThatThrownBy(() -> currencyService.fetchRates())
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("500");
    }

    @Test
    void fetchRates_throwsExternalApiExceptionOn4xx() {
        wireMockServer.stubFor(
                get(urlPathEqualTo("/api/v1/rates"))
                        .willReturn(aResponse().withStatus(401))
        );

        assertThatThrownBy(() -> currencyService.fetchRates())
                .isInstanceOf(ExternalApiException.class)
                .extracting(t -> ((ExternalApiException) t).getStatusCode())
                .isEqualTo(401);
    }
}
