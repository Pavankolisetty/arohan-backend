package com.arohan.reflection;

import java.time.LocalDate;
import java.util.List;

public final class GrowthSignalDtos {
    private GrowthSignalDtos() {}

    public record EvidenceItem(LocalDate date, String label, String detail) {}
    public record GrowthSignal(
        String key,
        String kind,
        String title,
        String summary,
        String evidence,
        String method,
        int sampleSize,
        int minimumSample,
        boolean ready,
        String tone,
        List<EvidenceItem> evidenceItems
    ) {}
    public record GrowthSignalResponse(
        LocalDate from,
        LocalDate to,
        String boundaryNote,
        List<GrowthSignal> signals
    ) {}
}
