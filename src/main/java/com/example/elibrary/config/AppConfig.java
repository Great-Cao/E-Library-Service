package com.example.elibrary.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AppConfig {

    /** A UTC clock keeps every persisted timestamp unambiguous and testable. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
