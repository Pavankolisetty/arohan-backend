package com.arohan.lifearea;

import com.arohan.lifearea.LifeAreaDtos.LifeAreaResponse;
import com.arohan.lifearea.LifeAreaDtos.ThemeSuggestionResponse;
import com.arohan.lifearea.LifeAreaDtos.UpsertLifeAreaRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/v1/life-areas")
public class LifeAreaController {
    private final LifeAreaService service;

    public LifeAreaController(LifeAreaService service) {
        this.service = service;
    }

    @GetMapping
    List<LifeAreaResponse> list(@AuthenticationPrincipal Jwt jwt,
                                @RequestParam(defaultValue = "false") boolean includeArchived) {
        return service.list(userId(jwt), includeArchived);
    }

    @GetMapping("/{id}")
    LifeAreaResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.get(userId(jwt), id);
    }

    @GetMapping("/theme-suggestion")
    ThemeSuggestionResponse suggestion(@RequestParam String name) {
        return service.suggest(name);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    LifeAreaResponse create(@AuthenticationPrincipal Jwt jwt,
                            @Valid @RequestBody UpsertLifeAreaRequest request) {
        return service.create(userId(jwt), request);
    }

    @PutMapping("/{id}")
    LifeAreaResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                            @Valid @RequestBody UpsertLifeAreaRequest request) {
        return service.update(userId(jwt), id, request);
    }

    @PatchMapping("/{id}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void archive(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        service.archive(userId(jwt), id);
    }

    @PatchMapping("/{id}/restore")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void restore(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        service.restore(userId(jwt), id);
    }

    @PostMapping("/from-starters")
    List<LifeAreaResponse> fromStarters(@AuthenticationPrincipal Jwt jwt) {
        return service.createFromStarters(userId(jwt));
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}

