package com.arohan.finance;

import com.arohan.shared.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "finance_bucket")
public class FinanceBucket extends AuditableEntity {
    @Id private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "system_key", nullable = false, length = 24) private String systemKey;
    @Column(nullable = false, length = 60) private String name;
    @Column(name = "color_hex", nullable = false, columnDefinition = "CHAR(7)")
    private String colorHex;
    @Column(name = "icon_key", nullable = false, length = 40) private String iconKey;
    @Column(name = "position_index", nullable = false) private int positionIndex;
    @Column(nullable = false) private boolean active;

    protected FinanceBucket() {}

    FinanceBucket(UUID userId, String systemKey, String name, String colorHex,
                  String iconKey, int positionIndex) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.systemKey = systemKey;
        this.name = name;
        this.colorHex = colorHex;
        this.iconKey = iconKey;
        this.positionIndex = positionIndex;
        this.active = true;
    }

    UUID getId() { return id; }
    UUID getUserId() { return userId; }
    String getSystemKey() { return systemKey; }
    String getName() { return name; }
    String getColorHex() { return colorHex; }
    String getIconKey() { return iconKey; }
    int getPositionIndex() { return positionIndex; }
    boolean isActive() { return active; }
    void update(String name, String colorHex, String iconKey, int positionIndex) {
        this.name = name;
        this.colorHex = colorHex;
        this.iconKey = iconKey;
        this.positionIndex = positionIndex;
    }
}
