package com.unstampedpages.currencyservice.controller;

import com.unstampedpages.currencyservice.model.CurrencyRatesResponse;
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

    private final CurrencyService currencyService;
    private final String cacheControlValue;

    public CurrencyController(CurrencyService currencyService,
                               @Value("${currency.api.cache-max-age:300}") int cacheMaxAge) {
        this.currencyService = currencyService;
        // Pre-build the header value once; it never changes at runtime.
        this.cacheControlValue = "public, max-age=" + cacheMaxAge;
    }

    /**
     * Returns live exchange rates with a {@code Cache-Control} header that
     * instructs CloudFront (and any intermediate proxy) to cache the response
     * for {@code currency.api.cache-max-age} seconds (default 300).
     */
    @GetMapping("/rates")
    public ResponseEntity<CurrencyRatesResponse> getRates() {
        CurrencyRatesResponse rates = currencyService.fetchRates();
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, cacheControlValue)
                .body(rates);
    }
}
