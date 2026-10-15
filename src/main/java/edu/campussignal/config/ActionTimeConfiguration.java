package edu.campussignal.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ActionTimeConfiguration {
    @Bean
    Clock actionClock() {
        return Clock.systemUTC();
    }
}
