package com.autoscientist.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hypotheses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hypothesis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "research_question_id", nullable = false)
    private ResearchQuestion researchQuestion;

    @Column(name = "null_hypothesis", nullable = false, columnDefinition = "TEXT")
    private String nullHypothesis;

    @Column(name = "alt_hypothesis", nullable = false, columnDefinition = "TEXT")
    private String altHypothesis;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String rationale;

    @Column(name = "suggested_test", nullable = false, length = 100)
    private String suggestedTest;

    @Column(nullable = false)
    private String var1;

    @Column
    private String var2;

    @Column
    @Builder.Default
    private Double confidence = 0.95;

    @Column(name = "rank_order")
    @Builder.Default
    private Integer rankOrder = 1;

    @OneToMany(mappedBy = "hypothesis", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StatisticalTest> statisticalTests = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
