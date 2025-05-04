package it.polito.cloudresources.eventprocessor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Represents a webhook configuration (mirrored from reservation-be).
 */
@Entity
@Table(name = "webhook_configs") // Assuming same table name as in reservation-be
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false) // Adjust if inheriting from a base class
public class WebhookConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Assuming ID is managed by reservation-be, not generated here

    private String name;

    private String url;

    @Enumerated(EnumType.STRING)
    private WebhookEventType eventType;

    private String secret;

    private boolean enabled = true;

    // Simplified relationship mapping - assuming we only need the ID
    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "resource_type_id")
    private Long resourceTypeId;

    // Add other fields if needed by the query (e.g., siteId)
    // private String siteId;

    // Note: This entity is read-only in this service. No need for full relationship mapping.
}
