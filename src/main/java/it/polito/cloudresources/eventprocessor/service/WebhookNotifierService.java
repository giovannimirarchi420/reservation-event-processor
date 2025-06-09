package it.polito.cloudresources.eventprocessor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polito.cloudresources.eventprocessor.model.Event;
import it.polito.cloudresources.eventprocessor.model.Resource;
import it.polito.cloudresources.eventprocessor.model.WebhookConfig;
import it.polito.cloudresources.eventprocessor.model.WebhookEventType;
import it.polito.cloudresources.eventprocessor.model.dto.EventWebhookPayload;
import it.polito.cloudresources.eventprocessor.model.dto.BatchEventWebhookPayload;
import it.polito.cloudresources.eventprocessor.repository.WebhookConfigRepository;
import it.polito.cloudresources.eventprocessor.repository.EventRepository;
import it.polito.cloudresources.eventprocessor.util.DateTimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets; // Import StandardCharsets
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookNotifierService {

    private final WebhookConfigRepository webhookConfigRepository;
    private final EventRepository eventRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final DateTimeUtils dateTimeUtils;
    private final KeycloakService keycloakService;
    private final SshKeyService sshService;

    /**
     * Notify webhooks with a batch of events belonging to the same user.
     * All events in the list must belong to the same user (same keycloakId).
     * If events belong to different users, the notification will be rejected.
     * 
     * @param eventType The type of webhook event (EVENT_START, EVENT_END, etc.)
     * @param events List of events belonging to the same user
     */
    @Async // Execute webhook calls asynchronously
    public void notify(WebhookEventType eventType, List<Event> events) {
        if (events == null || events.isEmpty()) {
            log.debug("No events provided for notification");
            return;
        }

        // Verify all events belong to the same user
        String firstUserKeycloakId = events.get(0).getKeycloakId();
        boolean allSameUser = events.stream()
                .allMatch(event -> firstUserKeycloakId.equals(event.getKeycloakId()));
        
        if (!allSameUser) {
            log.error("All events in the batch must belong to the same user. Rejecting notification.");
            return;
        }

        log.debug("Searching webhooks for event type {} and {} events for user {}", 
                  eventType, events.size(), firstUserKeycloakId);

        // Find webhooks subscribed to this event type for any of the resources in the batch
        List<Long> resourceIds = events.stream()
                .map(event -> event.getResource().getId())
                .distinct()
                .toList();

        List<WebhookConfig> relevantWebhooks = findRelevantWebhooksForEvents(resourceIds, eventType);

        if (relevantWebhooks.isEmpty()) {
            log.debug("No relevant webhooks found for {} events", events.size());
            return;
        }

        log.info("Found {} relevant webhooks for {} events", relevantWebhooks.size(), events.size());

        for (WebhookConfig webhook : relevantWebhooks) {
            if (!webhook.isEnabled()) {
                log.debug("Skipping disabled webhook: {}", webhook.getName());
                continue;
            }
            try {
                sendBatchWebhook(webhook, eventType, events);
            } catch (Exception e) {
                // Log error but continue processing other webhooks
                log.error("Error sending webhook {} for {} events: {}", 
                         webhook.getName(), events.size(), e.getMessage(), e);
                // Consider adding retry logic or queuing failed attempts if needed
            }
        }
    }

    /**
     * Notify webhooks with a single event (backward compatibility method).
     * This method wraps the single event in a list and calls the batch notify method.
     * 
     * @param eventType The type of webhook event
     * @param event Single event to notify
     */
    // Keep the old single-event notify method for backward compatibility
    @Async
    public void notify(WebhookEventType eventType, Event event) {
        notify(eventType, List.of(event));
    }

    private List<WebhookConfig> findRelevantWebhooksForEvents(List<Long> resourceIds, WebhookEventType eventType) {
        // Find webhooks for all resources in the batch - combine results and deduplicate
        return resourceIds.stream()
                .flatMap(resourceId -> webhookConfigRepository.findRelevantWebhooksForResourceEvent(resourceId, eventType).stream())
                .distinct()
                .toList();
    }

    private void sendBatchWebhook(WebhookConfig webhook, WebhookEventType eventType, List<Event> events) throws JsonProcessingException {
        BatchEventWebhookPayload payload = createBatchPayload(eventType, events);
        log.debug("Batch payload for webhook {}: {}", webhook.getName(), payload);
        String payloadJson = objectMapper.writeValueAsString(payload);
        log.debug("Batch payload JSON for webhook {}: {}", webhook.getName(), payloadJson);
        HttpHeaders headers = createHeaders(webhook, payloadJson);

        HttpEntity<String> entity = new HttpEntity<>(payloadJson, headers);

        log.info("Sending batch webhook '{}' for event type {} with {} events to URL: {}", 
                 webhook.getName(), eventType, events.size(), webhook.getUrl());

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    webhook.getUrl(),
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Batch webhook '{}' sent successfully for {} events. Status: {}", 
                         webhook.getName(), events.size(), response.getStatusCode());
            } else {
                log.warn("Batch webhook '{}' for {} events failed. Status: {}, Response: {}", 
                         webhook.getName(), events.size(), response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("Failed to send batch webhook '{}' for {} events: {}", 
                     webhook.getName(), events.size(), e.getMessage());
            throw e; // Re-throw to be caught by the caller for potential retries
        }
    }

    private BatchEventWebhookPayload createBatchPayload(WebhookEventType eventType, List<Event> events) {
        if (events == null || events.isEmpty()) {
            throw new IllegalArgumentException("Events list cannot be null or empty");
        }

        // Get user details from the first event (all events belong to the same user)
        Event firstEvent = events.get(0);
        String keycloakId = firstEvent.getKeycloakId();
        
        String sshPublicKey = null;
        String username = null;
        String email = null;

        // Fetch user details from Keycloak
        try {
            Optional<UserRepresentation> userOpt = keycloakService.getUserById(keycloakId);
            if (userOpt.isPresent()) {
                UserRepresentation user = userOpt.get();
                username = user.getUsername();
                email = user.getEmail();
                log.debug("Found user details for {}: username={}, email={}", keycloakId, username, email);
            } else {
                log.warn("User details not found for Keycloak ID: {}", keycloakId);
            }
        } catch (Exception e) {
            log.error("Error fetching user details for user {}: {}", keycloakId, e.getMessage());
        }

        // Fetch user SSH key from Keycloak
        try {
            Optional<String> sshKeyOpt = sshService.getUserSshKey(keycloakId);
            if (sshKeyOpt.isPresent()) {
                sshPublicKey = sshKeyOpt.get();
                log.debug("Found SSH key for user {}", keycloakId);
            } else {
                log.debug("No SSH key found for user {}", keycloakId);
            }
        } catch (Exception e) {
            log.error("Error fetching SSH key for user {}: {}", keycloakId, e.getMessage());
        }

        // Create event info list
        List<BatchEventWebhookPayload.EventInfo> eventInfoList = events.stream()
                .map(event -> createEventInfo(event))
                .toList();

        // Fetch currently active resources for the user
        ZonedDateTime currentTime = dateTimeUtils.ensureTimeZone(ZonedDateTime.now());
        List<BatchEventWebhookPayload.EventInfo> activeResourcesList = null;
        
        try {
            List<Event> activeEvents = eventRepository.findActiveEventsForUser(keycloakId, currentTime);
            activeResourcesList = activeEvents.stream()
                    .map(event -> createEventInfo(event))
                    .toList();
            log.debug("Found {} active resources for user {}", activeResourcesList.size(), keycloakId);
        } catch (Exception e) {
            log.error("Error fetching active resources for user {}: {}", keycloakId, e.getMessage());
            // Continue without active resources if there's an error
        }

        // Build the batch payload
        return BatchEventWebhookPayload.builder()
                .eventType(eventType)
                .timestamp(currentTime)
                .eventCount(events.size())
                .userId(keycloakId)
                .username(username)
                .email(email)
                .sshPublicKey(sshPublicKey)
                .events(eventInfoList)
                .activeResources(activeResourcesList)
                .build();
    }

    private BatchEventWebhookPayload.EventInfo createEventInfo(Event event) {
        Resource resource = event.getResource();
        String siteName = null;

        // Fetch site name from Keycloak using siteId from the resource
        if (resource != null && resource.getSiteId() != null) {
            try {
                Optional<String> siteNameOpt = keycloakService.getGroupNameById(resource.getSiteId());
                if (siteNameOpt.isPresent()) {
                    siteName = siteNameOpt.get();
                    log.debug("Found site name '{}' for site ID {}", siteName, resource.getSiteId());
                } else {
                    log.warn("Site name not found for site ID: {}", resource.getSiteId());
                }
            } catch (Exception e) {
                log.error("Error fetching site name for site ID {}: {}", resource.getSiteId(), e.getMessage());
            }
        }

        BatchEventWebhookPayload.EventInfo.EventInfoBuilder eventInfoBuilder = BatchEventWebhookPayload.EventInfo.builder()
                .eventId(event.getId().toString())
                .eventTitle(event.getTitle())
                .eventDescription(event.getDescription())
                .eventStart(event.getStart())
                .eventEnd(event.getEnd());

        if (resource != null) {
            eventInfoBuilder = eventInfoBuilder
                    .resourceId(resource.getId())
                    .resourceName(resource.getName())
                    .resourceSpecs(resource.getSpecs())
                    .resourceLocation(resource.getLocation())
                    .siteId(resource.getSiteId())
                    .siteName(siteName);
            if (resource.getType() != null) {
                eventInfoBuilder = eventInfoBuilder.resourceType(resource.getType().getName());
            }
        }

        return eventInfoBuilder.build();
    }

    private void sendWebhook(WebhookConfig webhook, WebhookEventType eventType, Event event) throws JsonProcessingException {
        EventWebhookPayload payload = createPayload(eventType, event);
        log.debug(null, "Payload for webhook {}: {}", webhook.getName(), payload);
        String payloadJson = objectMapper.writeValueAsString(payload);
        log.debug("Payload JSON for webhook {}: {}", webhook.getName(), payloadJson);
        HttpHeaders headers = createHeaders(webhook, payloadJson);

        HttpEntity<String> entity = new HttpEntity<>(payloadJson, headers);

        log.info("Sending webhook '{}' for event type {} to URL: {} with body: {}", webhook.getName(), eventType, webhook.getUrl(), entity.getBody());

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    webhook.getUrl(),
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Webhook '{}' sent successfully for event ID {}. Status: {}", webhook.getName(), event.getId(), response.getStatusCode());
            } else {
                log.warn("Webhook '{}' for event ID {} failed. Status: {}, Response: {}", webhook.getName(), event.getId(), response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("Failed to send webhook '{}' for event ID {}: {}", webhook.getName(), event.getId(), e.getMessage());
            throw e; // Re-throw to be caught by the caller for potential retries
        }
    }

    private EventWebhookPayload createPayload(WebhookEventType eventType, Event event) {
        String sshPublicKey = null;
        String username = null;
        String email = null;
        String siteName = null;

        // Fetch user details from Keycloak
        try {
            Optional<UserRepresentation> userOpt = keycloakService.getUserById(event.getKeycloakId());
            if (userOpt.isPresent()) {
                UserRepresentation user = userOpt.get();
                username = user.getUsername();
                email = user.getEmail();
                log.debug("Found user details for {}: username={}, email={}", event.getKeycloakId(), username, email);
            } else {
                log.warn("User details not found for Keycloak ID: {}", event.getKeycloakId());
            }
        } catch (Exception e) {
            log.error("Error fetching user details for user {}: {}", event.getKeycloakId(), e.getMessage());
        }

        // Fetch user SSH key from Keycloak
        try {
            Optional<String> sshKeyOpt = sshService.getUserSshKey(event.getKeycloakId());
            if (sshKeyOpt.isPresent()) {
                sshPublicKey = sshKeyOpt.get(); // Use the new variable name
                log.debug("Found SSH key for user {}", event.getKeycloakId());
            } else {
                log.debug("No SSH key found for user {}", event.getKeycloakId());
            }
        } catch (Exception e) {
            log.error("Error fetching SSH key for user {}: {}", event.getKeycloakId(), e.getMessage());
        }

        // Fetch site name from Keycloak using siteId from the resource
        Resource resource = event.getResource();
        if (resource != null && resource.getSiteId() != null) {
            try {
                Optional<String> siteNameOpt = keycloakService.getGroupNameById(resource.getSiteId());
                if (siteNameOpt.isPresent()) {
                    siteName = siteNameOpt.get();
                    log.debug("Found site name '{}' for site ID {}", siteName, resource.getSiteId());
                } else {
                    log.warn("Site name not found for site ID: {}", resource.getSiteId());
                }
            } catch (Exception e) {
                log.error("Error fetching site name for site ID {}: {}", resource.getSiteId(), e.getMessage());
            }
        }

        // Build the flattened payload
        EventWebhookPayload.EventWebhookPayloadBuilder payloadBuilder = EventWebhookPayload.builder()
                .eventType(eventType)
                .timestamp(dateTimeUtils.ensureTimeZone(ZonedDateTime.now()))
                .eventId(event.getId().toString())
                .userId(event.getKeycloakId())
                .username(username)
                .email(email)
                .sshPublicKey(sshPublicKey)
                .eventTitle(event.getTitle())
                .eventDescription(event.getDescription())
                .eventStart(event.getStart())
                .eventEnd(event.getEnd());

        if (resource != null) {
            payloadBuilder = payloadBuilder
                    .resourceId(resource.getId())
                    .resourceName(resource.getName())
                    .resourceSpecs(resource.getSpecs())
                    .resourceLocation(resource.getLocation())
                    .siteId(resource.getSiteId())
                    .siteName(siteName); // Add fetched site name
            if (resource.getType() != null) {
                payloadBuilder = payloadBuilder.resourceType(resource.getType().getName());
            }
        }

        return payloadBuilder.build();
    }

    private HttpHeaders createHeaders(WebhookConfig webhook, String payloadJson) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (webhook.getSecret() != null && !webhook.getSecret().isEmpty()) {
            try {
                Mac sha256Hmac = Mac.getInstance("HmacSHA256");
                // Use UTF-8 for the secret bytes
                SecretKeySpec secretKeySpec = new SecretKeySpec(webhook.getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
                sha256Hmac.init(secretKeySpec);
                // Use UTF-8 for the payload bytes
                byte[] hash = sha256Hmac.doFinal(payloadJson.getBytes(StandardCharsets.UTF_8));
                String signature = new String(Base64.getEncoder().encode(hash), StandardCharsets.UTF_8);
                headers.add("X-Webhook-Signature", signature);
                log.debug("Added signature header for webhook {}", webhook.getName());
            } catch (Exception e) {
                log.error("Error generating HMAC signature for webhook {}: {}", webhook.getName(), e.getMessage(), e);
                // Proceed without signature if generation fails
            }
        }
        return headers;
    }
}
