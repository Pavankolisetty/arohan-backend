package com.arohan.reflection;

import com.arohan.reflection.GrowthSignalDtos.GrowthSignalResponse;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/growth-signals")
public class GrowthSignalController {
    private final GrowthSignalService service;
    public GrowthSignalController(GrowthSignalService service) { this.service = service; }

    @GetMapping
    GrowthSignalResponse signals(@AuthenticationPrincipal Jwt jwt,
                                 @RequestParam(required = false) LocalDate from,
                                 @RequestParam(required = false) LocalDate to) {
        return service.signals(UUID.fromString(jwt.getSubject()), from, to);
    }
}
