package com.arohan.analytics;

import com.arohan.tracking.TrackingStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class AnalyticsDtos {
    private AnalyticsDtos() {}

    public record CountSummary(
        int eligible,
        int completed,
        int partial,
        int skipped,
        int missed,
        Double consistencyPercent
    ) {}

    public record Signal(
        String key,
        String label,
        Double value,
        String unit,
        int sampleSize,
        int minimumSample,
        boolean ready,
        String direction,
        String explanation
    ) {}

    public record LifeAreaPulse(
        UUID lifeAreaId,
        String name,
        String colorHex,
        int desiredImportance,
        int eligible,
        int completed,
        Double consistencyPercent,
        Double desiredFocusShare,
        Double actualAttentionShare,
        String alignment
    ) {}

    public record RhythmRecord(
        UUID habitId,
        String habitName,
        String lifeAreaName,
        TrackingStatus status,
        boolean cueStarted,
        BigDecimal actualValue,
        String targetUnit,
        Integer qualityRating,
        String reflection
    ) {}

    public record RhythmDay(
        LocalDate date,
        int due,
        int completed,
        int partial,
        int skipped,
        int missed,
        int recoveries,
        List<RhythmRecord> records
    ) {}

    public record CueFlow(
        int cueReady,
        int cueStarts,
        int fullPracticesAfterStart,
        int reflections,
        Double completedAfterStartPercent,
        boolean ready
    ) {}

    public record SkillJourney(
        UUID habitId,
        String habitName,
        String lifeAreaName,
        String stage,
        int eligible,
        int completed,
        Double consistencyPercent,
        long ageDays,
        String nextStageHint
    ) {}

    public record ProgressStory(
        String title,
        String evidence,
        String recovery,
        String strongestCue,
        String recurringBarrier,
        String nextExperiment
    ) {}

    public record GrowthStudioResponse(
        LocalDate from,
        LocalDate to,
        String timeZone,
        Instant generatedAt,
        CountSummary counts,
        List<Signal> signals,
        List<LifeAreaPulse> lifeAreas,
        List<RhythmDay> rhythm,
        CueFlow cueFlow,
        List<SkillJourney> skillJourney,
        ProgressStory progressStory
    ) {}
}
