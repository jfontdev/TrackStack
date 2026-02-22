package com.jfontdev.trackstack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The main entry point for the Trackstack application.
 * <p>
 * This class bootstraps the Spring Boot application. It is annotated with
 * {@link SpringBootApplication} which enables auto-configuration, component
 * scanning,
 * and configuration properties.
 */
@SpringBootApplication
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
