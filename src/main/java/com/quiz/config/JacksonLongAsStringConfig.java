package com.quiz.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Serializes {@link Long} as JSON strings so browser {@code JSON.parse} does not lose precision
 * (IDs from CockroachDB often exceed {@code Number.MAX_SAFE_INTEGER}).
 */
@Configuration
public class JacksonLongAsStringConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longIdsAsString() {
        return builder -> builder
                .serializerByType(Long.class, ToStringSerializer.instance)
                .serializerByType(Long.TYPE, ToStringSerializer.instance);
    }
}
