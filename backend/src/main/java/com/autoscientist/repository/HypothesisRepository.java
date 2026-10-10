package com.autoscientist.repository;

import com.autoscientist.entity.Hypothesis;
import com.autoscientist.entity.ResearchQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HypothesisRepository extends JpaRepository<Hypothesis, Long> {
    List<Hypothesis> findByResearchQuestionOrderByRankOrderAsc(ResearchQuestion researchQuestion);
}
