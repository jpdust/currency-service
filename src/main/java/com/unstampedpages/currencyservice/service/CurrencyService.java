package com.unstampedpages.currencyservice.service;

import com.unstampedpages.currencyservice.config.CurrencyApiProperties;
import com.unstampedpages.currencyservice.exception.ExternalApiException;
import com.unstampedpages.currencyservice.model.CurrencyRatesResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class CurrencyService {

    private static final Logger log = LoggerFactory.getLogger(CurrencyService.class);

    private final RestClient restClient;
    private final CurrencyApiProperties props;

    public CurrencyService(RestClient currencyRestClient, CurrencyApiProperties props) {
        this.restClient = currencyRestClient;
        this.props = props;
    }

    /**
     * Fetches live exchange rates from the upstream provider.
     *
     * <p>Query parameters ({@code source} and {@code apikey}) are appended to the
     * configured base URL path. The API key is never logged.
     *
     * @return parsed {@link CurrencyRatesResponse}
     * @throws ExternalApiException    if the upstream API responds with an HTTP error
     * @throws org.springframework.web.client.ResourceAccessException if the network is unreachable
     */
    public CurrencyRatesResponse fetchRates() {
        log.info("Fetching currency rates from upstream API (source={})", props.source());

        return restClient.get()
                .uri(uri -> uri
                        .path(props.path())
                        .queryParam("source", props.source())
                        .queryParam("apikey", props.key())
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    int status = response.getStatusCode().value();
                    throw new ExternalApiException(
                            "Upstream currency API returned HTTP " + status, status);
                })
                .body(CurrencyRatesResponse.class);
    }
}
