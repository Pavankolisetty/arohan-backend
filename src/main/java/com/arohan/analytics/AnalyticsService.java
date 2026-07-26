package com.arohan.analytics;

import com.arohan.analytics.AnalyticsDtos.CountSummary;
import com.arohan.analytics.AnalyticsDtos.CueFlow;
import com.arohan.analytics.AnalyticsDtos.GrowthStudioResponse;
import com.arohan.analytics.AnalyticsDtos.LifeAreaPulse;
import com.arohan.analytics.AnalyticsDtos.ProgressStory;
import com.arohan.analytics.AnalyticsDtos.RhythmDay;
import com.arohan.analytics.AnalyticsDtos.RhythmRecord;
import com.arohan.analytics.AnalyticsDtos.Signal;
import com.arohan.analytics.AnalyticsDtos.SkillJourney;
import com.arohan.habit.GrowthHabit;
import com.arohan.habit.GrowthHabitRepository;
import com.arohan.habit.HabitSchedule;
import com.arohan.habit.HabitScheduleRepository;
import com.arohan.habit.ScheduleType;
import com.arohan.lifearea.LifeArea;
import com.arohan.lifearea.LifeAreaRepository;
import com.arohan.shared.ApiException;
import com.arohan.tracking.HabitTrackingEntry;
import com.arohan.tracking.HabitTrackingRepository;
import com.arohan.tracking.ScheduleEngine;
import com.arohan.tracking.TrackingStatus;
import com.arohan.user.User;
import com.arohan.user.UserRepository;
import com.arohan.user.WeekStart;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {
    private final GrowthHabitRepository habits;
    private final HabitScheduleRepository schedules;
    private final HabitTrackingRepository entries;
    private final LifeAreaRepository areas;
    private final UserRepository users;
    private final ScheduleEngine engine;

    public AnalyticsService(GrowthHabitRepository habits, HabitScheduleRepository schedules,
                            HabitTrackingRepository entries, LifeAreaRepository areas,
                            UserRepository users, ScheduleEngine engine) {
        this.habits = habits;
        this.schedules = schedules;
        this.entries = entries;
        this.areas = areas;
        this.users = users;
        this.engine = engine;
    }

    @Transactional(readOnly = true)
    public GrowthStudioResponse studio(UUID userId, LocalDate from, LocalDate to,
                                       UUID lifeAreaId) {
        User user = users.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User was not found."));
        LocalDate localToday = LocalDate.now(zone(user));
        LocalDate end = to == null || to.isAfter(localToday) ? localToday : to;
        LocalDate start = from == null ? end.minusDays(6) : from;
        if (start.isAfter(end) || ChronoUnit.DAYS.between(start, end) > 366) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "Choose a valid Growth Studio range of up to 366 days.");
        }
        Map<UUID, LifeArea> areaMap = new HashMap<>();
        areas.findAllByUserIdOrderByPositionIndexAscCreatedAtAsc(userId)
            .forEach(area -> areaMap.put(area.getId(), area));
        List<Context> contexts = habits
            .findAllByUserIdOrderByPositionIndexAscCreatedAtAsc(userId).stream()
            .filter(habit -> lifeAreaId == null || habit.getLifeAreaId().equals(lifeAreaId))
            .map(habit -> new Context(habit,
                schedules.findByHabitId(habit.getId()).orElse(null),
                areaMap.get(habit.getLifeAreaId())))
            .filter(context -> context.schedule() != null && context.area() != null)
            .toList();
        List<HabitTrackingEntry> rangeEntries = entries
            .findAllByUserIdAndScheduledDateBetweenOrderByScheduledDateAsc(userId, start, end);
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        LocalDate previousEnd = start.minusDays(1);
        LocalDate previousStart = previousEnd.minusDays(days - 1);
        List<HabitTrackingEntry> previousEntries = entries
            .findAllByUserIdAndScheduledDateBetweenOrderByScheduledDateAsc(
                userId, previousStart, previousEnd);

        Evaluation current = evaluate(user, contexts, rangeEntries, start, end, localToday, true);
        Evaluation previous = evaluate(user, contexts, previousEntries,
            previousStart, previousEnd, localToday, false);
        Double momentumDelta = current.counts().eligible() > 0
            && previous.counts().eligible() > 0
            ? round(current.counts().consistencyPercent()
                - previous.counts().consistencyPercent()) : null;
        String momentumDirection = momentumDelta == null ? "GATHERING"
            : momentumDelta >= 5 ? "IMPROVING"
            : momentumDelta <= -5 ? "DECLINING" : "STEADY";

        Recovery recovery = recovery(contexts, rangeEntries, start, end, localToday);
        int cueStarts = (int) rangeEntries.stream()
            .filter(entry -> entry.getCueStartedAt() != null).count();
        int afterStart = (int) rangeEntries.stream()
            .filter(entry -> entry.getCueStartedAt() != null)
            .filter(entry -> entry.getStatus() == TrackingStatus.COMPLETED
                || entry.getStatus() == TrackingStatus.PARTIAL).count();
        Double cuePercent = cueStarts == 0 ? null : percent(afterStart, cueStarts);
        int reflections = (int) rangeEntries.stream()
            .filter(entry -> entry.getCueStartedAt() != null)
            .filter(entry -> entry.getReflection() != null).count();

        List<Signal> signals = List.of(
            new Signal("CONSISTENCY", "Rhythm met", current.counts().consistencyPercent(),
                "%", current.counts().eligible(), 1, current.counts().eligible() >= 1,
                null, "Completed moments divided by genuine scheduled opportunities."),
            new Signal("MOMENTUM", "Momentum", momentumDelta, "percentage points",
                Math.min(current.counts().eligible(), previous.counts().eligible()), 2,
                current.counts().eligible() >= 2 && previous.counts().eligible() >= 2,
                momentumDirection, "Your current rhythm compared with the previous equal period."),
            new Signal("RECOVERY", "Recovery", recovery.percent(), "%",
                recovery.recoverableMisses(), 2, recovery.recoverableMisses() >= 2,
                null, "Returns on the next scheduled opportunity after a missed one.")
        );

        List<LifeAreaPulse> areaPulses = areaPulses(contexts, current);
        CueFlow cueFlow = new CueFlow(current.counts().eligible(), cueStarts, afterStart,
            reflections, cuePercent, cueStarts >= 3);
        List<SkillJourney> journey = skillJourney(contexts, current, end);
        ProgressStory story = story(current, momentumDirection, recovery, contexts,
            rangeEntries, cueStarts, afterStart);
        return new GrowthStudioResponse(start, end, user.getTimeZone(), Instant.now(),
            current.counts(), signals, areaPulses, current.rhythm(), cueFlow, journey, story);
    }

    private Evaluation evaluate(User user, List<Context> contexts,
                                List<HabitTrackingEntry> rangeEntries,
                                LocalDate from, LocalDate to, LocalDate today,
                                boolean includeRhythm) {
        Map<EntryKey, HabitTrackingEntry> entryMap = new HashMap<>();
        rangeEntries.forEach(entry -> entryMap.put(
            new EntryKey(entry.getHabitId(), entry.getScheduledDate()), entry));
        Map<LocalDate, DayBuilder> days = new LinkedHashMap<>();
        if (includeRhythm) {
            from.datesUntil(to.plusDays(1)).forEach(date -> days.put(date, new DayBuilder(date)));
        }
        MutableCounts total = new MutableCounts();
        Map<UUID, MutableCounts> byArea = new HashMap<>();
        Map<UUID, MutableCounts> byHabit = new HashMap<>();

        for (Context context : contexts) {
            MutableCounts areaCounts = byArea.computeIfAbsent(
                context.habit().getLifeAreaId(), ignored -> new MutableCounts());
            MutableCounts habitCounts = byHabit.computeIfAbsent(
                context.habit().getId(), ignored -> new MutableCounts());
            if (engine.isFlexible(context.schedule().getType())) {
                evaluateFlexible(user, context, rangeEntries, from, to, today,
                    total, areaCounts, habitCounts, days);
                continue;
            }
            for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
                HabitTrackingEntry entry = entryMap.get(new EntryKey(context.habit().getId(), date));
                boolean eligible = engine.isFixedEligible(context.habit(), context.schedule(), date)
                    || (context.schedule().getType() == ScheduleType.CUSTOM && entry != null);
                if (!eligible) continue;
                applyOpportunity(total, areaCounts, habitCounts, entry, date, today);
                if (includeRhythm) addDay(days.get(date), context, entry, date, today);
            }
        }
        return new Evaluation(total.summary(), byArea, byHabit,
            days.values().stream().map(DayBuilder::response).toList());
    }

    private void evaluateFlexible(User user, Context context,
                                  List<HabitTrackingEntry> rangeEntries,
                                  LocalDate from, LocalDate to, LocalDate today,
                                  MutableCounts total, MutableCounts area,
                                  MutableCounts habit, Map<LocalDate, DayBuilder> days) {
        LocalDate lastEligible = engine.lastEligibleDate(context.habit());
        LocalDate effectiveTo = lastEligible != null && lastEligible.isBefore(to)
            ? lastEligible : to;
        if (effectiveTo.isBefore(from)) return;
        List<HabitTrackingEntry> matching = rangeEntries.stream()
            .filter(entry -> entry.getHabitId().equals(context.habit().getId())).toList();
        matching.forEach(entry -> {
            applyOpportunity(total, area, habit, entry, entry.getScheduledDate(), today);
            if (!days.isEmpty()) addDay(days.get(entry.getScheduledDate()), context, entry,
                entry.getScheduledDate(), today);
        });
        LocalDate cursor = from;
        while (!cursor.isAfter(effectiveTo)) {
            LocalDate periodStart;
            LocalDate periodEnd;
            if (context.schedule().getType() == ScheduleType.TIMES_PER_WEEK) {
                DayOfWeek first = user.getWeekStart() == WeekStart.SUNDAY
                    ? DayOfWeek.SUNDAY : DayOfWeek.MONDAY;
                periodStart = cursor.with(TemporalAdjusters.previousOrSame(first));
                periodEnd = periodStart.plusDays(6);
            } else {
                periodStart = cursor.withDayOfMonth(1);
                periodEnd = cursor.with(TemporalAdjusters.lastDayOfMonth());
            }
            LocalDate boundedStart = periodStart.isBefore(from) ? from : periodStart;
            LocalDate boundedEnd = periodEnd.isAfter(effectiveTo) ? effectiveTo : periodEnd;
            int recorded = (int) matching.stream().filter(entry ->
                !entry.getScheduledDate().isBefore(boundedStart)
                    && !entry.getScheduledDate().isAfter(boundedEnd)
                    && entry.getStatus() != null).count();
            boolean closed = periodEnd.isBefore(today) && !boundedEnd.isBefore(
                engine.firstEligibleDate(context.habit(), context.schedule()));
            if (closed) {
                int expected = Math.max(recorded, context.schedule().getTargetCount());
                int missing = expected - recorded;
                total.addMissed(missing);
                area.addMissed(missing);
                habit.addMissed(missing);
            }
            cursor = periodEnd.plusDays(1);
        }
    }

    private void applyOpportunity(MutableCounts total, MutableCounts area, MutableCounts habit,
                                  HabitTrackingEntry entry, LocalDate date, LocalDate today) {
        total.eligible++;
        area.eligible++;
        habit.eligible++;
        if (entry == null || entry.getStatus() == null) {
            if (date.isBefore(today)) {
                total.missed++;
                area.missed++;
                habit.missed++;
            }
            return;
        }
        switch (entry.getStatus()) {
            case COMPLETED -> { total.completed++; area.completed++; habit.completed++; }
            case PARTIAL -> { total.partial++; area.partial++; habit.partial++; }
            case SKIPPED -> { total.skipped++; area.skipped++; habit.skipped++; }
        }
    }

    private void addDay(DayBuilder day, Context context, HabitTrackingEntry entry,
                        LocalDate date, LocalDate today) {
        if (day == null) return;
        day.due++;
        TrackingStatus status = entry == null ? null : entry.getStatus();
        if (status == TrackingStatus.COMPLETED) day.completed++;
        else if (status == TrackingStatus.PARTIAL) day.partial++;
        else if (status == TrackingStatus.SKIPPED) day.skipped++;
        else if (date.isBefore(today)) day.missed++;
        day.records.add(new RhythmRecord(
            context.habit().getId(), context.habit().getName(), context.area().getName(),
            status, entry != null && entry.getCueStartedAt() != null,
            entry == null ? null : entry.getActualValue(), context.habit().getTargetUnit(),
            entry == null ? null : entry.getQualityRating(),
            entry == null ? null : entry.getReflection()
        ));
    }

    private Recovery recovery(List<Context> contexts, List<HabitTrackingEntry> rangeEntries,
                              LocalDate from, LocalDate to, LocalDate today) {
        Map<EntryKey, HabitTrackingEntry> map = new HashMap<>();
        rangeEntries.forEach(entry -> map.put(
            new EntryKey(entry.getHabitId(), entry.getScheduledDate()), entry));
        int recoverable = 0;
        int returns = 0;
        for (Context context : contexts) {
            if (engine.isFlexible(context.schedule().getType())
                || context.schedule().getType() == ScheduleType.CUSTOM) continue;
            List<LocalDate> opportunities = from.datesUntil(to.plusDays(1))
                .filter(date -> date.isBefore(today))
                .filter(date -> engine.isFixedEligible(
                    context.habit(), context.schedule(), date)).toList();
            for (int index = 0; index + 1 < opportunities.size(); index++) {
                HabitTrackingEntry missed = map.get(
                    new EntryKey(context.habit().getId(), opportunities.get(index)));
                if (missed != null && missed.getStatus() != null) continue;
                recoverable++;
                HabitTrackingEntry next = map.get(
                    new EntryKey(context.habit().getId(), opportunities.get(index + 1)));
                if (next != null && (next.getStatus() == TrackingStatus.COMPLETED
                    || next.getStatus() == TrackingStatus.PARTIAL)) returns++;
            }
        }
        return new Recovery(recoverable, returns,
            recoverable == 0 ? null : percent(returns, recoverable));
    }

    private List<LifeAreaPulse> areaPulses(List<Context> contexts, Evaluation evaluation) {
        Map<UUID, LifeArea> unique = new LinkedHashMap<>();
        contexts.forEach(context -> unique.put(context.area().getId(), context.area()));
        int importanceTotal = unique.values().stream()
            .mapToInt(LifeArea::getDesiredImportance).sum();
        int attentionTotal = evaluation.byArea().values().stream()
            .mapToInt(counts -> counts.completed + counts.partial).sum();
        return unique.values().stream().map(area -> {
            MutableCounts counts = evaluation.byArea()
                .getOrDefault(area.getId(), new MutableCounts());
            double desired = importanceTotal == 0 ? 0
                : area.getDesiredImportance() * 100.0 / importanceTotal;
            double actual = attentionTotal == 0 ? 0
                : (counts.completed + counts.partial) * 100.0 / attentionTotal;
            double difference = actual - desired;
            String alignment = attentionTotal < 5 ? "GATHERING"
                : difference > 10 ? "MORE_ATTENTION"
                : difference < -10 ? "LESS_ATTENTION" : "ALIGNED";
            return new LifeAreaPulse(area.getId(), area.getName(), area.getColorHex(),
                area.getDesiredImportance(), counts.eligible, counts.completed,
                counts.summary().consistencyPercent(), round(desired), round(actual), alignment);
        }).toList();
    }

    private List<SkillJourney> skillJourney(List<Context> contexts, Evaluation evaluation,
                                            LocalDate end) {
        return contexts.stream().map(context -> {
            MutableCounts counts = evaluation.byHabit()
                .getOrDefault(context.habit().getId(), new MutableCounts());
            Double consistency = counts.summary().consistencyPercent();
            long age = Math.max(0, ChronoUnit.DAYS.between(
                context.habit().getTrackingEnabledFrom(), end) + 1);
            String stage;
            String hint;
            if (counts.eligible < 3) {
                stage = "Seed"; hint = "Meet this habit on three eligible opportunities.";
            } else if (counts.eligible < 7 || value(consistency) < 40) {
                stage = "Starting"; hint = "Keep making the beginning easy and repeatable.";
            } else if (counts.eligible < 14 || value(consistency) < 65) {
                stage = "Building Rhythm"; hint = "Practise returning after an interrupted day.";
            } else if (counts.eligible < 30 || age < 42 || value(consistency) < 80) {
                stage = "Becoming Consistent"; hint = "Let this rhythm settle across different weeks.";
            } else {
                stage = "Integrated"; hint = "Protect the cue while allowing the practice to evolve.";
            }
            return new SkillJourney(context.habit().getId(), context.habit().getName(),
                context.area().getName(), stage, counts.eligible, counts.completed,
                consistency, age, hint);
        }).toList();
    }

    private ProgressStory story(Evaluation current, String momentum, Recovery recovery,
                                List<Context> contexts, List<HabitTrackingEntry> rangeEntries,
                                int cueStarts, int afterStart) {
        String evidence = current.counts().eligible() == 0
            ? "Your story will use recorded moments, not estimates."
            : "You completed " + current.counts().completed() + " of "
                + current.counts().eligible() + " eligible opportunities.";
        String recoveryText = recovery.returns() == 0
            ? "The next return will be meaningful evidence."
            : "You returned " + recovery.returns() + " times after an interrupted rhythm.";
        String strongestCue = cueStarts == 0 ? "No cue starts recorded yet."
            : afterStart + " of " + cueStarts + " cue starts flowed into practice.";
        Map<String, Long> barriers = rangeEntries.stream()
            .filter(entry -> entry.getFrictionNote() != null)
            .collect(java.util.stream.Collectors.groupingBy(
                HabitTrackingEntry::getFrictionNote, java.util.stream.Collectors.counting()));
        String barrier = barriers.entrySet().stream()
            .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey)
            .orElse("No recurring barrier has enough evidence yet.");
        String experiment = momentum.equals("DECLINING")
            ? "Try shrinking one Easy Start Cue for the next eligible opportunity."
            : cueStarts < 3
                ? "Tap Start when the cue appears so Arohan can learn what helps."
                : "Keep the strongest cue stable for one more week.";
        return new ProgressStory("Your Progress Story", evidence, recoveryText,
            strongestCue, barrier, experiment);
    }

    private ZoneId zone(User user) {
        try { return ZoneId.of(user.getTimeZone()); }
        catch (java.time.DateTimeException ignored) { return ZoneId.of("UTC"); }
    }

    private double value(Double number) { return number == null ? 0 : number; }
    private static Double percent(int numerator, int denominator) {
        return denominator == 0 ? null : round(numerator * 100.0 / denominator);
    }
    private static Double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private record Context(GrowthHabit habit, HabitSchedule schedule, LifeArea area) {}
    private record EntryKey(UUID habitId, LocalDate date) {}
    private record Evaluation(CountSummary counts, Map<UUID, MutableCounts> byArea,
                              Map<UUID, MutableCounts> byHabit, List<RhythmDay> rhythm) {}
    private record Recovery(int recoverableMisses, int returns, Double percent) {}
    private static final class MutableCounts {
        int eligible;
        int completed;
        int partial;
        int skipped;
        int missed;

        void addMissed(int count) {
            eligible += count;
            missed += count;
        }

        CountSummary summary() {
            return new CountSummary(eligible, completed, partial, skipped, missed,
                percent(completed, eligible));
        }
    }

    private static final class DayBuilder {
        final LocalDate date;
        int due;
        int completed;
        int partial;
        int skipped;
        int missed;
        int recoveries;
        final List<RhythmRecord> records = new ArrayList<>();

        DayBuilder(LocalDate date) { this.date = date; }
        RhythmDay response() {
            return new RhythmDay(date, due, completed, partial, skipped, missed,
                recoveries, List.copyOf(records));
        }
    }
}
