package com.autoscientist.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "research_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResearchQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_id", nullable = false)
    private Dataset dataset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "target_variable")
    private String targetVariable;

    @Column(name = "predictor_variable")
    private String predictorVariable;

    @Column(name = "question_type", nullable = false, length = 50)
    @Builder.Default
    private String questionType = "ASSOCIATION"; // ASSOCIATION, DIFFERENCE, PREDICTION

    @Column(name = "suggested_test", length = 100)
    private String suggestedTest;

    @Column(length = 50)
    @Builder.Default
    private String status = "ACTIVE";

    @OneToMany(mappedBy = "researchQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Hypothesis> hypotheses = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = "ACTIVE";
    }
}
