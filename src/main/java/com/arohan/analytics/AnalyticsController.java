package com.arohan.analytics;

import com.arohan.analytics.AnalyticsDtos.GrowthStudioResponse;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/growth-studio")
public class AnalyticsController {
    private final AnalyticsService service;

    public AnalyticsController(AnalyticsService service) {
        this.service = service;
    }

    @GetMapping
    GrowthStudioResponse studio(@AuthenticationPrincipal Jwt jwt,
                                @RequestParam(required = false) LocalDate from,
                                @RequestParam(required = false) LocalDate to,
                                @RequestParam(required = false) UUID lifeAreaId) {
        return service.studio(UUID.fromString(jwt.getSubject()), from, to, lifeAreaId);
    }
}
