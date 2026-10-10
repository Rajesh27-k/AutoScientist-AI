package com.autoscientist.repository;

import com.autoscientist.entity.Dataset;
import com.autoscientist.entity.Experiment;
import com.autoscientist.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExperimentRepository extends JpaRepository<Experiment, Long> {
    List<Experiment> findByDatasetOrderByCreatedAtDesc(Dataset dataset);
    List<Experiment> findByUserOrderByCreatedAtDesc(User user);
    List<Experiment> findByDatasetAndUserOrderByCreatedAtDesc(Dataset dataset, User user);
}
