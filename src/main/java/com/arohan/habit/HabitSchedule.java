package com.arohan.habit;

import com.arohan.shared.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "habit_schedule")
public class HabitSchedule extends AuditableEntity {
    @Id
    private UUID id;

    @Column(name = "habit_id", nullable = false, unique = true)
    private UUID habitId;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false, length = 28)
    private ScheduleType type;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(length = 80)
    private String weekdays;

    @Column(name = "interval_days")
    private Integer intervalDays;

    @Column(name = "target_count")
    private Integer targetCount;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "custom_description", length = 500)
    private String customDescription;

    protected HabitSchedule() {}

    public HabitSchedule(UUID habitId, HabitDtos.ScheduleRequest request) {
        this.id = UUID.randomUUID();
        this.habitId = habitId;
        update(request);
    }

    public void update(HabitDtos.ScheduleRequest request) {
        this.type = request.type();
        this.startDate = request.startDate();
        this.weekdays = request.weekdays() == null || request.weekdays().isEmpty()
            ? null : request.weekdays().stream().sorted().map(Enum::name)
                .collect(java.util.stream.Collectors.joining(","));
        this.intervalDays = request.intervalDays();
        this.targetCount = request.targetCount();
        this.dueDate = request.dueDate();
        this.customDescription = request.customDescription() == null
            || request.customDescription().isBlank() ? null : request.customDescription().trim();
    }

    public UUID getId() { return id; }
    public UUID getHabitId() { return habitId; }
    public ScheduleType getType() { return type; }
    public LocalDate getStartDate() { return startDate; }
    public String getWeekdays() { return weekdays; }
    public Integer getIntervalDays() { return intervalDays; }
    public Integer getTargetCount() { return targetCount; }
    public LocalDate getDueDate() { return dueDate; }
    public String getCustomDescription() { return customDescription; }
}
