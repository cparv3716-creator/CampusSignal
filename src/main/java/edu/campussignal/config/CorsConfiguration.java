package edu.campussignal.config;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
@Configuration
public class CorsConfiguration {
    @Bean CorsFilter corsFilter(@Value("${campussignal.allowed-origin:http://127.0.0.1:5174}") String origin) {
        var rules = new org.springframework.web.cors.CorsConfiguration(); rules.setAllowedOrigins(List.of(origin)); rules.setAllowCredentials(true);
        rules.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "OPTIONS")); rules.setAllowedHeaders(List.of("Content-Type", "X-CampusSignal-CSRF"));
        var source = new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/api/**", rules); return new CorsFilter(source);
    }
}
