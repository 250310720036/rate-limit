package com.example.ratelimit;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class RateLimitController {

    private final RateLimiterService rateLimiterService;

    public RateLimitController(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @GetMapping("/access")
    public ResponseEntity<String> checkAccess(@RequestParam(defaultValue = "user1") String key) {
        // Allows max 5 requests, refilling at 1 token/sec
        boolean allowed = rateLimiterService.isAllowed(key, 5, 1);
        
        if (allowed) {
            return ResponseEntity.ok("Access granted!");
        } else {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Rate limit exceeded. Please try again later.");
        }
    }
}
