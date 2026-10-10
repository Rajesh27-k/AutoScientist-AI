package com.autoscientist.repository;

import com.autoscientist.entity.Dataset;
import com.autoscientist.entity.Finding;
import com.autoscientist.entity.ResearchQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FindingRepository extends JpaRepository<Finding, Long> {
    List<Finding> findByDatasetOrderByCreatedAtDesc(Dataset dataset);
    List<Finding> findByResearchQuestion(ResearchQuestion researchQuestion);
}
