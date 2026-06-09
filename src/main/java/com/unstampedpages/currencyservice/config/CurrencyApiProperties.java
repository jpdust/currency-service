package com.unstampedpages.currencyservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed configuration for the external currency rates API.
*
 * <p>Production: set {@code CURRENCY_API_KEY} as an environment variable.
 * <p>Local dev: define {@code currency.api.key} in {@code local.properties} (git-ignored).
 */
@ConfigurationProperties(prefix = "currency.api")
public record CurrencyApiProperties(
        String baseUrl,
        String path,
        String key,
        String source,
        int cacheMaxAge
) {}
