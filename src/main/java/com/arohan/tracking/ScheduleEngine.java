package com.arohan.tracking;

import com.arohan.habit.GrowthHabit;
import com.arohan.habit.HabitSchedule;
import com.arohan.habit.ScheduleType;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

@Component
public class ScheduleEngine {
    public boolean isFixedEligible(GrowthHabit habit, HabitSchedule schedule, LocalDate date) {
        LocalDate first = firstEligibleDate(habit, schedule);
        LocalDate last = lastEligibleDate(habit);
        if (date.isBefore(first) || (last != null && date.isAfter(last))) return false;
        return switch (schedule.getType()) {
            case DAILY -> true;
            case SELECTED_WEEKDAYS -> schedule.getWeekdays() != null
                && java.util.Arrays.asList(schedule.getWeekdays().split(","))
                    .contains(date.getDayOfWeek().name());
            case ALTERNATE_DAYS, EVERY_N_DAYS, ROTATION ->
                ChronoUnit.DAYS.between(first, date) % schedule.getIntervalDays() == 0;
            case ONE_TIME -> schedule.getDueDate() != null
                && schedule.getDueDate().equals(date);
            case TIMES_PER_WEEK, TIMES_PER_MONTH, CUSTOM -> false;
        };
    }

    public LocalDate firstEligibleDate(GrowthHabit habit, HabitSchedule schedule) {
        return schedule.getStartDate().isAfter(habit.getTrackingEnabledFrom())
            ? schedule.getStartDate() : habit.getTrackingEnabledFrom();
    }

    public boolean isFlexible(ScheduleType type) {
        return type == ScheduleType.TIMES_PER_WEEK || type == ScheduleType.TIMES_PER_MONTH;
    }

    public LocalDate lastEligibleDate(GrowthHabit habit) {
        if (habit.getArchivedAt() != null) {
            return habit.getArchivedAt().atZone(ZoneOffset.UTC).toLocalDate();
        }
        if (habit.getPausedAt() != null) {
            return habit.getPausedAt().atZone(ZoneOffset.UTC).toLocalDate();
        }
        return null;
    }
}
