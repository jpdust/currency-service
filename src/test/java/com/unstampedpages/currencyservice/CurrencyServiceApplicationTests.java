package com.unstampedpages.currencyservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test: verifies the Spring application context loads without errors.
 * The API key is supplied via a test property so the context does not require
 * an environment variable or local.properties to be present in CI.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "currency.api.key=smoke-test-key",
        "currency.api.base-url=https://api.example.com"
})
class CurrencyServiceApplicationTests {

    @Test
    void contextLoads() {
        // Passes if the Spring context starts without throwing
    }
}
