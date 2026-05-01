package com.example.imunidata.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableScheduling
public class HealthCheckScheduler {

    private final RestTemplate restTemplate = new RestTemplate();

    @Scheduled(fixedRate = 60000) // 60 segundos
    public void keepAlive() {
        try {
            String response = restTemplate.getForObject("http://localhost:8080/healthz", String.class);
            System.out.println("Health check: " + response);
        } catch (Exception e) {
            System.err.println("Health check failed: " + e.getMessage());
        }
    }
}
