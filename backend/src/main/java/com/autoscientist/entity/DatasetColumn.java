package com.autoscientist.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dataset_columns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatasetColumn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_id", nullable = false)
    private Dataset dataset;

    @Column(name = "column_index", nullable = false)
    private Integer columnIndex;

    @Column(name = "column_name", nullable = false)
    private String columnName;

    @Column(name = "data_type", nullable = false, length = 50)
    private String dataType; // NUMERIC, CATEGORICAL, BOOLEAN, DATE_TIME, TEXT, UNKNOWN

    @Column(name = "type_confidence")
    @Builder.Default
    private Double typeConfidence = 1.0;

    @Column(name = "missing_count")
    @Builder.Default
    private Integer missingCount = 0;

    @Column(name = "missing_pct")
    @Builder.Default
    private Double missingPct = 0.0;

    @Column(name = "unique_count")
    @Builder.Default
    private Integer uniqueCount = 0;

    @Column(name = "min_val")
    private Double minVal;

    @Column(name = "max_val")
    private Double maxVal;

    @Column(name = "mean_val")
    private Double meanVal;

    @Column(name = "median_val")
    private Double medianVal;

    @Column(name = "std_dev")
    private Double stdDev;

    @Column(name = "variance")
    private Double variance;

    @Column(name = "q1")
    private Double q1;

    @Column(name = "q3")
    private Double q3;

    @Column(name = "skewness")
    private Double skewness;

    @Column(name = "categories_json", columnDefinition = "LONGTEXT")
    private String categoriesJson; // Top categories and frequencies formatted as JSON

    @Column(name = "distribution_json", columnDefinition = "LONGTEXT")
    private String distributionJson; // Binned histogram distribution formatted as JSON

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
