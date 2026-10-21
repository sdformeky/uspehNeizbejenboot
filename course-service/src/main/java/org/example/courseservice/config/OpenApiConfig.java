package org.example.courseservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI courseServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("course-service API")
                .version("1.0")
                .description("Каталог курсов онлайн-школы: курсы с категориями, преподавателями и тегами. "
                        + "Через api-gateway доступен по префиксу /api/courses."));
    }
}
