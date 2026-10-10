package com.autoscientist.service;

import com.autoscientist.analysis.drift.DataDriftEngine;
import com.autoscientist.dto.drift.DatasetDriftReportDto;
import com.autoscientist.entity.Dataset;
import com.autoscientist.entity.User;
import com.autoscientist.exception.BadRequestException;
import com.autoscientist.util.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataDriftService {

    private final DatasetService datasetService;
    private final FileStorageService fileStorageService;
    private final DataDriftEngine dataDriftEngine;

    public DatasetDriftReportDto compareDatasets(Long referenceDatasetId, Long targetDatasetId, User user) {
        Dataset refDataset = datasetService.getDatasetEntityWithOwnership(referenceDatasetId, user);
        Dataset targetDataset = datasetService.getDatasetEntityWithOwnership(targetDatasetId, user);

        try (InputStream refStream = fileStorageService.getInputStream(refDataset.getStoredFilename());
             InputStream targetStream = fileStorageService.getInputStream(targetDataset.getStoredFilename())) {

            return dataDriftEngine.analyzeDrift(refDataset, refStream, targetDataset, targetStream);
        } catch (Exception e) {
            log.error("Drift comparison failed between {} and {}", referenceDatasetId, targetDatasetId, e);
            throw new BadRequestException("Data drift analysis failed: " + e.getMessage());
        }
    }
}
