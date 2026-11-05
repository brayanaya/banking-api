package com.trinity.banking.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Reloj inyectable: permite fijar la hora en los tests y centraliza la zona horaria. */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${banking.timezone:America/Bogota}") String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }
}
