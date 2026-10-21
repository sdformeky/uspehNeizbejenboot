package org.example.enrollmentservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Enrollment Service API")
                        .version("1.0.0")
                        .description("Микросервис управления записями студентов на курсы. " +
                                     "Обеспечивает валидацию через внешние сервисы (Student и Course) " +
                                     "и защищен предохранителем Circuit Breaker."));
    }
}
