package com.arohan.habit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

public final class HabitDtos {
    private HabitDtos() {}

    public record ScheduleRequest(
        @NotNull ScheduleType type,
        @NotNull LocalDate startDate,
        Set<DayOfWeek> weekdays,
        Integer intervalDays,
        Integer targetCount,
        LocalDate dueDate,
        @Size(max = 500) String customDescription
    ) {}

    public record UpsertHabitRequest(
        @NotNull HabitKind kind,
        @NotNull UUID lifeAreaId,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 500) String purpose,
        @NotNull TrackingMethod trackingMethod,
        @DecimalMin(value = "0.0001") BigDecimal targetValue,
        @Size(max = 40) String targetUnit,
        @NotBlank @Size(max = 300) String cueNote,
        @NotBlank @Size(max = 300) String twoMinuteStarter,
        LocalTime preferredTime,
        @Size(max = 160) String preferredPlace,
        @Size(max = 200) String precedingActivity,
        @Size(max = 200) String situation,
        @NotBlank @Size(max = 400) String fallbackPlan,
        int positionIndex,
        @NotNull @Valid ScheduleRequest schedule
    ) {}

    public record ScheduleResponse(
        ScheduleType type,
        LocalDate startDate,
        Set<DayOfWeek> weekdays,
        Integer intervalDays,
        Integer targetCount,
        LocalDate dueDate,
        String customDescription
    ) {}

    public record HabitResponse(
        UUID id,
        UUID lifeAreaId,
        String lifeAreaName,
        String lifeAreaColor,
        HabitKind kind,
        String name,
        String purpose,
        TrackingMethod trackingMethod,
        BigDecimal targetValue,
        String targetUnit,
        String cueNote,
        String twoMinuteStarter,
        LocalTime preferredTime,
        String preferredPlace,
        String precedingActivity,
        String situation,
        String fallbackPlan,
        HabitStatus status,
        int positionIndex,
        ScheduleResponse schedule,
        Instant pausedAt,
        Instant archivedAt,
        Instant createdAt,
        Instant updatedAt
    ) {}
}
