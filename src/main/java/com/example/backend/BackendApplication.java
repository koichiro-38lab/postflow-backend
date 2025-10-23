package com.example.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.example.backend.batch.DemoContentResetScheduler;
import com.example.backend.config.AppProperties;
import com.example.backend.config.DemoResetProperties;
import com.example.backend.config.MediaStorageProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({ MediaStorageProperties.class, AppProperties.class, DemoResetProperties.class })
public class BackendApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(BackendApplication.class, args);

        // CLI引数チェック: --demo-reset-full
        if (args.length > 0 && "--demo-reset-full".equals(args[0])) {
            try {
                DemoContentResetScheduler scheduler = context.getBean(DemoContentResetScheduler.class);
                scheduler.executeFullSeed();
                System.exit(0);
            } catch (Exception e) {
                System.err.println("Failed to execute full seed: " + e.getMessage());
                System.exit(1);
            }
        }
    }

}
