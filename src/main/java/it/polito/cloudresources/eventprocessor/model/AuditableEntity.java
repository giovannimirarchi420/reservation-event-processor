package it.polito.cloudresources.eventprocessor.model;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import it.polito.cloudresources.eventprocessor.config.datetime.DateTimeConfig;

import java.time.ZonedDateTime;

/**
 * Base class for auditable entities. Copied from reservation-be.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class) // Requires @EnableJpaAuditing in config
@Getter
@Setter
public abstract class AuditableEntity {

    @CreatedDate
    private ZonedDateTime createdAt;

    @LastModifiedDate
    private ZonedDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        ZonedDateTime now = ZonedDateTime.now(DateTimeConfig.DEFAULT_ZONE_ID);
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = ZonedDateTime.now(DateTimeConfig.DEFAULT_ZONE_ID);
    }
}
