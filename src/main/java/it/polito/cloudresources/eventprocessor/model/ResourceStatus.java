package it.polito.cloudresources.eventprocessor.model;

/**
 * Enumeration of possible resource statuses. Copied from reservation-be.
 */
public enum ResourceStatus {
    ACTIVE,     // Resource is operational and available for booking
    MAINTENANCE, // Resource is under maintenance
    UNAVAILABLE  // Resource is not available for booking
}
