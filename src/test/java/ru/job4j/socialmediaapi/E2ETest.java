package ru.job4j.socialmediaapi;

import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * E2E test of the REST API: the full application on a random port, called over real HTTP
 * through {@link org.springframework.boot.resttestclient.TestRestTemplate}, with PostgreSQL
 * in Docker started by Testcontainers ({@link TestcontainersConfiguration}). The application
 * clock is replaced by {@link E2EClockConfiguration}: records get growing {@code created_at}
 * that do not depend on the system time.
 *
 * <p>All E2E test classes share the same configuration, so Spring caches one context
 * and one container for them. The container is shared: each test uses its own
 * {@code UUID.randomUUID()} data or cleans what it changes.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import({TestcontainersConfiguration.class, E2EClockConfiguration.class})
public @interface E2ETest {
}
