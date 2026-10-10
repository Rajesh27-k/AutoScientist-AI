package com.autoscientist.service;

import com.autoscientist.analysis.profiling.DatasetProfiler;
import com.autoscientist.analysis.quality.DataQualityEngine;
import com.autoscientist.dto.dataset.*;
import com.autoscientist.entity.DataQualityIssue;
import com.autoscientist.entity.DataQualityReport;
import com.autoscientist.entity.Dataset;
import com.autoscientist.entity.DatasetColumn;
import com.autoscientist.entity.User;
import com.autoscientist.exception.BadRequestException;
import com.autoscientist.exception.ResourceNotFoundException;
import com.autoscientist.exception.UnauthorizedException;
import com.autoscientist.repository.DataQualityReportRepository;
import com.autoscientist.repository.DatasetColumnRepository;
import com.autoscientist.repository.DatasetRepository;
import com.autoscientist.util.CsvValidator;
import com.autoscientist.util.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DatasetService {

    private final DatasetRepository datasetRepository;
    private final DatasetColumnRepository datasetColumnRepository;
    private final DataQualityReportRepository dataQualityReportRepository;
    private final FileStorageService fileStorageService;
    private final DatasetProfiler datasetProfiler;
    private final DataQualityEngine dataQualityEngine;

    @Transactional
    public DatasetDetailDto uploadDataset(MultipartFile file, String name, String description, User owner) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file cannot be empty.");
        }

        // Validate CSV structural integrity
        try (InputStream testIn = file.getInputStream()) {
            CsvValidator.validateCsv(testIn);
        } catch (Exception e) {
            throw new BadRequestException("CSV validation failed: " + e.getMessage());
        }

        // Store file safely
        String storedFilename = fileStorageService.storeFile(file);
        String datasetName = (name != null && !name.trim().isEmpty()) ? name.trim() : file.getOriginalFilename();

        Dataset dataset = Dataset.builder()
                .owner(owner)
                .name(datasetName)
                .originalFilename(file.getOriginalFilename())
                .storedFilename(storedFilename)
                .fileSize(file.getSize())
                .description(description)
                .build();

        dataset = datasetRepository.save(dataset);

        // Run Profiler
        try (InputStream in = fileStorageService.getInputStream(storedFilename)) {
            DatasetProfiler.ProfilingResult profResult = datasetProfiler.profile(in, dataset);
            dataset.setRowCount(profResult.getRowCount());
            dataset.setColumnCount(profResult.getColumnCount());
            dataset.setDatasetType(profResult.getDatasetType());
            dataset.setColumns(profResult.getColumns());
        } catch (Exception e) {
            fileStorageService.deleteFile(storedFilename);
            throw new RuntimeException("Profiling failed: " + e.getMessage(), e);
        }

        // Run Quality Engine
        DataQualityReport qualityReport;
        try (InputStream in = fileStorageService.getInputStream(storedFilename)) {
            qualityReport = dataQualityEngine.evaluate(in, dataset, dataset.getColumns());
            dataset.setQualityReport(qualityReport);
        } catch (Exception e) {
            fileStorageService.deleteFile(storedFilename);
            throw new RuntimeException("Data quality audit failed: " + e.getMessage(), e);
        }

        dataset = datasetRepository.save(dataset);
        return mapToDetailDto(dataset);
    }

    @Transactional(readOnly = true)
    public List<DatasetDto> listUserDatasets(User owner) {
        return datasetRepository.findByOwnerOrderByCreatedAtDesc(owner)
                .stream()
                .map(this::mapToSummaryDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DatasetDetailDto getDatasetDetail(Long id, User owner) {
        Dataset dataset = getDatasetEntityWithOwnership(id, owner);
        return mapToDetailDto(dataset);
    }

    @Transactional(readOnly = true)
    public Dataset getDatasetEntityWithOwnership(Long id, User owner) {
        Dataset dataset = datasetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dataset not found with id: " + id));

        if (!dataset.getOwner().getId().equals(owner.getId())) {
            throw new UnauthorizedException("You do not have permission to access dataset id: " + id);
        }

        return dataset;
    }

    @Transactional
    public void deleteDataset(Long id, User owner) {
        Dataset dataset = getDatasetEntityWithOwnership(id, owner);
        String storedFilename = dataset.getStoredFilename();
        datasetRepository.delete(dataset);
        fileStorageService.deleteFile(storedFilename);
    }

    public DatasetDto mapToSummaryDto(Dataset dataset) {
        Double score = null;
        if (dataset.getQualityReport() != null) {
            score = dataset.getQualityReport().getOverallScore();
        }

        return DatasetDto.builder()
                .id(dataset.getId())
                .name(dataset.getName())
                .originalFilename(dataset.getOriginalFilename())
                .fileSize(dataset.getFileSize())
                .rowCount(dataset.getRowCount())
                .columnCount(dataset.getColumnCount())
                .datasetType(dataset.getDatasetType())
                .description(dataset.getDescription())
                .qualityScore(score)
                .createdAt(dataset.getCreatedAt())
                .updatedAt(dataset.getUpdatedAt())
                .build();
    }

    public DatasetDetailDto mapToDetailDto(Dataset dataset) {
        List<DatasetColumnDto> columnDtos = dataset.getColumns().stream()
                .map(col -> DatasetColumnDto.builder()
                        .id(col.getId())
                        .columnIndex(col.getColumnIndex())
                        .columnName(col.getColumnName())
                        .dataType(col.getDataType())
                        .typeConfidence(col.getTypeConfidence())
                        .missingCount(col.getMissingCount())
                        .missingPct(col.getMissingPct())
                        .uniqueCount(col.getUniqueCount())
                        .minVal(col.getMinVal())
                        .maxVal(col.getMaxVal())
                        .meanVal(col.getMeanVal())
                        .medianVal(col.getMedianVal())
                        .stdDev(col.getStdDev())
                        .variance(col.getVariance())
                        .q1(col.getQ1())
                        .q3(col.getQ3())
                        .skewness(col.getSkewness())
                        .categoriesJson(col.getCategoriesJson())
                        .distributionJson(col.getDistributionJson())
                        .build())
                .collect(Collectors.toList());

        DataQualityReportDto qualityReportDto = null;
        if (dataset.getQualityReport() != null) {
            DataQualityReport rep = dataset.getQualityReport();
            List<DataQualityIssueDto> issueDtos = rep.getIssues().stream()
                    .map(issue -> DataQualityIssueDto.builder()
                            .id(issue.getId())
                            .columnName(issue.getColumnName())
                            .issueType(issue.getIssueType())
                            .severity(issue.getSeverity())
                            .description(issue.getDescription())
                            .recommendation(issue.getRecommendation())
                            .affectedRows(issue.getAffectedRows())
                            .build())
                    .collect(Collectors.toList());

            qualityReportDto = DataQualityReportDto.builder()
                    .id(rep.getId())
                    .overallScore(rep.getOverallScore())
                    .missingValueScore(rep.getMissingValueScore())
                    .duplicateScore(rep.getDuplicateScore())
                    .outlierScore(rep.getOutlierScore())
                    .consistencyScore(rep.getConsistencyScore())
                    .totalIssues(rep.getTotalIssues())
                    .issues(issueDtos)
                    .build();
        }

        return DatasetDetailDto.builder()
                .id(dataset.getId())
                .name(dataset.getName())
                .originalFilename(dataset.getOriginalFilename())
                .fileSize(dataset.getFileSize())
                .rowCount(dataset.getRowCount())
                .columnCount(dataset.getColumnCount())
                .datasetType(dataset.getDatasetType())
                .description(dataset.getDescription())
                .createdAt(dataset.getCreatedAt())
                .updatedAt(dataset.getUpdatedAt())
                .columns(columnDtos)
                .qualityReport(qualityReportDto)
                .build();
    }
}
