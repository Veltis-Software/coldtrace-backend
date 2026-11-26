package com.acme.coldtrace.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@org.springframework.scheduling.annotation.EnableScheduling
@EnableJpaAuditing
@SpringBootApplication
public class ColdtracePlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(ColdtracePlatformApplication.class, args);
    }

}
