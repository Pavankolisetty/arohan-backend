package com.arohan.lifearea;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class LifeAreaDtos {
    private LifeAreaDtos() {}

    public record UpsertLifeAreaRequest(
        UUID parentId,
        @NotBlank @Size(max = 80) String name,
        @Size(max = 500) String description,
        @Pattern(regexp = "^$|^#[0-9A-Fa-f]{6}$") String colorHex,
        @Size(max = 40) String iconKey,
        @Size(max = 40) String backgroundKey,
        @Size(max = 1000) String backgroundImageUrl,
        @Min(1) @Max(5) int desiredImportance,
        @Min(0) int positionIndex
    ) {}

    public record LifeAreaResponse(
        UUID id,
        UUID parentId,
        String name,
        String description,
        String colorHex,
        String iconKey,
        String backgroundKey,
        String backgroundImageUrl,
        int desiredImportance,
        LifeAreaStatus status,
        int positionIndex,
        long habitCount,
        List<LifeAreaResponse> subareas,
        Instant createdAt,
        Instant updatedAt
    ) {}

    public record ThemeSuggestionResponse(
        String colorHex,
        String iconKey,
        String backgroundKey
    ) {}
}
