package com.autoscientist.repository;

import com.autoscientist.entity.Dataset;
import com.autoscientist.entity.Report;
import com.autoscientist.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByUserOrderByCreatedAtDesc(User user);
    List<Report> findByDatasetOrderByCreatedAtDesc(Dataset dataset);
}
