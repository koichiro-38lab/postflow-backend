package com.example.backend.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

@TestConfiguration
public class TestClockConfig {
    // 単一インスタンスの可変Clock（中身のdelegateだけ差し替える）
    private static final MutableClock MUTABLE_CLOCK = new MutableClock(
            Clock.systemUTC());

    @Bean
    @Primary
    public Clock testClock() {
        return MUTABLE_CLOCK; // 常に同一インスタンスを返す
    }

    // テストからClockを進めるためのユーティリティ
    public static void setTestClock(Clock clock) {
        MUTABLE_CLOCK.setDelegate(clock);
    }

    public static void setOffsetSeconds(long seconds) {
        Clock base = Clock.systemUTC();
        Clock offset = Clock.offset(base, Duration.ofSeconds(seconds));
        setTestClock(offset);
    }

    public static Clock getTestClock() {
        return MUTABLE_CLOCK;
    }

    // 可変Clock本体
    static final class MutableClock extends Clock {
        private final AtomicReference<Clock> delegate;

        MutableClock(Clock initial) {
            this.delegate = new AtomicReference<>(initial);
        }

        void setDelegate(Clock newDelegate) {
            this.delegate.set(newDelegate);
        }

        @Override
        public ZoneId getZone() {
            return delegate.get().getZone();
        }

        @Override
        public Clock withZone(ZoneId zone) {
            Clock current = delegate.get();
            Clock zoned = current.withZone(zone);
            return new MutableClock(zoned);
        }

        @Override
        public Instant instant() {
            return delegate.get().instant();
        }
    }
}
