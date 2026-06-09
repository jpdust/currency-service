package com.unstampedpages.currencyservice.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class UpstreamRateTest {

    @Test
    void constructor_setsAllFields() {
        var rate = new UpstreamRate(new BigDecimal("0.9235"), "USD", "EUR", "2026-06-09T04:33:00+0000");

        assertThat(rate.rate()).isEqualByComparingTo("0.9235");
        assertThat(rate.source()).isEqualTo("USD");
        assertThat(rate.target()).isEqualTo("EUR");
        assertThat(rate.time()).isEqualTo("2026-06-09T04:33:00+0000");
    }

    @Test
    void rate_preservesBigDecimalPrecision() {
        var rate = new UpstreamRate(new BigDecimal("1373.123456789"), "USD", "NGN", "2026-06-09T04:33:00+0000");

        assertThat(rate.rate()).isEqualByComparingTo(new BigDecimal("1373.123456789"));
    }

    @Test
    void equality_twoInstancesWithSameValues_areEqual() {
        var a = new UpstreamRate(new BigDecimal("1.08"), "USD", "EUR", "2026-06-09T04:33:00+0000");
        var b = new UpstreamRate(new BigDecimal("1.08"), "USD", "EUR", "2026-06-09T04:33:00+0000");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void equality_instancesWithDifferentTargets_areNotEqual() {
        var a = new UpstreamRate(new BigDecimal("1.08"), "USD", "EUR", "2026-06-09T04:33:00+0000");
        var b = new UpstreamRate(new BigDecimal("1.08"), "USD", "GBP", "2026-06-09T04:33:00+0000");

        assertThat(a).isNotEqualTo(b);
    }
}
