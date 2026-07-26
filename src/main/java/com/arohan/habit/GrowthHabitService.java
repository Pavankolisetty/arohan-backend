package com.arohan.habit;

import com.arohan.habit.HabitDtos.HabitResponse;
import com.arohan.habit.HabitDtos.ScheduleRequest;
import com.arohan.habit.HabitDtos.ScheduleResponse;
import com.arohan.habit.HabitDtos.UpsertHabitRequest;
import com.arohan.lifearea.LifeArea;
import com.arohan.lifearea.LifeAreaRepository;
import com.arohan.lifearea.LifeAreaStatus;
import com.arohan.shared.ApiException;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GrowthHabitService {
    private final GrowthHabitRepository habits;
    private final HabitScheduleRepository schedules;
    private final LifeAreaRepository areas;

    public GrowthHabitService(GrowthHabitRepository habits, HabitScheduleRepository schedules,
                              LifeAreaRepository areas) {
        this.habits = habits;
        this.schedules = schedules;
        this.areas = areas;
    }

    @Transactional(readOnly = true)
    public List<HabitResponse> list(UUID userId, HabitStatus status, UUID lifeAreaId) {
        return habits.findAllByUserIdOrderByPositionIndexAscCreatedAtAsc(userId).stream()
            .filter(habit -> status == null || habit.getStatus() == status)
            .filter(habit -> lifeAreaId == null || habit.getLifeAreaId().equals(lifeAreaId))
            .map(habit -> response(userId, habit))
            .toList();
    }

    @Transactional(readOnly = true)
    public HabitResponse get(UUID userId, UUID id) {
        return response(userId, require(userId, id));
    }

    @Transactional
    public HabitResponse create(UUID userId, UpsertHabitRequest request) {
        validate(userId, request);
        GrowthHabit habit = habits.save(new GrowthHabit(userId, request));
        schedules.save(new HabitSchedule(habit.getId(), request.schedule()));
        return response(userId, habit);
    }

    @Transactional
    public HabitResponse update(UUID userId, UUID id, UpsertHabitRequest request) {
        validate(userId, request);
        GrowthHabit habit = require(userId, id);
        habit.update(request);
        schedules.findByHabitId(id)
            .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT,
                "This Growth Habit has no schedule."))
            .update(request.schedule());
        return response(userId, habit);
    }

    @Transactional
    public HabitResponse pause(UUID userId, UUID id) {
        GrowthHabit habit = require(userId, id);
        if (habit.getStatus() == HabitStatus.ARCHIVED) {
            throw new ApiException(HttpStatus.CONFLICT,
                "An archived Growth Habit cannot be paused.");
        }
        habit.pause();
        return response(userId, habit);
    }

    @Transactional
    public HabitResponse restart(UUID userId, UUID id) {
        GrowthHabit habit = require(userId, id);
        if (habit.getStatus() == HabitStatus.ARCHIVED) {
            throw new ApiException(HttpStatus.CONFLICT,
                "Restore the archived Growth Habit before restarting it.");
        }
        LifeArea area = requireArea(userId, habit.getLifeAreaId());
        if (area.getStatus() != LifeAreaStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT,
                "Restore its Life Area before restarting this Growth Habit.");
        }
        habit.restart();
        return response(userId, habit);
    }

    @Transactional
    public HabitResponse archive(UUID userId, UUID id) {
        GrowthHabit habit = require(userId, id);
        habit.archive();
        return response(userId, habit);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        GrowthHabit habit = require(userId, id);
        habits.delete(habit);
    }

    private void validate(UUID userId, UpsertHabitRequest request) {
        LifeArea area = requireArea(userId, request.lifeAreaId());
        if (area.getStatus() != LifeAreaStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT,
                "Choose an active Life Area.");
        }
        ScheduleRequest schedule = request.schedule();
        if (request.kind() == HabitKind.MILESTONE
            && (request.trackingMethod() != TrackingMethod.MILESTONE
                || schedule.type() != ScheduleType.ONE_TIME)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "A Milestone uses milestone tracking and a one-time schedule.");
        }
        if (request.kind() == HabitKind.GROWTH_HABIT
            && request.trackingMethod() == TrackingMethod.MILESTONE) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "Milestone tracking is only available for a Milestone.");
        }
        if (Set.of(TrackingMethod.DURATION, TrackingMethod.QUANTITY, TrackingMethod.VALUE)
            .contains(request.trackingMethod())
            && (request.targetValue() == null || request.targetUnit() == null
                || request.targetUnit().isBlank())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "This tracking method needs a positive target and unit.");
        }
        if (schedule.type() == ScheduleType.SELECTED_WEEKDAYS
            && (schedule.weekdays() == null || schedule.weekdays().isEmpty())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "Choose at least one weekday.");
        }
        if (Set.of(ScheduleType.ALTERNATE_DAYS, ScheduleType.EVERY_N_DAYS,
                   ScheduleType.ROTATION).contains(schedule.type())
            && (schedule.intervalDays() == null || schedule.intervalDays() < 2
                || schedule.intervalDays() > 365)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "This rhythm needs an interval from 2 to 365 days.");
        }
        if (Set.of(ScheduleType.TIMES_PER_WEEK, ScheduleType.TIMES_PER_MONTH)
            .contains(schedule.type())
            && (schedule.targetCount() == null || schedule.targetCount() < 1
                || schedule.targetCount() > 31)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "Choose a frequency from 1 to 31.");
        }
        if (schedule.type() == ScheduleType.ONE_TIME && schedule.dueDate() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "Choose a date for this one-time rhythm.");
        }
        if (schedule.type() == ScheduleType.CUSTOM
            && (schedule.customDescription() == null
                || schedule.customDescription().isBlank())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "Describe your custom rhythm.");
        }
    }

    private HabitResponse response(UUID userId, GrowthHabit habit) {
        LifeArea area = requireArea(userId, habit.getLifeAreaId());
        HabitSchedule schedule = schedules.findByHabitId(habit.getId())
            .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT,
                "This Growth Habit has no schedule."));
        Set<DayOfWeek> weekdays = schedule.getWeekdays() == null
            ? Set.of()
            : Arrays.stream(schedule.getWeekdays().split(","))
                .map(DayOfWeek::valueOf)
                .collect(java.util.stream.Collectors.toCollection(
                    () -> EnumSet.noneOf(DayOfWeek.class)));
        return new HabitResponse(
            habit.getId(), habit.getLifeAreaId(), area.getName(), area.getColorHex(),
            habit.getKind(), habit.getName(), habit.getPurpose(), habit.getTrackingMethod(),
            habit.getTargetValue(), habit.getTargetUnit(), habit.getCueNote(),
            habit.getTwoMinuteStarter(), habit.getPreferredTime(), habit.getPreferredPlace(),
            habit.getPrecedingActivity(), habit.getSituation(), habit.getFallbackPlan(),
            habit.getStatus(), habit.getPositionIndex(),
            new ScheduleResponse(schedule.getType(), schedule.getStartDate(), weekdays,
                schedule.getIntervalDays(), schedule.getTargetCount(), schedule.getDueDate(),
                schedule.getCustomDescription()),
            habit.getPausedAt(), habit.getArchivedAt(), habit.getCreatedAt(),
            habit.getUpdatedAt()
        );
    }

    private GrowthHabit require(UUID userId, UUID id) {
        return habits.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                "Growth Habit was not found."));
    }

    private LifeArea requireArea(UUID userId, UUID id) {
        return areas.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                "Life Area was not found."));
    }
}
