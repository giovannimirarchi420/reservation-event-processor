package it.polito.cloudresources.eventprocessor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * Resource entity representing physical or virtual resources that can be booked
 */
@Entity
@Table(name = "resources")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Resource extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Resource parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private Set<Resource> subResources = new HashSet<>();

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Size(max = 255)
    private String specs;

    @NotBlank
    @Size(max = 100)
    private String location;

    @Enumerated(EnumType.STRING)
    private ResourceStatus status = ResourceStatus.ACTIVE;

    @ManyToOne
    @JoinColumn(name = "type_id", nullable = false)
    private ResourceType type;

    @OneToMany(mappedBy = "resource", cascade = CascadeType.ALL)
    private Set<Event> events = new HashSet<>();

    @NotBlank
    private String siteId; // Keycloak Group ID representing the site

}