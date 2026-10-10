package com.autoscientist.repository;

import com.autoscientist.entity.Experiment;
import com.autoscientist.entity.ExperimentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExperimentVersionRepository extends JpaRepository<ExperimentVersion, Long> {
    List<ExperimentVersion> findByExperimentOrderByVersionNumberDesc(Experiment experiment);
}
