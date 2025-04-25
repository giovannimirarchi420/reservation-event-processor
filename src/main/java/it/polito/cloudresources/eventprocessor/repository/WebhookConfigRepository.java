package it.polito.cloudresources.eventprocessor.repository;

import it.polito.cloudresources.eventprocessor.model.WebhookConfig;
import it.polito.cloudresources.eventprocessor.model.WebhookEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebhookConfigRepository extends JpaRepository<WebhookConfig, Long> {

    /**
     * Finds enabled webhooks relevant to a specific resource event.
     * A webhook is relevant if:
     * - It's enabled.
     * - It subscribes to the specific eventType or ALL event types.
     * - It's configured for the specific resource OR the resource's type OR neither (global).
     *
     * Note: This query assumes the WebhookConfig entity in this module has resourceId and resourceTypeId fields.
     * It mirrors the logic expected from reservation-be's repository.
     */
    @Query("SELECT wc FROM WebhookConfig wc JOIN Event e ON e.resource.id = :resourceId " +
           "WHERE wc.enabled = true " +
           "AND (wc.eventType = :eventType OR wc.eventType = it.polito.cloudresources.eventprocessor.model.WebhookEventType.ALL) " +
           "AND (" +
           "  (wc.resourceId = :resourceId) " +
           // Assuming Event entity has direct access to resource type ID or we join through Resource
           "  OR (wc.resourceTypeId = e.resource.type.id) " +
           "  OR (wc.resourceId IS NULL AND wc.resourceTypeId IS NULL) " +
           // Add siteId check if necessary and available
           ")")
    List<WebhookConfig> findRelevantWebhooksForResourceEvent(
            @Param("resourceId") Long resourceId,
            @Param("eventType") WebhookEventType eventType);

}
