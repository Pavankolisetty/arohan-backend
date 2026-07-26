package com.arohan.habit;

import com.arohan.shared.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "growth_habit")
public class GrowthHabit extends AuditableEntity {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "life_area_id", nullable = false)
    private UUID lifeAreaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HabitKind kind;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 500)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "tracking_method", nullable = false, length = 20)
    private TrackingMethod trackingMethod;

    @Column(name = "target_value", precision = 19, scale = 4)
    private BigDecimal targetValue;

    @Column(name = "target_unit", length = 40)
    private String targetUnit;

    @Column(name = "cue_note", nullable = false, length = 300)
    private String cueNote;

    @Column(name = "two_minute_starter", nullable = false, length = 300)
    private String twoMinuteStarter;

    @Column(name = "preferred_time")
    private LocalTime preferredTime;

    @Column(name = "preferred_place", length = 160)
    private String preferredPlace;

    @Column(name = "preceding_activity", length = 200)
    private String precedingActivity;

    @Column(length = 200)
    private String situation;

    @Column(name = "fallback_plan", nullable = false, length = 400)
    private String fallbackPlan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private HabitStatus status;

    @Column(name = "position_index", nullable = false)
    private int positionIndex;

    @Column(name = "tracking_enabled_from", nullable = false)
    private LocalDate trackingEnabledFrom;

    @Column(name = "paused_at")
    private Instant pausedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    protected GrowthHabit() {}

    public GrowthHabit(UUID userId, HabitDtos.UpsertHabitRequest request) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.status = HabitStatus.ACTIVE;
        this.trackingEnabledFrom = LocalDate.now(ZoneOffset.UTC);
        update(request);
    }

    public void update(HabitDtos.UpsertHabitRequest request) {
        this.lifeAreaId = request.lifeAreaId();
        this.kind = request.kind();
        this.name = request.name().trim();
        this.purpose = request.purpose().trim();
        this.trackingMethod = request.trackingMethod();
        this.targetValue = request.targetValue();
        this.targetUnit = trimToNull(request.targetUnit());
        this.cueNote = request.cueNote().trim();
        this.twoMinuteStarter = request.twoMinuteStarter().trim();
        this.preferredTime = request.preferredTime();
        this.preferredPlace = trimToNull(request.preferredPlace());
        this.precedingActivity = trimToNull(request.precedingActivity());
        this.situation = trimToNull(request.situation());
        this.fallbackPlan = request.fallbackPlan().trim();
        this.positionIndex = request.positionIndex();
    }

    public void pause() {
        if (status == HabitStatus.ACTIVE) {
            status = HabitStatus.PAUSED;
            pausedAt = Instant.now();
        }
    }

    public void restart() {
        if (status != HabitStatus.ARCHIVED) {
            status = HabitStatus.ACTIVE;
            pausedAt = null;
        }
    }

    public void archive() {
        status = HabitStatus.ARCHIVED;
        archivedAt = Instant.now();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getLifeAreaId() { return lifeAreaId; }
    public HabitKind getKind() { return kind; }
    public String getName() { return name; }
    public String getPurpose() { return purpose; }
    public TrackingMethod getTrackingMethod() { return trackingMethod; }
    public BigDecimal getTargetValue() { return targetValue; }
    public String getTargetUnit() { return targetUnit; }
    public String getCueNote() { return cueNote; }
    public String getTwoMinuteStarter() { return twoMinuteStarter; }
    public LocalTime getPreferredTime() { return preferredTime; }
    public String getPreferredPlace() { return preferredPlace; }
    public String getPrecedingActivity() { return precedingActivity; }
    public String getSituation() { return situation; }
    public String getFallbackPlan() { return fallbackPlan; }
    public HabitStatus getStatus() { return status; }
    public int getPositionIndex() { return positionIndex; }
    public LocalDate getTrackingEnabledFrom() { return trackingEnabledFrom; }
    public Instant getPausedAt() { return pausedAt; }
    public Instant getArchivedAt() { return archivedAt; }
}
