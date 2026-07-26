package com.arohan.tracking;

import com.arohan.habit.ScheduleType;
import com.arohan.habit.TrackingMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public final class TrackingDtos {
    private TrackingDtos() {}

    public record RecordPracticeRequest(
        @NotNull TrackingStatus status,
        @DecimalMin("0.0") BigDecimal actualValue,
        @Min(1) @Max(5) Integer qualityRating,
        @Size(max = 1000) String reflection,
        @Size(max = 300) String frictionNote
    ) {}

    public record TrackingEntryResponse(
        UUID id,
        UUID habitId,
        LocalDate scheduledDate,
        TrackingStatus status,
        BigDecimal actualValue,
        Integer qualityRating,
        String reflection,
        String frictionNote,
        Instant cueStartedAt,
        Instant completedAt
    ) {}

    public record TodayHabitResponse(
        UUID habitId,
        UUID lifeAreaId,
        String lifeAreaName,
        String lifeAreaColor,
        String name,
        String purpose,
        String cueNote,
        String twoMinuteStarter,
        LocalTime preferredTime,
        String preferredPlace,
        TrackingMethod trackingMethod,
        BigDecimal targetValue,
        String targetUnit,
        ScheduleType scheduleType,
        String rhythmLabel,
        String opportunityType,
        TrackingEntryResponse entry
    ) {}

    public record TodayResponse(
        LocalDate date,
        String timeZone,
        long completedCount,
        long partialCount,
        long remainingCount,
        java.util.List<TodayHabitResponse> habits
    ) {}
}
