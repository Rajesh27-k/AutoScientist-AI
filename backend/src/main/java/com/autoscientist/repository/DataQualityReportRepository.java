package com.autoscientist.repository;

import com.autoscientist.entity.DataQualityReport;
import com.autoscientist.entity.Dataset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DataQualityReportRepository extends JpaRepository<DataQualityReport, Long> {
    Optional<DataQualityReport> findByDataset(Dataset dataset);
}
