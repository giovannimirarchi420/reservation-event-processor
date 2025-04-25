package it.polito.cloudresources.eventprocessor.model;

/**
 * Enumeration of supported webhook event types (mirrored from reservation-be).
 */
public enum WebhookEventType {
    EVENT_CREATED,
    EVENT_UPDATED,
    EVENT_DELETED,
    EVENT_START,       // Relevant for this service
    EVENT_END,         // Relevant for this service
    RESOURCE_CREATED,
    RESOURCE_UPDATED,
    RESOURCE_STATUS_CHANGED,
    RESOURCE_DELETED,
    ALL                // Relevant for this service
}
