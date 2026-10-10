package com.autoscientist.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "findings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Finding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_id", nullable = false)
    private Dataset dataset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "research_question_id")
    private ResearchQuestion researchQuestion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hypothesis_id")
    private Hypothesis hypothesis;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statistical_test_id")
    private StatisticalTest statisticalTest;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String summary;

    @Column(name = "evidence_text", nullable = false, columnDefinition = "LONGTEXT")
    private String evidenceText;

    @Column(name = "confidence_level", nullable = false, length = 50)
    private String confidenceLevel; // HIGH, MODERATE, WEAK, INSUFFICIENT

    @Column(name = "causality_disclaimer", nullable = false, columnDefinition = "LONGTEXT")
    @Builder.Default
    private String causalityDisclaimer = "Empirical findings reflect observed statistical associations and do not substantiate causal mechanisms without experimental intervention.";

    @OneToMany(mappedBy = "finding", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EvidenceNode> evidenceNodes = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.causalityDisclaimer == null) {
            this.causalityDisclaimer = "Empirical findings reflect observed statistical associations and do not substantiate causal mechanisms without experimental intervention.";
        }
    }
}
