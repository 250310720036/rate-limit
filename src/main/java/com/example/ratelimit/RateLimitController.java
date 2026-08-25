package com.example.ratelimit;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class RateLimitController {

    @GetMapping("/sensitive-data")
    @RateLimited(capacity = 3, refillRate = 1, keyPrefix = "sensitive")
    public ResponseEntity<String> getProtectedData() {
        return ResponseEntity.ok("Access granted to sensitive data!");
    }
}