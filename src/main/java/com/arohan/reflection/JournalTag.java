package com.arohan.reflection;

import com.arohan.shared.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "journal_tag")
public class JournalTag extends AuditableEntity {
    @Id private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(nullable = false, length = 40) private String name;
    @Column(name = "color_hex", nullable = false, length = 7) private String colorHex;

    protected JournalTag() {}

    public JournalTag(UUID userId, String name, String colorHex) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.name = name;
        this.colorHex = colorHex;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getName() { return name; }
    public String getColorHex() { return colorHex; }
}
