package com.example.backend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HelloController {

    @GetMapping("/")
    public Map<String, Object> root() {
        return Map.of("status", "ok");
    }

    @GetMapping("/hello")
    public Map<String, Object> hello() {
        return Map.of("message", "hello");
    }
}

