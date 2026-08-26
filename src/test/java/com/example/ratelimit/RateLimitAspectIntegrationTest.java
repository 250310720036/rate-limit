package com.example.ratelimit;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class RateLimitAspectIntegrationTest {

    @Container
    static RedisContainer redisContainer = new RedisContainer(
            DockerImageName.parse("redis:7.2-alpine")
    );

    @DynamicPropertySource
    static void overrideRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", redisContainer::getFirstMappedPort);
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should allow requests within limit and reject extra requests with HTTP 429")
    void shouldEnforceRateLimiting() throws Exception {
        String endpoint = "/api/sensitive-data";

        // Request 1: Allowed (HTTP 200)
        mockMvc.perform(get(endpoint))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Limit", "1"));

        // Request 2: Exceeds capacity (HTTP 429)
        mockMvc.perform(get(endpoint))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("X-RateLimit-Limit", "1"))
                .andExpect(header().string("Retry-After", "1"));
    }

    @Test
    @DisplayName("Should replenish tokens after waiting refill period")
    void shouldReplenishTokensAfterRefillPeriod() throws Exception {
        String endpoint = "/api/sensitive-data";

        // Consume available token
        mockMvc.perform(get(endpoint))
                .andExpect(status().isOk());

        // Immediately gets rate-limited
        mockMvc.perform(get(endpoint))
                .andExpect(status().isTooManyRequests());

        // Wait 1.1 seconds for the token bucket to refill
        Thread.sleep(1100);

        // Request should now succeed
        mockMvc.perform(get(endpoint))
                .andExpect(status().isOk());
    }
}
