package com.arohan.tracking;

import com.arohan.shared.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "habit_tracking_entry")
public class HabitTrackingEntry extends AuditableEntity {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "habit_id", nullable = false)
    private UUID habitId;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private TrackingStatus status;

    @Column(name = "actual_value", precision = 19, scale = 4)
    private BigDecimal actualValue;

    @Column(name = "quality_rating", columnDefinition = "TINYINT")
    private Integer qualityRating;

    @Column(length = 1000)
    private String reflection;

    @Column(name = "friction_note", length = 300)
    private String frictionNote;

    @Column(name = "cue_started_at")
    private Instant cueStartedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected HabitTrackingEntry() {}

    public HabitTrackingEntry(UUID userId, UUID habitId, LocalDate scheduledDate) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.habitId = habitId;
        this.scheduledDate = scheduledDate;
    }

    public void recordCueStart() {
        if (cueStartedAt == null) cueStartedAt = Instant.now();
    }

    public void record(TrackingDtos.RecordPracticeRequest request) {
        this.status = request.status();
        this.actualValue = request.actualValue();
        this.qualityRating = request.qualityRating();
        this.reflection = trimToNull(request.reflection());
        this.frictionNote = trimToNull(request.frictionNote());
        this.completedAt = Instant.now();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getHabitId() { return habitId; }
    public LocalDate getScheduledDate() { return scheduledDate; }
    public TrackingStatus getStatus() { return status; }
    public BigDecimal getActualValue() { return actualValue; }
    public Integer getQualityRating() { return qualityRating; }
    public String getReflection() { return reflection; }
    public String getFrictionNote() { return frictionNote; }
    public Instant getCueStartedAt() { return cueStartedAt; }
    public Instant getCompletedAt() { return completedAt; }
}
