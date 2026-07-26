package com.arohan.reflection;

import static com.arohan.reflection.GrowthSignalDtos.*;

import com.arohan.analytics.AnalyticsDtos.LifeAreaPulse;
import com.arohan.analytics.AnalyticsService;
import com.arohan.shared.ApiException;
import com.arohan.tracking.HabitTrackingEntry;
import com.arohan.tracking.HabitTrackingRepository;
import com.arohan.tracking.TrackingStatus;
import com.arohan.user.User;
import com.arohan.user.UserRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GrowthSignalService {
    private final HabitTrackingRepository tracking;
    private final JournalEntryRepository reflections;
    private final JournalTagRepository tags;
    private final UserRepository users;
    private final AnalyticsService analytics;

    public GrowthSignalService(HabitTrackingRepository tracking, JournalEntryRepository reflections,
                               JournalTagRepository tags, UserRepository users,
                               AnalyticsService analytics) {
        this.tracking = tracking;
        this.reflections = reflections;
        this.tags = tags;
        this.users = users;
        this.analytics = analytics;
    }

    @Transactional(readOnly = true)
    public GrowthSignalResponse signals(UUID userId, LocalDate from, LocalDate to) {
        User user = users.findById(userId).orElseThrow();
        ZoneId zone = ZoneId.of(user.getTimeZone());
        LocalDate safeTo = to == null ? LocalDate.now(zone) : to;
        LocalDate safeFrom = from == null ? safeTo.minusDays(29) : from;
        if (safeFrom.isAfter(safeTo)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The beginning date must be before the ending date.");
        }
        if (safeFrom.isBefore(safeTo.minusDays(365))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a signal window of one year or less.");
        }
        List<HabitTrackingEntry> moments =
            tracking.findAllByUserIdAndScheduledDateBetweenOrderByScheduledDateAsc(userId, safeFrom, safeTo);
        List<JournalEntry> notes =
            reflections.findAllByUserIdAndEntryDateBetweenOrderByPinnedDescEntryDateDescCreatedAtDesc(
                userId, safeFrom, safeTo);
        List<GrowthSignal> result = new ArrayList<>();
        result.add(commonTime(moments, zone));
        result.add(commonBarrier(moments));
        result.add(improvingArea(userId, safeFrom, safeTo));
        result.add(reflectionTheme(userId, notes));
        result.add(energyAssociation(moments, notes));
        return new GrowthSignalResponse(safeFrom, safeTo,
            "Growth Signals describe patterns in your own records. They are supportive observations—not diagnoses or proof of cause.",
            result);
    }

    private GrowthSignal improvingArea(UUID userId, LocalDate from, LocalDate to) {
        long days = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1;
        int half = Math.max(1, (int) days / 2);
        LocalDate currentFrom = to.minusDays(half - 1L);
        LocalDate previousTo = currentFrom.minusDays(1);
        LocalDate previousFrom = previousTo.minusDays(half - 1L);
        List<LifeAreaPulse> current = analytics.studio(userId, currentFrom, to, null).lifeAreas();
        Map<UUID, LifeAreaPulse> previous = analytics.studio(userId, previousFrom, previousTo, null)
            .lifeAreas().stream().collect(Collectors.toMap(LifeAreaPulse::lifeAreaId, Function.identity()));
        record Change(LifeAreaPulse current, LifeAreaPulse previous, double delta) {}
        Change best = current.stream()
            .filter(area -> area.eligible() >= 2 && previous.containsKey(area.lifeAreaId())
                && previous.get(area.lifeAreaId()).eligible() >= 2)
            .map(area -> new Change(area, previous.get(area.lifeAreaId()),
                safePercent(area.consistencyPercent()) - safePercent(previous.get(area.lifeAreaId()).consistencyPercent())))
            .max(Comparator.comparingDouble(Change::delta)).orElse(null);
        boolean ready = best != null;
        String title = ready && best.delta() >= 5
            ? best.current().name() + " is gaining steadiness"
            : ready ? best.current().name() + " is holding a steady rhythm"
            : "Life Area change needs a fair comparison";
        String summary = ready
            ? String.format(Locale.ROOT, "Rhythm moved from %.0f%% to %.0f%% across two equal %d-day windows.",
                safePercent(best.previous().consistencyPercent()),
                safePercent(best.current().consistencyPercent()), half)
            : "Arohan needs at least 2 scheduled opportunities in both halves for the same Life Area.";
        int sample = ready
            ? Math.min(best.current().eligible(), best.previous().eligible())
            : 0;
        return signal("improving-area", "RHYTHM", title, summary,
            ready ? best.current().eligible() + " recent and " + best.previous().eligible()
                + " earlier opportunities were compared." : "No Life Area has two comparable windows yet.",
            "The selected range is split into equal recent and earlier windows. Schedule-aware completion percentages are compared only when each window has at least 2 opportunities.",
            sample, 2, ready, "sage", List.of());
    }

    private GrowthSignal commonTime(List<HabitTrackingEntry> moments, ZoneId zone) {
        List<HabitTrackingEntry> completed = moments.stream()
            .filter(m -> m.getStatus() == TrackingStatus.COMPLETED && m.getCompletedAt() != null).toList();
        Map<String, Long> counts = completed.stream().collect(Collectors.groupingBy(
            m -> timeBand(m.getCompletedAt().atZone(zone).getHour()), LinkedHashMap::new, Collectors.counting()));
        Map.Entry<String, Long> best = counts.entrySet().stream()
            .max(Map.Entry.comparingByValue()).orElse(null);
        boolean ready = completed.size() >= 3;
        String title = ready ? "Your practice often lands in the " + best.getKey().toLowerCase(Locale.ROOT)
            : "Your natural practice time is still emerging";
        String summary = ready
            ? best.getValue() + " of " + completed.size() + " completed moments happened in this time window."
            : "Complete " + Math.max(0, 3 - completed.size()) + " more scheduled moment"
                + (3 - completed.size() == 1 ? "" : "s") + " to reveal a useful time pattern.";
        List<EvidenceItem> evidence = completed.stream().limit(6)
            .map(m -> new EvidenceItem(m.getScheduledDate(), "Completed",
                m.getCompletedAt().atZone(zone).toLocalTime().withSecond(0).withNano(0).toString()))
            .toList();
        return signal("natural-time", "RHYTHM", title, summary,
            completed.size() + " completed moments have a recorded finish time.",
            "Arohan groups completion times into morning, afternoon, evening, and night, then shows the most frequent group after 3 completions.",
            completed.size(), 3, ready, "sage", evidence);
    }

    private GrowthSignal commonBarrier(List<HabitTrackingEntry> moments) {
        List<HabitTrackingEntry> withFriction = moments.stream()
            .filter(m -> m.getFrictionNote() != null && !m.getFrictionNote().isBlank()).toList();
        Map<String, Long> counts = withFriction.stream().collect(Collectors.groupingBy(
            m -> m.getFrictionNote().strip().toLowerCase(Locale.ROOT), Collectors.counting()));
        Map.Entry<String, Long> best = counts.entrySet().stream()
            .max(Map.Entry.comparingByValue()).orElse(null);
        boolean ready = best != null && best.getValue() >= 2;
        String title = ready ? "A recurring friction: “" + titleCase(best.getKey()) + "”"
            : "Friction becomes useful when it repeats";
        String summary = ready
            ? "You mentioned this barrier " + best.getValue() + " times. A smaller cue or a different moment may help."
            : "Add a short friction note when a practice feels difficult; two similar notes can reveal a pattern.";
        List<EvidenceItem> evidence = withFriction.stream().limit(6)
            .map(m -> new EvidenceItem(m.getScheduledDate(), "Friction noted", m.getFrictionNote())).toList();
        return signal("recurring-friction", "FRICTION", title, summary,
            withFriction.size() + " moments include a friction note.",
            "Arohan normalizes your wording and surfaces the most repeated barrier only when it appears at least twice.",
            best == null ? 0 : best.getValue().intValue(), 2, ready, "clay", evidence);
    }

    private GrowthSignal reflectionTheme(UUID userId, List<JournalEntry> notes) {
        Map<UUID, Long> uses = notes.stream().flatMap(n -> n.getTagIds().stream())
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        Map.Entry<UUID, Long> best = uses.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
        JournalTag tag = best == null ? null : tags.findByIdAndUserId(best.getKey(), userId).orElse(null);
        boolean ready = tag != null && best.getValue() >= 2;
        return signal("reflection-theme", "REFLECTION", ready ? "“" + tag.getName() + "” keeps returning"
                : "Your reflection themes are taking root",
            ready ? "This tag appears across " + best.getValue() + " reflections."
                : "Use the same tag twice to let a theme emerge without analyzing your private words.",
            notes.size() + " reflections are included in this window.",
            "Only tag frequency is counted. Arohan does not infer emotions or diagnoses from journal text.",
            ready ? best.getValue().intValue() : uses.values().stream().mapToInt(Long::intValue).max().orElse(0),
            2, ready, "gold", List.of());
    }

    private GrowthSignal energyAssociation(List<HabitTrackingEntry> moments, List<JournalEntry> notes) {
        Map<LocalDate, Double> energy = notes.stream().filter(n -> n.getEnergyScore() != null)
            .collect(Collectors.groupingBy(JournalEntry::getEntryDate,
                Collectors.averagingInt(JournalEntry::getEnergyScore)));
        Map<LocalDate, Boolean> practiced = moments.stream().collect(Collectors.groupingBy(
            HabitTrackingEntry::getScheduledDate,
            Collectors.collectingAndThen(Collectors.toList(), list -> list.stream().anyMatch(
                m -> m.getStatus() == TrackingStatus.COMPLETED || m.getStatus() == TrackingStatus.PARTIAL))));
        List<Double> met = energy.entrySet().stream().filter(e -> practiced.getOrDefault(e.getKey(), false))
            .map(Map.Entry::getValue).toList();
        List<Double> notMet = energy.entrySet().stream().filter(e -> !practiced.getOrDefault(e.getKey(), false))
            .map(Map.Entry::getValue).toList();
        boolean ready = met.size() >= 3 && notMet.size() >= 3;
        double metAverage = met.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double otherAverage = notMet.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        String summary = ready
            ? String.format(Locale.ROOT, "Average energy was %.1f on practice days and %.1f on other check-in days.",
                metAverage, otherAverage)
            : "Record energy on at least 3 practice days and 3 other days for a fair comparison.";
        List<EvidenceItem> evidence = energy.entrySet().stream().sorted(Map.Entry.comparingByKey())
            .limit(8).map(e -> new EvidenceItem(e.getKey(), practiced.getOrDefault(e.getKey(), false)
                ? "Practice day" : "Other day", String.format(Locale.ROOT, "Energy %.1f/5", e.getValue()))).toList();
        return signal("energy-practice", "ASSOCIATION",
            ready ? "Energy and practice, side by side" : "Building an energy comparison", summary,
            energy.size() + " dated energy check-ins can be compared.",
            "Arohan compares average energy on dates with completed or partial practice against other check-in dates. This is association, not causation.",
            Math.min(met.size(), notMet.size()), 3, ready, "sky", evidence);
    }

    private GrowthSignal signal(String key, String kind, String title, String summary, String evidence,
                                String method, int sample, int minimum, boolean ready, String tone,
                                List<EvidenceItem> items) {
        return new GrowthSignal(key, kind, title, summary, evidence, method, sample, minimum, ready, tone, items);
    }
    private String timeBand(int hour) {
        if (hour >= 5 && hour < 12) return "Morning";
        if (hour >= 12 && hour < 17) return "Afternoon";
        if (hour >= 17 && hour < 22) return "Evening";
        return "Night";
    }
    private String titleCase(String value) {
        return value.isBlank() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
    private double safePercent(Double value) { return value == null ? 0 : value; }
}
