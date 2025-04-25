package it.polito.cloudresources.eventprocessor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.GroupResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for interacting with Keycloak to retrieve user information
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakService {

    public static final String ATTR_SSH_KEY = "ssh_key";

    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    // Admin credentials
    @Value("${keycloak.admin.username:admin}")
    private String adminUsername;

    @Value("${keycloak.admin.password:admin}")
    private String adminPassword;

    /**
     * Creates an admin Keycloak client
     */
    protected Keycloak getKeycloakClient() {
        return KeycloakBuilder.builder()
                .serverUrl(authServerUrl)
                .realm("master")
                .clientId("admin-cli")
                .username(adminUsername)
                .password(adminPassword)
                .build();
    }

    /**
     * Get the realm resource
     */
    protected RealmResource getRealmResource() {
        return getKeycloakClient().realm(realm);
    }

    /**
     * Get user representation by ID
     * @param userId The Keycloak user ID
     * @return Optional containing the UserRepresentation if found
     */
    public Optional<UserRepresentation> getUserById(String userId) {
        try {
            log.debug("Fetching user representation for user ID '{}'", userId);
            UserResource userResource = getRealmResource().users().get(userId);
            UserRepresentation user = userResource.toRepresentation();
            return Optional.ofNullable(user);
        } catch (Exception e) {
            log.error("Error fetching user representation from Keycloak for user {}", userId, e);
            return Optional.empty();
        }
    }

    /**
     * Get SSH key for a user
     * @param userId The Keycloak user ID
     * @return Optional containing the SSH key if found
     */
    public Optional<String> getUserSshKey(String userId) {
        try {
            log.debug("Fetching SSH key for user ID '{}'", userId);
            UserResource userResource = getRealmResource().users().get(userId);
            UserRepresentation user = userResource.toRepresentation();
            
            Map<String, List<String>> attributes = user.getAttributes();
            if (attributes != null && attributes.containsKey(ATTR_SSH_KEY)) {
                List<String> values = attributes.get(ATTR_SSH_KEY);
                if (!values.isEmpty()) {
                    return Optional.of(values.get(0));
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error fetching user SSH key from Keycloak for user {}", userId, e);
            return Optional.empty();
        }
    }

    /**
     * Get group name by ID (Site Name)
     * @param groupId The Keycloak group ID (siteId)
     * @return Optional containing the group name if found
     */
    public Optional<String> getGroupNameById(String groupId) {
        try {
            log.debug("Fetching group name for group ID '{}'", groupId);
            GroupResource groupResource = getRealmResource().groups().group(groupId);
            GroupRepresentation group = groupResource.toRepresentation();
            if (group != null) {
                return Optional.ofNullable(group.getName());
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error fetching group name from Keycloak for group ID {}", groupId, e);
            return Optional.empty();
        }
    }
}