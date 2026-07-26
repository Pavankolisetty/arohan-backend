package com.arohan.tracking;

import com.arohan.tracking.TrackingDtos.RecordPracticeRequest;
import com.arohan.tracking.TrackingDtos.TodayResponse;
import com.arohan.tracking.TrackingDtos.TrackingEntryResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tracking")
public class TrackingController {
    private final TrackingService service;

    public TrackingController(TrackingService service) {
        this.service = service;
    }

    @GetMapping("/today")
    TodayResponse today(@AuthenticationPrincipal Jwt jwt,
                        @RequestParam(required = false) LocalDate date) {
        return service.today(userId(jwt), date);
    }

    @PostMapping("/habits/{habitId}/cue-start")
    TrackingEntryResponse cueStart(@AuthenticationPrincipal Jwt jwt,
                                   @PathVariable UUID habitId,
                                   @RequestParam LocalDate date) {
        return service.cueStart(userId(jwt), habitId, date);
    }

    @PutMapping("/habits/{habitId}/practice")
    TrackingEntryResponse record(@AuthenticationPrincipal Jwt jwt,
                                 @PathVariable UUID habitId,
                                 @RequestParam LocalDate date,
                                 @Valid @RequestBody RecordPracticeRequest request) {
        return service.record(userId(jwt), habitId, date, request);
    }

    @DeleteMapping("/habits/{habitId}/practice")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void clear(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID habitId,
               @RequestParam LocalDate date) {
        service.clear(userId(jwt), habitId, date);
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
