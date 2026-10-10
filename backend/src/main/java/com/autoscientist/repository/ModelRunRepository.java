package com.autoscientist.repository;

import com.autoscientist.entity.ExperimentVersion;
import com.autoscientist.entity.ModelRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModelRunRepository extends JpaRepository<ModelRun, Long> {
    List<ModelRun> findByExperimentVersion(ExperimentVersion experimentVersion);
}
