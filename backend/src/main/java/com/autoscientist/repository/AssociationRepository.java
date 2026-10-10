package com.autoscientist.repository;

import com.autoscientist.entity.Association;
import com.autoscientist.entity.Dataset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssociationRepository extends JpaRepository<Association, Long> {
    List<Association> findByDatasetOrderByRankScoreDesc(Dataset dataset);
    List<Association> findByDatasetAndIsSignificantTrueOrderByRankScoreDesc(Dataset dataset);
    void deleteByDataset(Dataset dataset);
}
