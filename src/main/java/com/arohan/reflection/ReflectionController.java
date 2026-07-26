package com.arohan.reflection;

import static com.arohan.reflection.ReflectionDtos.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reflections")
public class ReflectionController {
    private final ReflectionService service;
    public ReflectionController(ReflectionService service) { this.service = service; }

    @GetMapping
    List<ReflectionResponse> list(@AuthenticationPrincipal Jwt jwt,
        @RequestParam(required = false) LocalDate from,
        @RequestParam(required = false) LocalDate to,
        @RequestParam(required = false) String query,
        @RequestParam(required = false) ReflectionType type,
        @RequestParam(required = false) UUID lifeAreaId,
        @RequestParam(required = false) UUID habitId,
        @RequestParam(required = false) UUID tagId,
        @RequestParam(required = false) Boolean pinned) {
        return service.list(userId(jwt), from, to, query, type, lifeAreaId, habitId, tagId, pinned);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ReflectionResponse create(@AuthenticationPrincipal Jwt jwt,
                              @Valid @RequestBody ReflectionRequest request) {
        return service.create(userId(jwt), request);
    }

    @PutMapping("/{id}")
    ReflectionResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                              @Valid @RequestBody ReflectionRequest request) {
        return service.update(userId(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        service.delete(userId(jwt), id);
    }

    @GetMapping("/tags")
    List<TagResponse> tags(@AuthenticationPrincipal Jwt jwt) {
        return service.listTags(userId(jwt));
    }

    @PostMapping("/tags")
    @ResponseStatus(HttpStatus.CREATED)
    TagResponse createTag(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TagRequest request) {
        return service.createTag(userId(jwt), request);
    }

    @DeleteMapping("/tags/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteTag(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        service.deleteTag(userId(jwt), id);
    }

    private UUID userId(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
}
