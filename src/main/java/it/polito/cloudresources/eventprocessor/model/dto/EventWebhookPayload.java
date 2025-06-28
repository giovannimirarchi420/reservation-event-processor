package it.polito.cloudresources.eventprocessor.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.polito.cloudresources.eventprocessor.model.WebhookEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * Payload structure for event-related webhooks.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // Exclude null fields from JSON
public class EventWebhookPayload {
    private WebhookEventType eventType;
    private ZonedDateTime timestamp;
    private String eventId; // Renamed from webhookId to eventId for clarity

    // User Information
    private String userId;
    private String username;
    private String email;
    
    private String sshPublicKey;

    // Event Information
    private String eventTitle;
    private String eventDescription;
    private ZonedDateTime eventStart;
    private ZonedDateTime eventEnd;
    private String customParameters; // JSON string of custom parameter values

    // Resource Information
    private Long resourceId;
    private String resourceName;
    private String resourceType;
    private String resourceSpecs;
    private String resourceLocation;

    // Site Information
    private String siteId;
    private String siteName;
}
