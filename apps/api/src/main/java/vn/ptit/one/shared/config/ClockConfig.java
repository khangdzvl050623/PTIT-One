package vn.ptit.one.shared.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Một nguồn thời gian UTC cho cả ứng dụng; test thay bằng đồng hồ cố định. */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
