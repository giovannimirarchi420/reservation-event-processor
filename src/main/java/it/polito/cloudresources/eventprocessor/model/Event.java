package it.polito.cloudresources.eventprocessor.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

import it.polito.cloudresources.eventprocessor.config.datetime.DateTimeConfig;

/**
 * Event entity. Copied from reservation-be.
 * Added 'startNotifiedAt' and 'endNotifiedAt' fields to track processing status for start/end events.
 */
@Entity
@Table(name = "events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Event extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    private String title;

    @Size(max = 500)
    private String description;

    @NotNull
    @Column(name = "start_time")
    private ZonedDateTime start;

    @NotNull
    @Column(name = "end_time")
    private ZonedDateTime end;

    @ManyToOne(fetch = FetchType.LAZY) // Lazy fetch resource
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @NotBlank
    @Column(name = "keycloak_id")
    private String keycloakId; // Keycloak user ID

    // Field to mark when the start notification was sent
    @Column(name = "start_notified_at")
    private ZonedDateTime startNotifiedAt;

    // Field to mark when the end notification was sent
    @Column(name = "end_notified_at")
    private ZonedDateTime endNotifiedAt;

    /**
     * Pre-persist hook to ensure start and end dates have correct timezone.
     */
    @PrePersist
    @Override
    public void prePersist() {
        super.prePersist();
        ensureCorrectTimezone();
    }

    /**
     * Pre-update hook to ensure start and end dates have correct timezone.
     */
    @PreUpdate
    @Override
    public void preUpdate() {
        super.preUpdate();
        ensureCorrectTimezone();
    }

    private void ensureCorrectTimezone() {
        if (start != null) {
            start = start.withZoneSameInstant(DateTimeConfig.DEFAULT_ZONE_ID);
        }
        if (end != null) {
            end = end.withZoneSameInstant(DateTimeConfig.DEFAULT_ZONE_ID);
        }
    }

    // Avoid issues with Lombok and circular dependencies
    @Override
    public String toString() {
        return "Event{" +
               "id=" + id +
               ", title='" + title + '\'' +
               ", start=" + start +
               ", end=" + end +
               ", resourceId=" + (resource != null ? resource.getId() : null) +
               ", keycloakId='" + keycloakId + '\'' +
               ", startNotifiedAt=" + startNotifiedAt +
               ", endNotifiedAt=" + endNotifiedAt +
               '}';
    }
}
