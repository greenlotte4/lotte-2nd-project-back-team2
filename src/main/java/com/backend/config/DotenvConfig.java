package com.backend.config;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.util.Properties;

@Configuration
public class DotenvConfig {


    @Bean
    public static Dotenv dotenv() {
        return Dotenv.configure()
                .directory("./")
                .ignoreIfMissing()
                .load();
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setProperties(convertDotenvToProperties());
        return configurer;
    }

    private static Properties convertDotenvToProperties() {
        Dotenv dotenv = dotenv();
        Properties properties = new Properties();
        dotenv.entries().forEach(entry ->
                properties.setProperty(entry.getKey(), entry.getValue())
        );
        return properties;
    }
}
