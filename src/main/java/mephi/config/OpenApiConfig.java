package mephi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI spracherkennungOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Spracherkennung API")
                        .version("1.0.0")
                        .description("Веб-сервис для распознавания аудиозаписей"));
    }
}