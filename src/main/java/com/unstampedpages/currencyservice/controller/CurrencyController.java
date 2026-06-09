package com.unstampedpages.currencyservice.controller;

import com.unstampedpages.currencyservice.model.CurrencyRatesResponse;
import com.unstampedpages.currencyservice.model.RatesFetchResult;
import com.unstampedpages.currencyservice.service.CurrencyService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CurrencyController {

    public static final String HEADER_RATES_STALE = "X-Rates-Stale";

    private final CurrencyService currencyService;
    private final String cacheControlValue;

    public CurrencyController(CurrencyService currencyService,
                               @Value("${currency.api.cache-max-age:300}") int cacheMaxAge,
                               @Value("${currency.api.stale-if-error:86400}") int staleIfError) {
        this.currencyService = currencyService;
        // Pre-build the header value once; it never changes at runtime.
        // stale-if-error instructs CloudFront to serve its own cached response for up to
        // staleIfError seconds when the origin returns an error.
        this.cacheControlValue = "public, max-age=" + cacheMaxAge + ", stale-if-error=" + staleIfError;
    }

    /**
     * Returns live exchange rates.
     *
     * <p>On success: {@code Cache-Control: public, max-age=300, stale-if-error=86400}
     * instructs CloudFront to cache fresh responses for 5 minutes and to serve its
     * cached copy for up to 24 hours if the origin becomes unavailable.
     *
     * <p>On upstream failure with a populated in-memory cache: returns the last known
     * rates with {@code X-Rates-Stale: true} so clients can display an appropriate warning.
     */
    @GetMapping("/rates")
    public ResponseEntity<CurrencyRatesResponse> getRates() {
        RatesFetchResult result = currencyService.fetchRates();

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, cacheControlValue);

        if (result.stale()) {
            builder.header(HEADER_RATES_STALE, "true");
        }

        return builder.body(result.rates());
    }
}
