package com.farmacia.farmacia.controller;

import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    private final MongoTemplate mongo;
    private volatile boolean pronto;
    public HealthController(MongoTemplate mongo) { this.mongo = mongo; }

    @EventListener(ApplicationReadyEvent.class)
    public void pronto() { pronto = true; }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> verificar() {
        try {
            if (!pronto) return ResponseEntity.status(503).body(Map.of("status", "inicializando"));
            mongo.executeCommand(new Document("ping", 1));
            return ResponseEntity.ok(Map.of("status", "ok", "banco", "mongodb"));
        } catch (DataAccessException | com.mongodb.MongoException ex) {
            return ResponseEntity.status(503).body(Map.of("status", "indisponivel"));
        }
    }
}
