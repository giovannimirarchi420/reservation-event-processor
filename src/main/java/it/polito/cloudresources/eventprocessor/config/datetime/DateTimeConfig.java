package it.polito.cloudresources.eventprocessor.config.datetime;

import java.time.ZoneId;

// Simple configuration class to hold the default ZoneId
// Copied and simplified from reservation-be
public class DateTimeConfig {
    public static final ZoneId DEFAULT_ZONE_ID = ZoneId.of("UTC");
}
