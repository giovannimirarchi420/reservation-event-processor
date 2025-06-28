package it.polito.cloudresources.eventprocessor.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Resource type entity. Copied from reservation-be.
 * Note: Relationships like @OneToMany(mappedBy = "type") are removed as they are not needed for reading.
 */
@Entity
@Table(name = "resource_types")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ResourceType extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 50)
    private String name;

    @Size(max = 7)
    private String color;

    // @OneToMany(mappedBy = "type", cascade = CascadeType.ALL)
    // private Set<Resource> resources = new HashSet<>(); // Removed, not needed for reading

    @Column(name = "site_id")
    @NotBlank
    private String siteId; // Keycloak Group ID representing the site

    @Column(name = "custom_parameters", columnDefinition = "TEXT")
    private String customParameters; // JSON string storing custom parameter definitions

}
