package com.unstampedpages.currencyservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    /**
     * Creates a {@link RestClient} scoped to the currency rates API base URL.
     * The builder is created directly (Spring Boot 4 does not auto-configure
     * a {@code RestClient.Builder} bean), keeping the HTTP client self-contained.
     */
    @Bean
    public RestClient currencyRestClient(CurrencyApiProperties props) {
        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .build();
    }
}
