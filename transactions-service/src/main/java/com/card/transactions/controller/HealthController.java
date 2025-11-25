package com.card.transactions.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {
//nada solo para hacer primer commit LuisZerafin
    @GetMapping
    public Map<String, Object> healh() {
        return Map.of(
                "status", "UP",
                "service", "transactions-service"
        );
    }
}
