package com.arohan.tracking;

import com.arohan.habit.GrowthHabit;
import com.arohan.habit.GrowthHabitRepository;
import com.arohan.habit.HabitSchedule;
import com.arohan.habit.HabitScheduleRepository;
import com.arohan.habit.HabitStatus;
import com.arohan.habit.ScheduleType;
import com.arohan.lifearea.LifeArea;
import com.arohan.lifearea.LifeAreaRepository;
import com.arohan.lifearea.LifeAreaStatus;
import com.arohan.shared.ApiException;
import com.arohan.tracking.TrackingDtos.RecordPracticeRequest;
import com.arohan.tracking.TrackingDtos.TodayHabitResponse;
import com.arohan.tracking.TrackingDtos.TodayResponse;
import com.arohan.tracking.TrackingDtos.TrackingEntryResponse;
import com.arohan.user.User;
import com.arohan.user.UserRepository;
import com.arohan.user.WeekStart;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrackingService {
    private final HabitTrackingRepository entries;
    private final GrowthHabitRepository habits;
    private final HabitScheduleRepository schedules;
    private final LifeAreaRepository areas;
    private final UserRepository users;
    private final ScheduleEngine engine;

    public TrackingService(HabitTrackingRepository entries, GrowthHabitRepository habits,
                           HabitScheduleRepository schedules, LifeAreaRepository areas,
                           UserRepository users, ScheduleEngine engine) {
        this.entries = entries;
        this.habits = habits;
        this.schedules = schedules;
        this.areas = areas;
        this.users = users;
        this.engine = engine;
    }

    @Transactional(readOnly = true)
    public TodayResponse today(UUID userId, LocalDate requestedDate) {
        User user = requireUser(userId);
        LocalDate today = LocalDate.now(zone(user));
        LocalDate date = requestedDate == null ? today : requestedDate;
        if (date.isAfter(today.plusDays(31)) || date.isBefore(today.minusYears(2))) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "Choose a date near your current growth journey.");
        }
        List<TodayHabitResponse> due = habits
            .findAllByUserIdOrderByPositionIndexAscCreatedAtAsc(userId).stream()
            .filter(habit -> habit.getStatus() == HabitStatus.ACTIVE)
            .map(habit -> todayItem(user, habit, date))
            .filter(java.util.Objects::nonNull)
            .toList();
        long completed = due.stream().filter(item -> item.entry() != null
            && item.entry().status() == TrackingStatus.COMPLETED).count();
        long partial = due.stream().filter(item -> item.entry() != null
            && item.entry().status() == TrackingStatus.PARTIAL).count();
        long remaining = due.stream().filter(item -> item.entry() == null
            || item.entry().status() == null).count();
        return new TodayResponse(date, user.getTimeZone(), completed, partial, remaining, due);
    }

    @Transactional
    public TrackingEntryResponse cueStart(UUID userId, UUID habitId, LocalDate date) {
        User user = requireUser(userId);
        GrowthHabit habit = requireHabit(userId, habitId);
        requireAvailable(user, habit, date);
        HabitTrackingEntry entry = entries
            .findByUserIdAndHabitIdAndScheduledDate(userId, habitId, date)
            .orElseGet(() -> new HabitTrackingEntry(userId, habitId, date));
        entry.recordCueStart();
        return response(entries.save(entry));
    }

    @Transactional
    public TrackingEntryResponse record(UUID userId, UUID habitId, LocalDate date,
                                        RecordPracticeRequest request) {
        User user = requireUser(userId);
        GrowthHabit habit = requireHabit(userId, habitId);
        requireAvailable(user, habit, date);
        validateActual(habit, request);
        HabitTrackingEntry entry = entries
            .findByUserIdAndHabitIdAndScheduledDate(userId, habitId, date)
            .orElseGet(() -> new HabitTrackingEntry(userId, habitId, date));
        entry.record(request);
        return response(entries.save(entry));
    }

    @Transactional
    public void clear(UUID userId, UUID habitId, LocalDate date) {
        requireHabit(userId, habitId);
        entries.findByUserIdAndHabitIdAndScheduledDate(userId, habitId, date)
            .ifPresent(entries::delete);
    }

    private TodayHabitResponse todayItem(User user, GrowthHabit habit, LocalDate date) {
        LifeArea area = areas.findByIdAndUserId(habit.getLifeAreaId(), user.getId()).orElse(null);
        if (area == null || area.getStatus() != LifeAreaStatus.ACTIVE) return null;
        HabitSchedule schedule = requireSchedule(habit.getId());
        HabitTrackingEntry existing = entries
            .findByUserIdAndHabitIdAndScheduledDate(user.getId(), habit.getId(), date)
            .orElse(null);
        String type;
        if (existing != null) type = "RECORDED";
        else if (engine.isFixedEligible(habit, schedule, date)) type = "DUE";
        else if (schedule.getType() == ScheduleType.CUSTOM
            && !date.isBefore(engine.firstEligibleDate(habit, schedule))) type = "CUSTOM";
        else if (engine.isFlexible(schedule.getType())
            && flexibleRemaining(user, habit, schedule, date) > 0) type = "FLEXIBLE";
        else return null;
        return new TodayHabitResponse(
            habit.getId(), area.getId(), area.getName(), area.getColorHex(),
            habit.getName(), habit.getPurpose(), habit.getCueNote(),
            habit.getTwoMinuteStarter(), habit.getPreferredTime(), habit.getPreferredPlace(),
            habit.getTrackingMethod(), habit.getTargetValue(), habit.getTargetUnit(),
            schedule.getType(), rhythmLabel(schedule), type,
            existing == null ? null : response(existing)
        );
    }

    private void requireAvailable(User user, GrowthHabit habit, LocalDate date) {
        if (habit.getStatus() != HabitStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT,
                "Return this Growth Habit to an active rhythm before recording it.");
        }
        LifeArea area = areas.findByIdAndUserId(habit.getLifeAreaId(), user.getId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                "Life Area was not found."));
        if (area.getStatus() != LifeAreaStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT,
                "Restore the Life Area before recording this Growth Habit.");
        }
        LocalDate current = LocalDate.now(zone(user));
        if (date.isAfter(current)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "A future practice cannot be recorded yet.");
        }
        HabitSchedule schedule = requireSchedule(habit.getId());
        boolean existing = entries.findByUserIdAndHabitIdAndScheduledDate(
            user.getId(), habit.getId(), date).isPresent();
        boolean available = existing || engine.isFixedEligible(habit, schedule, date)
            || (schedule.getType() == ScheduleType.CUSTOM
                && !date.isBefore(engine.firstEligibleDate(habit, schedule)))
            || (engine.isFlexible(schedule.getType())
                && flexibleRemaining(user, habit, schedule, date) > 0);
        if (!available) {
            throw new ApiException(HttpStatus.CONFLICT,
                "This date is not an eligible opportunity for the Growth Habit.");
        }
    }

    private int flexibleRemaining(User user, GrowthHabit habit, HabitSchedule schedule,
                                  LocalDate date) {
        if (date.isBefore(engine.firstEligibleDate(habit, schedule))) return 0;
        LocalDate from;
        LocalDate to;
        if (schedule.getType() == ScheduleType.TIMES_PER_WEEK) {
            DayOfWeek firstDay = user.getWeekStart() == WeekStart.SUNDAY
                ? DayOfWeek.SUNDAY : DayOfWeek.MONDAY;
            from = date.with(TemporalAdjusters.previousOrSame(firstDay));
            to = from.plusDays(6);
        } else {
            from = date.withDayOfMonth(1);
            to = date.with(TemporalAdjusters.lastDayOfMonth());
        }
        long recorded = entries.countByUserIdAndHabitIdAndScheduledDateBetweenAndStatusIn(
            user.getId(), habit.getId(), from, to,
            List.of(TrackingStatus.COMPLETED, TrackingStatus.PARTIAL));
        return Math.max(0, schedule.getTargetCount() - (int) recorded);
    }

    private void validateActual(GrowthHabit habit, RecordPracticeRequest request) {
        if (request.actualValue() != null && habit.getTargetValue() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "This Growth Habit does not use a measured progress value.");
        }
    }

    private String rhythmLabel(HabitSchedule schedule) {
        return switch (schedule.getType()) {
            case DAILY -> "Every day";
            case SELECTED_WEEKDAYS -> "Selected weekdays";
            case TIMES_PER_WEEK -> schedule.getTargetCount() + " times this week";
            case TIMES_PER_MONTH -> schedule.getTargetCount() + " times this month";
            case CUSTOM -> schedule.getCustomDescription();
            case ONE_TIME -> "One meaningful date";
            case ALTERNATE_DAYS -> "Alternate days";
            case EVERY_N_DAYS, ROTATION -> "Every " + schedule.getIntervalDays() + " days";
        };
    }

    private TrackingEntryResponse response(HabitTrackingEntry entry) {
        return new TrackingEntryResponse(
            entry.getId(), entry.getHabitId(), entry.getScheduledDate(), entry.getStatus(),
            entry.getActualValue(), entry.getQualityRating(), entry.getReflection(),
            entry.getFrictionNote(), entry.getCueStartedAt(), entry.getCompletedAt()
        );
    }

    private User requireUser(UUID userId) {
        return users.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User was not found."));
    }

    private GrowthHabit requireHabit(UUID userId, UUID id) {
        return habits.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                "Growth Habit was not found."));
    }

    private HabitSchedule requireSchedule(UUID habitId) {
        return schedules.findByHabitId(habitId)
            .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT,
                "This Growth Habit has no schedule."));
    }

    private ZoneId zone(User user) {
        try {
            return ZoneId.of(user.getTimeZone());
        } catch (java.time.DateTimeException ignored) {
            return ZoneId.of("UTC");
        }
    }
}
