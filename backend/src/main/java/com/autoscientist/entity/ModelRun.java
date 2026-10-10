package com.autoscientist.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "models")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experiment_version_id", nullable = false)
    private ExperimentVersion experimentVersion;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(nullable = false, length = 100)
    private String algorithm;

    @Column(name = "hyperparameters_json", columnDefinition = "LONGTEXT")
    private String hyperparametersJson;

    @Column(name = "train_metrics_json", columnDefinition = "LONGTEXT")
    private String trainMetricsJson;

    @Column(name = "val_metrics_json", columnDefinition = "LONGTEXT")
    private String valMetricsJson;

    @Column(name = "test_metrics_json", columnDefinition = "LONGTEXT")
    private String testMetricsJson;

    @Column(name = "is_best_model")
    @Builder.Default
    private Boolean isBestModel = false;

    @Column(name = "fairness_metrics_json", columnDefinition = "LONGTEXT")
    private String fairnessMetricsJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
