package com.jfontdev.trackstack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

/**
 * The main entry point for the Trackstack application.
 * <p>
 * This class bootstraps the Spring Boot application. It is annotated with
 * {@link SpringBootApplication} which enables auto-configuration, component
 * scanning,
 * and configuration properties.
 * Additionally, {@link EnableSpringDataWebSupport} is used to configure how
 * Spring Data handles pagination and sorting in web requests, specifying that
 */
@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class TrackstackApplication {

    /**
     * The main method that starts the Spring Boot application.
     *
     * @param args command line arguments passed to the application
     */
    public static void main(String[] args) {
        SpringApplication.run(TrackstackApplication.class, args);
    }

}
