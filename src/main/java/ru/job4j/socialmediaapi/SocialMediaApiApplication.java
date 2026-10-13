package ru.job4j.socialmediaapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SocialMediaApiApplication {

    /**
     * Not private: Spring proxies the {@code @Configuration} class by subclassing it.
     */
    protected SocialMediaApiApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(SocialMediaApiApplication.class, args);
    }

}
