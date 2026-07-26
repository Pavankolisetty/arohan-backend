package com.arohan.lifearea;

import com.arohan.shared.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "life_area")
public class LifeArea extends AuditableEntity {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "color_hex", nullable = false, columnDefinition = "CHAR(7)")
    private String colorHex;

    @Column(name = "icon_key", nullable = false, length = 40)
    private String iconKey;

    @Column(name = "background_key", nullable = false, length = 40)
    private String backgroundKey;

    @Column(name = "background_image_url", length = 1000)
    private String backgroundImageUrl;

    @Column(name = "desired_importance", nullable = false, columnDefinition = "TINYINT")
    private int desiredImportance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private LifeAreaStatus status;

    @Column(name = "position_index", nullable = false)
    private int positionIndex;

    @Column(name = "archived_at")
    private Instant archivedAt;

    protected LifeArea() {}

    public LifeArea(UUID userId, UUID parentId, String name, String description,
                    String colorHex, String iconKey, String backgroundKey,
                    String backgroundImageUrl, int desiredImportance, int positionIndex) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.parentId = parentId;
        this.name = name;
        this.description = description;
        this.colorHex = colorHex;
        this.iconKey = iconKey;
        this.backgroundKey = backgroundKey;
        this.backgroundImageUrl = backgroundImageUrl;
        this.desiredImportance = desiredImportance;
        this.positionIndex = positionIndex;
        this.status = LifeAreaStatus.ACTIVE;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getParentId() { return parentId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getColorHex() { return colorHex; }
    public String getIconKey() { return iconKey; }
    public String getBackgroundKey() { return backgroundKey; }
    public String getBackgroundImageUrl() { return backgroundImageUrl; }
    public int getDesiredImportance() { return desiredImportance; }
    public LifeAreaStatus getStatus() { return status; }
    public int getPositionIndex() { return positionIndex; }
    public Instant getArchivedAt() { return archivedAt; }

    public void update(UUID parentId, String name, String description, String colorHex,
                       String iconKey, String backgroundKey, String backgroundImageUrl,
                       int desiredImportance, int positionIndex) {
        this.parentId = parentId;
        this.name = name;
        this.description = description;
        this.colorHex = colorHex;
        this.iconKey = iconKey;
        this.backgroundKey = backgroundKey;
        this.backgroundImageUrl = backgroundImageUrl;
        this.desiredImportance = desiredImportance;
        this.positionIndex = positionIndex;
    }

    public void archive() {
        status = LifeAreaStatus.ARCHIVED;
        archivedAt = Instant.now();
    }

    public void restore() {
        status = LifeAreaStatus.ACTIVE;
        archivedAt = null;
    }
}
