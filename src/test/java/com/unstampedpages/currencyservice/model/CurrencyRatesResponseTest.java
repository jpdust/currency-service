package com.unstampedpages.currencyservice.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CurrencyRatesResponseTest {

    @Test
    void constructor_setsAllFields() {
        var rates = Map.of("EUR", new BigDecimal("0.9235"), "GBP", new BigDecimal("0.7885"));
        var response = new CurrencyRatesResponse(true, "USD", "2026-06-09T04:33:00+0000", rates);

        assertThat(response.success()).isTrue();
        assertThat(response.source()).isEqualTo("USD");
        assertThat(response.date()).isEqualTo("2026-06-09T04:33:00+0000");
        assertThat(response.rates()).isEqualTo(rates);
    }

    @Test
    void constructor_allowsNullFields() {
        var response = new CurrencyRatesResponse(null, null, null, null);

        assertThat(response.success()).isNull();
        assertThat(response.source()).isNull();
        assertThat(response.date()).isNull();
        assertThat(response.rates()).isNull();
    }

    @Test
    void equality_twoInstancesWithSameValues_areEqual() {
        var rates = Map.of("JPY", new BigDecimal("154.32"));
        var a = new CurrencyRatesResponse(true, "USD", "2026-06-09T04:33:00+0000", rates);
        var b = new CurrencyRatesResponse(true, "USD", "2026-06-09T04:33:00+0000", rates);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void equality_instancesWithDifferentValues_areNotEqual() {
        var a = new CurrencyRatesResponse(true, "USD", "2026-06-09T04:33:00+0000", Map.of());
        var b = new CurrencyRatesResponse(true, "EUR", "2026-06-09T04:33:00+0000", Map.of());

        assertThat(a).isNotEqualTo(b);
    }
}
