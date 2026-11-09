package ru.job4j.socialmediaapi;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Application clock of the E2E tests. It does not depend on the system time: every reading
 * is one second later than the previous one, so records created one after another always get
 * growing, distinct {@code created_at} even on a machine with a coarse or non-monotonic
 * system clock (CI containers). The E2E database is used only by the E2E tests, so the records
 * of the latest test are always the newest.
 */
@TestConfiguration(proxyBeanMethods = false)
public class E2EClockConfiguration {

    /** Time of the first reading of the clock. */
    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    /**
     * @return clock that replaces the UTC system clock of {@code shared.config.ClockConfiguration}
     */
    @Bean
    @Primary
    Clock steppingClock() {
        return new SteppingClock(START);
    }

    /** UTC clock independent of the system time: each {@link #instant()} is one second after the previous one. */
    static final class SteppingClock extends Clock {

        /** Distance between two readings. */
        private static final Duration STEP = Duration.ofSeconds(1);

        /** Time of the first reading. */
        private final Instant start;

        /** Number of readings made so far. */
        private final AtomicLong ticks = new AtomicLong();

        /**
         * @param start time of the first reading
         */
        SteppingClock(Instant start) {
            this.start = start;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException("SteppingClock is UTC only");
        }

        @Override
        public Instant instant() {
            return start.plus(STEP.multipliedBy(ticks.getAndIncrement()));
        }
    }
}
