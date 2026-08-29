package com.receitasquesabem.common;

import java.time.Instant;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Único endpoint da Fase 0. Confirma três coisas de uma vez:
 * a API arrancou, a base de dados responde, e o Flyway já criou o schema.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final JdbcTemplate jdbc;

    public HealthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record HealthResponse(String status, String database, String app, String timestamp) {
    }

    @GetMapping
    public HealthResponse health() {
        String app;
        String database;
        try {
            app = jdbc.queryForObject("SELECT name FROM app_info ORDER BY id LIMIT 1", String.class);
            database = "ok";
        } catch (Exception e) {
            app = null;
            database = "indisponível: " + e.getClass().getSimpleName();
        }
        return new HealthResponse("ok", database, app, Instant.now().toString());
    }
}
