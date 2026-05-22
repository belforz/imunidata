package com.example.imunidata.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableScheduling
public class HealthCheckScheduler {
    private static final Logger logger = LoggerFactory.getLogger(HealthCheckScheduler.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private Environment env;

    @Scheduled(fixedRate = 60000) // 60 segundos
    public void keepAlive() {
        String port = System.getenv("PORT");
        if (port == null || port.isBlank()) {
            port = env.getProperty("server.port", "8080");
        }
        // use the actual health endpoint path present in the application
        String url = "http://localhost:" + port + "/api/v1/healthz";
        try {
            String response = restTemplate.getForObject(url, String.class);
            logger.info("Health check ({}): {}", url, response);
        } catch (Exception e) {
            logger.warn("Health check failed ({}): {}", url, e.getMessage());
        }
    }
}
