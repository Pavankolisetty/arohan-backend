package com.arohan.reflection;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ReflectionDtos {
    private ReflectionDtos() {}

    public record ReflectionRequest(
        @NotNull ReflectionType entryType,
        @Size(max = 120) String title,
        @Size(max = 4000) String content,
        @NotNull LocalDate entryDate,
        UUID lifeAreaId,
        UUID habitId,
        @Min(1) @Max(5) Integer moodScore,
        @Min(1) @Max(5) Integer energyScore,
        boolean pinned,
        LocalDate periodStart,
        LocalDate periodEnd,
        @Size(max = 1000) String wins,
        @Size(max = 1000) String friction,
        @Size(max = 700) String nextAdjustment,
        @Size(max = 500) String smallCommitment,
        Set<UUID> tagIds
    ) {}

    public record TagRequest(
        @NotBlank @Size(max = 40) String name,
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String colorHex
    ) {}

    public record TagResponse(UUID id, String name, String colorHex) {}

    public record ReflectionResponse(
        UUID id,
        ReflectionType entryType,
        String title,
        String content,
        LocalDate entryDate,
        UUID lifeAreaId,
        String lifeAreaName,
        UUID habitId,
        String habitName,
        Integer moodScore,
        Integer energyScore,
        boolean pinned,
        LocalDate periodStart,
        LocalDate periodEnd,
        String wins,
        String friction,
        String nextAdjustment,
        String smallCommitment,
        List<TagResponse> tags,
        Instant createdAt,
        Instant updatedAt
    ) {}
}
