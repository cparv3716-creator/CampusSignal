package edu.campussignal.config;

import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class JsonConfiguration {
    @Bean
    Jackson2ObjectMapperBuilderCustomizer requireActualStrings() {
        // Jackson's general scalar-coercion switch does not cover numeric/boolean-to-string conversion.
        return builder -> builder.postConfigurer(mapper -> {
            var strings = mapper.coercionConfigFor(LogicalType.Textual);
            strings.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail);
            strings.setCoercion(CoercionInputShape.Float, CoercionAction.Fail);
            strings.setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);
        });
    }
}
