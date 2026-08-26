package com.example.ratelimit;
import org.springframework.http.HttpHeaders;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class RateLimitAspect {

    private final RateLimiterService rateLimiterService;

    public RateLimitAspect(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Around("@annotation(rateLimited)")
    public Object enforceRateLimit(ProceedingJoinPoint joinPoint, RateLimited rateLimited) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        
        String clientIp = getClientIp(request);
        String rateKey = rateLimited.keyPrefix() + ":" + clientIp;

        boolean allowed = rateLimiterService.isAllowed(rateKey, rateLimited.capacity(), rateLimited.refillRate());

        if (!allowed) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("X-RateLimit-Limit", String.valueOf(rateLimited.capacity()))
                    .header("Retry-After", "1")
                    .body("Rate limit exceeded for IP: " + clientIp);
        }

        Object result = joinPoint.proceed();

        // Attach rate-limit metadata headers to successful response
        if (result instanceof ResponseEntity<?> responseEntity) {
            HttpHeaders headers = HttpHeaders.writableHttpHeaders(responseEntity.getHeaders());
            headers.add("X-RateLimit-Limit", String.valueOf(rateLimited.capacity()));
            return new ResponseEntity<>(responseEntity.getBody(), headers, responseEntity.getStatusCode());
        }

        return result;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
