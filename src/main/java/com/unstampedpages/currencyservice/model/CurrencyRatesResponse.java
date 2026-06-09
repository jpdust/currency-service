package com.unstampedpages.currencyservice.model;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Response returned by this service's /api/rates endpoint.
 */
public record CurrencyRatesResponse(
        Boolean success,
        String source,
        String date,
        Map<String, BigDecimal> rates
) {}
