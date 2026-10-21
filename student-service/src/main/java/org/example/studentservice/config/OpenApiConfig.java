package org.example.studentservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI studentServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("student-service API")
                .version("1.0")
                .description("Студенты онлайн-школы: профили, учебные группы, статус. "
                        + "Через api-gateway доступен по префиксу /api/students."));
    }
}
