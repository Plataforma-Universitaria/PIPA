package br.ueg.tc.pipa.infra.generics;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.experimental.SuperBuilder;
import org.joda.time.LocalDateTime;

import java.util.UUID;

@MappedSuperclass
public abstract class GenericModel {

    @Column(updatable = false, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "modified_at", nullable = false)
    private LocalDateTime modifiedAt;

    @Column(name = "created_by", updatable = false, nullable = false)
    private UUID createdBy;

    @Column(name = "modified_by", nullable = false)
    private UUID modifiedBy;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = new LocalDateTime();
        this.createdAt = now;
        this.modifiedAt = now;

        UUID currentUser = getCurrentUserId();
        this.createdBy = currentUser;
        this.modifiedBy = currentUser;
    }

    @PreUpdate
    protected void onUpdate() {
        this.modifiedAt = new LocalDateTime();
        this.modifiedBy = getCurrentUserId();
    }

    private UUID getCurrentUserId() {
        return UUID.fromString("00000000-0000-0000-0000-000000000000");
    }
}
