package com.unstampedpages.currencyservice.model;

/**
 * Wraps a {@link CurrencyRatesResponse} with a flag indicating whether the
 * data was served from the stale in-memory cache due to an upstream failure.
 */
public record RatesFetchResult(CurrencyRatesResponse rates, boolean stale) {}
