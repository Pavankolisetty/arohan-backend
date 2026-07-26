package com.arohan.habit;

import com.arohan.habit.HabitDtos.HabitResponse;
import com.arohan.habit.HabitDtos.UpsertHabitRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/growth-habits")
public class GrowthHabitController {
    private final GrowthHabitService service;

    public GrowthHabitController(GrowthHabitService service) {
        this.service = service;
    }

    @GetMapping
    List<HabitResponse> list(@AuthenticationPrincipal Jwt jwt,
                             @RequestParam(required = false) HabitStatus status,
                             @RequestParam(required = false) UUID lifeAreaId) {
        return service.list(userId(jwt), status, lifeAreaId);
    }

    @GetMapping("/{id}")
    HabitResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.get(userId(jwt), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    HabitResponse create(@AuthenticationPrincipal Jwt jwt,
                         @Valid @RequestBody UpsertHabitRequest request) {
        return service.create(userId(jwt), request);
    }

    @PutMapping("/{id}")
    HabitResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                         @Valid @RequestBody UpsertHabitRequest request) {
        return service.update(userId(jwt), id, request);
    }

    @PatchMapping("/{id}/pause")
    HabitResponse pause(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.pause(userId(jwt), id);
    }

    @PatchMapping("/{id}/restart")
    HabitResponse restart(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.restart(userId(jwt), id);
    }

    @PatchMapping("/{id}/archive")
    HabitResponse archive(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.archive(userId(jwt), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        service.delete(userId(jwt), id);
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
