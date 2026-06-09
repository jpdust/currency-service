package com.unstampedpages.currencyservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Represents the JSON envelope returned by allratestoday.com.
 * Unknown fields are ignored to remain tolerant of future API additions.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CurrencyRatesResponse(
        Boolean success,
        String source,
        String date,
        Map<String, BigDecimal> rates
) {}
