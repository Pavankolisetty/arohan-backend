package com.arohan.finance;

import com.arohan.shared.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "finance_category")
public class FinanceCategory extends AuditableEntity {
    @Id private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "bucket_id", nullable = false) private UUID bucketId;
    @Column(nullable = false, length = 80) private String name;
    @Column(name = "position_index", nullable = false) private int positionIndex;
    @Column(nullable = false) private boolean active;

    protected FinanceCategory() {}

    FinanceCategory(UUID userId, UUID bucketId, String name, int positionIndex) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.bucketId = bucketId;
        this.name = name;
        this.positionIndex = positionIndex;
        this.active = true;
    }

    UUID getId() { return id; }
    UUID getUserId() { return userId; }
    UUID getBucketId() { return bucketId; }
    String getName() { return name; }
    int getPositionIndex() { return positionIndex; }
    boolean isActive() { return active; }
    void update(String name, UUID bucketId, int positionIndex, boolean active) {
        this.name = name;
        this.bucketId = bucketId;
        this.positionIndex = positionIndex;
        this.active = active;
    }
}
