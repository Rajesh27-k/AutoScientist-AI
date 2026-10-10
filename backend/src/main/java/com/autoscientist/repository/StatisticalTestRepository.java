package com.autoscientist.repository;

import com.autoscientist.entity.Dataset;
import com.autoscientist.entity.Hypothesis;
import com.autoscientist.entity.StatisticalTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatisticalTestRepository extends JpaRepository<StatisticalTest, Long> {
    List<StatisticalTest> findByHypothesis(Hypothesis hypothesis);
    List<StatisticalTest> findByDatasetOrderByCreatedAtDesc(Dataset dataset);
}
