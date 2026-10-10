package com.autoscientist.repository;

import com.autoscientist.entity.Dataset;
import com.autoscientist.entity.DatasetColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DatasetColumnRepository extends JpaRepository<DatasetColumn, Long> {
    List<DatasetColumn> findByDatasetOrderByColumnIndexAsc(Dataset dataset);
    Optional<DatasetColumn> findByDatasetAndColumnName(Dataset dataset, String columnName);
}
