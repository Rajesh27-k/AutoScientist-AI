package com.autoscientist.repository;

import com.autoscientist.entity.DataQualityIssue;
import com.autoscientist.entity.DataQualityReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataQualityIssueRepository extends JpaRepository<DataQualityIssue, Long> {
    List<DataQualityIssue> findByReportOrderBySeverityDesc(DataQualityReport report);
}
