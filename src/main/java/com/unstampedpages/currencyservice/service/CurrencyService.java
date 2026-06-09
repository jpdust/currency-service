package com.unstampedpages.currencyservice.service;

import com.unstampedpages.currencyservice.config.CurrencyApiProperties;
import com.unstampedpages.currencyservice.exception.ExternalApiException;
import com.unstampedpages.currencyservice.model.CurrencyRatesResponse;
import com.unstampedpages.currencyservice.model.RatesFetchResult;
import com.unstampedpages.currencyservice.model.UpstreamRate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class CurrencyService {

    private static final Logger log = LoggerFactory.getLogger(CurrencyService.class);

    private final RestClient restClient;
    private final CurrencyApiProperties props;
    private final RatesCache cache;

    public CurrencyService(RestClient currencyRestClient, CurrencyApiProperties props, RatesCache cache) {
        this.restClient = currencyRestClient;
        this.props = props;
        this.cache = cache;
    }

    /**
     * Fetches live exchange rates from the upstream provider.
     *
     * <p>The {@code source} query parameter is appended to the configured base URL path.
     * The API key is sent via the {@code Authorization: Bearer} header and is never logged.
     *
     * <p>On a successful response the result is stored in the in-memory cache.
     * If the upstream is unreachable or returns an error, the last cached response
     * is returned with {@link RatesFetchResult#stale()} set to {@code true}.
     * If the cache is also empty the exception is rethrown.
     *
     * @return {@link RatesFetchResult} containing the rates and a staleness flag
     * @throws ExternalApiException    if the upstream returns an HTTP error and the cache is empty
     * @throws ResourceAccessException if the network is unreachable and the cache is empty
     */
    public RatesFetchResult fetchRates() {
        log.info("Fetching currency rates from upstream API (source={})", props.source());

        try {
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
                    .collect(Collectors.toMap(UpstreamRate::target, UpstreamRate::rate,
                            (a, b) -> a, TreeMap::new));

            var response = new CurrencyRatesResponse(true, source, date, rates);
            cache.store(response);
            return new RatesFetchResult(response, false);

        } catch (ExternalApiException | ResourceAccessException ex) {
            return cache.get()
                    .map(cached -> {
                        log.warn("Upstream unavailable ({}); serving stale cached rates", ex.getMessage());
                        return new RatesFetchResult(cached, true);
                    })
                    .orElseThrow(() -> ex);
        }
    }
}
