package com.unstampedpages.currencyservice.service;

import com.unstampedpages.currencyservice.config.CurrencyApiProperties;
import com.unstampedpages.currencyservice.exception.ExternalApiException;
import com.unstampedpages.currencyservice.model.CurrencyRatesResponse;
import com.unstampedpages.currencyservice.model.UpstreamRate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.stream.Collectors;

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
     * <p>The {@code source} query parameter is appended to the configured base URL path.
     * The API key is sent via the {@code Authorization: Bearer} header and is never logged.
     *
     * @return parsed {@link CurrencyRatesResponse}
     * @throws ExternalApiException    if the upstream API responds with an HTTP error
     * @throws org.springframework.web.client.ResourceAccessException if the network is unreachable
     */
    public CurrencyRatesResponse fetchRates() {
        log.info("Fetching currency rates from upstream API (source={})", props.source());

        List<UpstreamRate> upstream = restClient.get()
                .uri(uri -> uri
                        .path(props.path())
                        .queryParam("source", props.source())
                        .build())
                .header("Authorization", "Bearer " + props.key())
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    int status = response.getStatusCode().value();
                    throw new ExternalApiException(
                            "Upstream currency API returned HTTP " + status, status);
                })
                .body(new ParameterizedTypeReference<>() {});

        String source = upstream.isEmpty() ? props.source() : upstream.getFirst().source();
        String date = upstream.isEmpty() ? null : upstream.getFirst().time();
        var rates = upstream.stream()
                .collect(Collectors.toMap(UpstreamRate::target, UpstreamRate::rate));

        return new CurrencyRatesResponse(true, source, date, rates);
    }
}
