package com.quiz.ratelimit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;

    @Valid
    private EndpointLimit login = new EndpointLimit(60, 20);

    @Valid
    private EndpointLimit submit = new EndpointLimit(30, 30);

    @Getter
    @Setter
    public static class EndpointLimit {
        @Min(1)
        private int windowSeconds;

        @Min(1)
        private int maxRequests;

        public EndpointLimit() {
        }

        public EndpointLimit(int windowSeconds, int maxRequests) {
            this.windowSeconds = windowSeconds;
            this.maxRequests = maxRequests;
        }
    }
}

