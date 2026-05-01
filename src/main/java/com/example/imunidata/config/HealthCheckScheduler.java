package com.example.imunidata.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

@Configuration
@EnableScheduling
public class HealthCheckScheduler {

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private Environment env;

    @Scheduled(fixedRate = 60000) // 60 segundos
    public void keepAlive() {
        String port = System.getenv("PORT");
        if (port == null || port.isBlank()) {
            port = env.getProperty("server.port", "8080");
        }
        String url = "http://localhost:" + port + "/healthz";
        try {
            String response = restTemplate.getForObject(url, String.class);
            System.out.println("Health check (" + url + "): " + response);
        } catch (Exception e) {
            System.err.println("Health check failed (" + url + "): " + e.getMessage());
        }
    }
}
