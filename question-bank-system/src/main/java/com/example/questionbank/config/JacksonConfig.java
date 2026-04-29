package com.example.questionbank.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer localDateTimeCustomizer() {
        DateTimeFormatter flexibleDateTimeFormatter = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd")
                .optionalStart().appendLiteral(' ').optionalEnd()
                .optionalStart().appendLiteral('T').optionalEnd()
                .appendPattern("HH:mm:ss")
                .optionalStart().appendPattern(".SSS").optionalEnd()
                .toFormatter();

        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return builder -> builder
                .serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(outputFormatter))
                .deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(flexibleDateTimeFormatter));
    }
}
