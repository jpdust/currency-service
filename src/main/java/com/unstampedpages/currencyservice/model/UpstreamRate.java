package com.unstampedpages.currencyservice.model;

import java.math.BigDecimal;

/**
 * Single entry in the array returned by allratestoday.com.
 * Example: {"rate":1.08,"source":"USD","target":"EUR","time":"2026-06-09T04:33:00+0000"}
 */
public record UpstreamRate(
        BigDecimal rate,
        String source,
        String target,
        String time
) {}
