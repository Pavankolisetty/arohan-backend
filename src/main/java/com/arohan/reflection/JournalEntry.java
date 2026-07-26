package com.arohan.reflection;

import com.arohan.shared.AuditableEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "journal_entry")
public class JournalEntry extends AuditableEntity {
    @Id private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 24)
    private ReflectionType entryType;
    @Column(length = 120) private String title;
    @Column(length = 4000) private String content;
    @Column(name = "entry_date", nullable = false) private LocalDate entryDate;
    @Column(name = "life_area_id") private UUID lifeAreaId;
    @Column(name = "habit_id") private UUID habitId;
    @Column(name = "mood_score", columnDefinition = "TINYINT") private Integer moodScore;
    @Column(name = "energy_score", columnDefinition = "TINYINT") private Integer energyScore;
    @Column(nullable = false) private boolean pinned;
    @Column(name = "period_start") private LocalDate periodStart;
    @Column(name = "period_end") private LocalDate periodEnd;
    @Column(length = 1000) private String wins;
    @Column(length = 1000) private String friction;
    @Column(name = "next_adjustment", length = 700) private String nextAdjustment;
    @Column(name = "small_commitment", length = 500) private String smallCommitment;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "journal_entry_tag", joinColumns = @JoinColumn(name = "entry_id"))
    @Column(name = "tag_id", nullable = false)
    private Set<UUID> tagIds = new LinkedHashSet<>();

    protected JournalEntry() {}

    public JournalEntry(UUID userId) {
        this.id = UUID.randomUUID();
        this.userId = userId;
    }

    public void update(ReflectionDtos.ReflectionRequest request) {
        entryType = request.entryType();
        title = clean(request.title());
        content = clean(request.content());
        entryDate = request.entryDate();
        lifeAreaId = request.lifeAreaId();
        habitId = request.habitId();
        moodScore = request.moodScore();
        energyScore = request.energyScore();
        pinned = request.pinned();
        periodStart = request.periodStart();
        periodEnd = request.periodEnd();
        wins = clean(request.wins());
        friction = clean(request.friction());
        nextAdjustment = clean(request.nextAdjustment());
        smallCommitment = clean(request.smallCommitment());
        tagIds.clear();
        tagIds.addAll(request.tagIds() == null ? Set.of() : request.tagIds());
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public ReflectionType getEntryType() { return entryType; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public LocalDate getEntryDate() { return entryDate; }
    public UUID getLifeAreaId() { return lifeAreaId; }
    public UUID getHabitId() { return habitId; }
    public Integer getMoodScore() { return moodScore; }
    public Integer getEnergyScore() { return energyScore; }
    public boolean isPinned() { return pinned; }
    public LocalDate getPeriodStart() { return periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public String getWins() { return wins; }
    public String getFriction() { return friction; }
    public String getNextAdjustment() { return nextAdjustment; }
    public String getSmallCommitment() { return smallCommitment; }
    public Set<UUID> getTagIds() { return Set.copyOf(tagIds); }
}
