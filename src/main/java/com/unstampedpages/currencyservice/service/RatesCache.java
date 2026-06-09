package com.unstampedpages.currencyservice.service;

import com.unstampedpages.currencyservice.model.CurrencyRatesResponse;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe in-memory cache for the last successfully fetched rates response.
 * Used as a fallback when the upstream API is unavailable.
 */
@Component
public class RatesCache {

    private final AtomicReference<CurrencyRatesResponse> cached = new AtomicReference<>();

    public void store(CurrencyRatesResponse response) {
        cached.set(response);
    }

    public Optional<CurrencyRatesResponse> get() {
        return Optional.ofNullable(cached.get());
    }

    public void clear() {
        cached.set(null);
    }
}
