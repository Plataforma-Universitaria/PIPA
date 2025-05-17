package br.ueg.tc.pipa.generic;

import jakarta.persistence.*;
import org.joda.time.DateTime;

import java.util.UUID;

@MappedSuperclass
public abstract class GenericModel {

    @Column(updatable = false, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "created_at", updatable = false, nullable = false)
    private DateTime createdAt;

    @Column(name = "modified_at", nullable = false)
    private DateTime modifiedAt;

    @Column(name = "created_by", updatable = false, nullable = false)
    private UUID createdBy;

    @Column(name = "modified_by", nullable = false)
    private UUID modifiedBy;

    @PrePersist
    protected void onCreate() {
        DateTime now = new DateTime();
        this.createdAt = now;
        this.modifiedAt = now;

        UUID currentUser = getCurrentUserId();
        this.createdBy = currentUser;
        this.modifiedBy = currentUser;
    }

    @PreUpdate
    protected void onUpdate() {
        this.modifiedAt = new DateTime();
        this.modifiedBy = getCurrentUserId();
    }

    private UUID getCurrentUserId() {
        return UUID.fromString("00000000-0000-0000-0000-000000000000"); // placeholder
    }
}
