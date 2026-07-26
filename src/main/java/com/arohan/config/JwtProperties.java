package com.arohan.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "arohan.jwt")
public record JwtProperties(String secret, Duration accessTokenTtl, String issuer) {}

