package com.receitasquesabem.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * O browser bloqueia pedidos de um site para outra origem, a menos que
 * o servidor de destino diga explicitamente que confia nessa origem.
 * Como o frontend e a API vivem em domínios diferentes, é isto que
 * autoriza o Cloudflare Pages (e o localhost:5173) a falar com a API.
 *
 * allowCredentials(true) é necessário para os cookies de sessão da Fase 1.
 * Com ele, a especificação proíbe usar "*" nas origens — daí a lista explícita.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public CorsConfig(@Value("${app.cors.allowed-origins}") String allowedOrigins) {
        this.allowedOrigins = allowedOrigins.split(",");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
