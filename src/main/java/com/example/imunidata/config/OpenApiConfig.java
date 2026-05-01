package com.example.imunidata.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Monitoramento de Vacinação - Imunidata")
                        .version("1.0")
                        .description("Sistema Full Stack de monitoramento de vacinação com dados reais do OpenDataSUS")
                        .contact(new Contact()
                                .name("Equipe Imunidata")
                                .email("contato@imunidata.com")));
    }
}

