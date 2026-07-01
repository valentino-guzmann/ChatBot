package com.chatbotmvt.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:src/main/resources/static/uploads}")
    private String uploadDir;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("https://elegant-creativity-production-5ec0.up.railway.app")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Resolver la ruta: si es relativa, convertir a absoluta desde el directorio de trabajo
        java.nio.file.Path uploadPath = Paths.get(uploadDir);
        if (!uploadPath.isAbsolute()) {
            uploadPath = Paths.get(System.getProperty("user.dir"), uploadDir).toAbsolutePath();
        }
        String resolvedPath = uploadPath.toString();
        System.out.println("📁 Configurando ResourceHandler: /uploads/** -> file:" + resolvedPath + "/");
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + resolvedPath + "/");
    }
}
