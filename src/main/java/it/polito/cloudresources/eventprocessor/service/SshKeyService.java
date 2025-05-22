package it.polito.cloudresources.eventprocessor.service;

import it.polito.cloudresources.eventprocessor.model.SshKey;
import it.polito.cloudresources.eventprocessor.repository.SshKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for managing SSH keys in the database
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SshKeyService {
    
    private final SshKeyRepository sshKeyRepository;
    
    /**
     * Get SSH key for a user
     * 
     * @param userId The Keycloak user ID
     * @return Optional containing the SSH key if found
     */
    public Optional<String> getUserSshKey(String userId) {
        return sshKeyRepository.findByUserId(userId)
                .map(SshKey::getSshKey);
    }
    
}
