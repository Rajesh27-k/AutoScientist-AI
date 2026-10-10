package com.autoscientist.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "evidence_nodes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvidenceNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finding_id", nullable = false)
    private Finding finding;

    @Column(name = "node_type", nullable = false, length = 100)
    private String nodeType; // DATASET, HYPOTHESIS, STAT_TEST, ML_MODEL

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "data_payload_json", columnDefinition = "LONGTEXT")
    private String dataPayloadJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
