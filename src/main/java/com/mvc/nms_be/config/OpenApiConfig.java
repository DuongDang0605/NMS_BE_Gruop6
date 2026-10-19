package com.mvc.nms_be.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI nmsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("News Management System API")
                        .version("v1")
                        .description("Login with a database account, then paste the JWT into Authorize. "
                                + "API permissions follow the account role: 1 = Admin, 2 = Staff."))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Paste the token returned by POST /api/v1/auth/login, without the Bearer prefix.")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
