package it.polito.cloudresources.eventprocessor.repository;

import it.polito.cloudresources.eventprocessor.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * Finds events that are starting or ending within the given time window and have not been processed yet.
     * An event's start is considered unprocessed if startNotifiedAt is null.
     * An event's end is considered unprocessed if endNotifiedAt is null.
     *
     * @param windowStart The start of the time window.
     * @param windowEnd   The end of the time window.
     * @return A list of unprocessed events within the window.
     */
    @Query("SELECT e FROM Event e WHERE " +
           "(e.startNotifiedAt IS NULL AND e.start >= :windowStart AND e.start <= :windowEnd) OR " +
           "(e.endNotifiedAt IS NULL AND e.end >= :windowStart AND e.end <= :windowEnd)")
    List<Event> findUnprocessedEventsInWindow(@Param("windowStart") ZonedDateTime windowStart,
                                              @Param("windowEnd") ZonedDateTime windowEnd);

    /**
     * Find events that start within a date range and haven't had their start notification sent.
     */
    @Query("SELECT e FROM Event e WHERE e.startNotifiedAt IS NULL AND e.start >= :startDate AND e.start <= :endDate")
    List<Event> findUnprocessedEventsStartingBetween(
            @Param("startDate") ZonedDateTime startDate,
            @Param("endDate") ZonedDateTime endDate);

    /**
     * Find events that end within a date range and haven't had their end notification sent.
     */
    @Query("SELECT e FROM Event e WHERE e.endNotifiedAt IS NULL AND e.end >= :startDate AND e.end <= :endDate")
    List<Event> findUnprocessedEventsEndingBetween(
            @Param("startDate") ZonedDateTime startDate,
            @Param("endDate") ZonedDateTime endDate);

    /**
     * Find currently active events for a user (events that have started but not yet ended).
     */
    @Query("SELECT e FROM Event e WHERE e.keycloakId = :keycloakId AND e.start <= :currentTime AND e.end > :currentTime")
    List<Event> findActiveEventsForUser(
            @Param("keycloakId") String keycloakId,
            @Param("currentTime") ZonedDateTime currentTime);
}
