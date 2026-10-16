package ru.job4j.socialmediaapi.system.internal.infrastructure.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Service endpoint to check that the application is up and answers HTTP requests.
 * Does not touch the database or other modules; not part of the OpenAPI contract.
 */
@RestController
@RequestMapping("/api")
public class PingController {

    /**
     * {@code GET /api/ping}.
     *
     * @return {@code 200} with {@code {"status": "ok", "service": "social-media-api"}}
     */
    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("status", "ok", "service", "social-media-api");
    }
}

