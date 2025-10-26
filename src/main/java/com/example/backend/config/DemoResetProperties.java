// filepath: backend/src/main/java/com/example/backend/config/DemoResetProperties.java
package com.example.backend.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.demo-reset")
public class DemoResetProperties {

    private boolean enabled = false;
    private Duration initialDelay = Duration.ofMinutes(1);
    private Duration fixedDelay = Duration.ofMinutes(30);
    private String sampleLocation = "classpath:/demo/sample_image/*.*";
    private String mediaFolder = "media";
    private String contentType = "image/avif";

    // 簡易シード設定
    private boolean minimalSeedOnStartup = true;
    private String minimalSeedScript = "classpath:db/seed/seed_minimal.sql";
    private String fullSeedScript = "classpath:db/seed/seed_full.sql";
}
