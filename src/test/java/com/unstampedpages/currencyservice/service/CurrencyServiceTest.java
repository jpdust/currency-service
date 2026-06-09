package com.unstampedpages.currencyservice.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.unstampedpages.currencyservice.config.CurrencyApiProperties;
import com.unstampedpages.currencyservice.exception.ExternalApiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
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

    private static final String RATES_ARRAY_JSON = """
            [
              {"rate": 0.9235, "source": "USD", "target": "EUR", "time": "2026-06-09T04:33:00+0000"},
              {"rate": 0.7885, "source": "USD", "target": "GBP", "time": "2026-06-09T04:33:00+0000"},
              {"rate": 154.32, "source": "USD", "target": "JPY", "time": "2026-06-09T04:33:00+0000"}
            ]
            """;

    private static final String EMPTY_ARRAY_JSON = "[]";

    private WireMockServer wireMockServer;
    private CurrencyService currencyService;
    private CurrencyApiProperties props;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();

        props = new CurrencyApiProperties(
                wireMockServer.baseUrl(),
                "/api/v1/rates",
                "test-api-key",
                "USD",
                300
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
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(RATES_ARRAY_JSON))
        );

        var result = currencyService.fetchRates();

        assertThat(result.success()).isTrue();
        assertThat(result.source()).isEqualTo("USD");
        assertThat(result.date()).isEqualTo("2026-06-09T04:33:00+0000");
        assertThat(result.rates()).hasSize(3);
        assertThat(result.rates().get("EUR")).isEqualByComparingTo("0.9235");
        assertThat(result.rates().get("GBP")).isEqualByComparingTo("0.7885");
        assertThat(result.rates().get("JPY")).isEqualByComparingTo("154.32");
    }

    @Test
    void fetchRates_sendsAuthorizationBearerHeader() {
        wireMockServer.stubFor(
                get(urlPathEqualTo("/api/v1/rates"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(RATES_ARRAY_JSON))
        );

        currencyService.fetchRates();

        wireMockServer.verify(getRequestedFor(urlPathEqualTo("/api/v1/rates"))
                .withHeader("Authorization", equalTo("Bearer test-api-key")));
    }

    @Test
    void fetchRates_sendsSourceQueryParam() {
        wireMockServer.stubFor(
                get(urlPathEqualTo("/api/v1/rates"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(RATES_ARRAY_JSON))
        );

        currencyService.fetchRates();

        wireMockServer.verify(getRequestedFor(urlPathEqualTo("/api/v1/rates"))
                .withQueryParam("source", equalTo("USD")));
    }

    @Test
    void fetchRates_doesNotSendApiKeyAsQueryParam() {
        wireMockServer.stubFor(
                get(urlPathEqualTo("/api/v1/rates"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(RATES_ARRAY_JSON))
        );

        currencyService.fetchRates();

        wireMockServer.verify(getRequestedFor(urlPathEqualTo("/api/v1/rates"))
                .withoutQueryParam("apikey"));
    }

    @Test
    void fetchRates_emptyUpstreamList_fallsBackToConfiguredSource() {
        wireMockServer.stubFor(
                get(urlPathEqualTo("/api/v1/rates"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(EMPTY_ARRAY_JSON))
        );

        var result = currencyService.fetchRates();

        assertThat(result.success()).isTrue();
        assertThat(result.source()).isEqualTo("USD");
        assertThat(result.date()).isNull();
        assertThat(result.rates()).isEmpty();
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

    @Test
    void fetchRates_throwsResourceAccessExceptionOnNetworkFailure() {
        wireMockServer.stop();

        assertThatThrownBy(() -> currencyService.fetchRates())
                .isInstanceOf(ResourceAccessException.class);
    }
}
