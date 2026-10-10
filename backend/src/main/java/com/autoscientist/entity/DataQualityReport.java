package com.autoscientist.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "data_quality_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataQualityReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_id", nullable = false, unique = true)
    private Dataset dataset;

    @Column(name = "overall_score", nullable = false)
    private Double overallScore;

    @Column(name = "missing_value_score", nullable = false)
    private Double missingValueScore;

    @Column(name = "duplicate_score", nullable = false)
    private Double duplicateScore;

    @Column(name = "outlier_score", nullable = false)
    private Double outlierScore;

    @Column(name = "consistency_score", nullable = false)
    private Double consistencyScore;

    @Column(name = "total_issues", nullable = false)
    @Builder.Default
    private Integer totalIssues = 0;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DataQualityIssue> issues = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
