package com.nfu.jasmine.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MySwaggerConfig {
    @Bean
    public OpenAPI api() {
        String securitySchemeName = "BearerAuth";
        return new OpenAPI()
                .info(apiInfo())
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components().addSecuritySchemes(securitySchemeName, securityScheme()));
    }

    private SecurityScheme securityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");
    }

    private Info apiInfo() {
        return new Info()
                .title("Jasmine花店管理系统接口文档")
                .description("一个花店管理系统项目")
                .version("1.0")
                .contact(new Contact()
                        .name("JipZeonGit")
                        .url("https://github.com/JipZeonGit/Jasmine")
                        .email("yejunjie2017@gmail.com"));
    }
}
