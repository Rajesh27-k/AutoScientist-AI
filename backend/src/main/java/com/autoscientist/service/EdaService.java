package com.autoscientist.service;

import com.autoscientist.analysis.eda.EdaEngine;
import com.autoscientist.dto.analysis.BoxPlotDto;
import com.autoscientist.dto.analysis.CorrelationMatrixDto;
import com.autoscientist.dto.analysis.GroupedSummaryDto;
import com.autoscientist.entity.Dataset;
import com.autoscientist.entity.User;
import com.autoscientist.util.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EdaService {

    private final DatasetService datasetService;
    private final FileStorageService fileStorageService;
    private final EdaEngine edaEngine;

    @Transactional(readOnly = true)
    public CorrelationMatrixDto getCorrelationMatrix(Long datasetId, User owner) {
        Dataset dataset = datasetService.getDatasetEntityWithOwnership(datasetId, owner);

        try (InputStream in = fileStorageService.getInputStream(dataset.getStoredFilename())) {
            EdaEngine.CorrelationMatrix matrix = edaEngine.computeCorrelationMatrix(in, dataset.getColumns());
            return CorrelationMatrixDto.builder()
                    .variables(matrix.getVariables())
                    .matrix(matrix.getMatrix())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute correlation matrix: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<BoxPlotDto> getBoxPlots(Long datasetId, User owner) {
        Dataset dataset = datasetService.getDatasetEntityWithOwnership(datasetId, owner);

        try (InputStream in = fileStorageService.getInputStream(dataset.getStoredFilename())) {
            List<EdaEngine.BoxPlotData> boxPlots = edaEngine.computeBoxPlots(in, dataset.getColumns());
            return boxPlots.stream()
                    .map(b -> BoxPlotDto.builder()
                            .variable(b.getVariable())
                            .min(b.getMin())
                            .q1(b.getQ1())
                            .median(b.getMedian())
                            .q3(b.getQ3())
                            .max(b.getMax())
                            .iqr(b.getIqr())
                            .outliers(b.getOutliers())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute box plots: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<GroupedSummaryDto> getGroupedSummary(Long datasetId, String numVar, String catVar, User owner) {
        Dataset dataset = datasetService.getDatasetEntityWithOwnership(datasetId, owner);

        try (InputStream in = fileStorageService.getInputStream(dataset.getStoredFilename())) {
            List<EdaEngine.GroupSummary> summaries = edaEngine.computeGroupedSummary(in, numVar, catVar);
            return summaries.stream()
                    .map(s -> GroupedSummaryDto.builder()
                            .groupName(s.getGroupName())
                            .count(s.count())
                            .mean(s.getMean())
                            .stdDev(s.getStdDev())
                            .median(s.getMedian())
                            .min(s.getMin())
                            .max(s.getMax())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute grouped summary: " + e.getMessage(), e);
        }
    }
}
