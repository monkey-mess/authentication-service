package ru.balybin.monkey_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/test")
    public ResponseEntity<String> HomeController() {
        return ResponseEntity.ok("Test message");
    }
}
