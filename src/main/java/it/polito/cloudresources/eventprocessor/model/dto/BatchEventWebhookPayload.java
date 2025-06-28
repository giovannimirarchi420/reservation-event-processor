package it.polito.cloudresources.eventprocessor.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.polito.cloudresources.eventprocessor.model.WebhookEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Payload structure for batch event-related webhooks.
 * Contains multiple events that belong to the same user.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // Exclude null fields from JSON
public class BatchEventWebhookPayload {
    private String webhookId;
    private WebhookEventType eventType;
    private ZonedDateTime timestamp;
    private int eventCount; // Number of events in the batch

    // User Information (common to all events)
    private String userId;
    private String username;
    private String email;
    private String sshPublicKey;

    // List of events
    private List<EventInfo> events;

    // Currently active resources for the user (resources that are currently booked/in use)
    private List<EventInfo> activeResources;

    /**
     * Individual event information within the batch
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EventInfo {
        private String eventId;
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
}
