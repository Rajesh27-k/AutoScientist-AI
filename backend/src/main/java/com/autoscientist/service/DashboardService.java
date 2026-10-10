package com.autoscientist.service;

import com.autoscientist.dto.dashboard.DashboardStatsDto;
import com.autoscientist.dto.dashboard.TimelineEventDto;
import com.autoscientist.entity.*;
import com.autoscientist.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DatasetRepository datasetRepository;
    private final DataQualityReportRepository dataQualityReportRepository;
    private final AssociationRepository associationRepository;
    private final ResearchQuestionRepository researchQuestionRepository;
    private final ExperimentRepository experimentRepository;
    private final FindingRepository findingRepository;
    private final ReportRepository reportRepository;

    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats(User user) {
        List<Dataset> datasets = datasetRepository.findByOwnerOrderByCreatedAtDesc(user);
        long totalDatasets = datasets.size();
        long totalObservations = datasets.stream().mapToLong(Dataset::getRowCount).sum();

        double totalQualityScore = 0;
        int qualityCount = 0;
        long totalAssoc = 0;
        long totalFindings = 0;

        List<TimelineEventDto> timeline = new ArrayList<>();

        for (Dataset d : datasets) {
            Optional<DataQualityReport> qr = dataQualityReportRepository.findByDataset(d);
            if (qr.isPresent()) {
                totalQualityScore += qr.get().getOverallScore();
                qualityCount++;
            }

            List<Association> assocs = associationRepository.findByDatasetOrderByRankScoreDesc(d);
            totalAssoc += assocs.size();

            List<Finding> findings = findingRepository.findByDatasetOrderByCreatedAtDesc(d);
            totalFindings += findings.size();

            timeline.add(TimelineEventDto.builder()
                    .eventType("DATASET_UPLOAD")
                    .title("Uploaded Dataset: " + d.getName())
                    .description(String.format("Profiled %d rows and %d columns (%s)", d.getRowCount(), d.getColumnCount(), d.getDatasetType()))
                    .datasetId(d.getId())
                    .datasetName(d.getName())
                    .timestamp(d.getCreatedAt())
                    .build());

            for (Finding f : findings.stream().limit(3).toList()) {
                timeline.add(TimelineEventDto.builder()
                        .eventType("FINDING_PRODUCED")
                        .title("Established Finding: " + f.getTitle())
                        .description(f.getSummary().length() > 100 ? f.getSummary().substring(0, 97) + "..." : f.getSummary())
                        .datasetId(d.getId())
                        .datasetName(d.getName())
                        .timestamp(f.getCreatedAt())
                        .build());
            }
        }

        List<ResearchQuestion> questions = researchQuestionRepository.findByUserOrderByCreatedAtDesc(user);
        for (ResearchQuestion q : questions.stream().limit(5).toList()) {
            timeline.add(TimelineEventDto.builder()
                    .eventType("QUESTION_ASKED")
                    .title("Research Question: " + (q.getQuestionText().length() > 60 ? q.getQuestionText().substring(0, 57) + "..." : q.getQuestionText()))
                    .description("Target: " + q.getTargetVariable() + " | Test: " + q.getSuggestedTest())
                    .datasetId(q.getDataset().getId())
                    .datasetName(q.getDataset().getName())
                    .timestamp(q.getCreatedAt())
                    .build());
        }

        List<Experiment> experiments = experimentRepository.findByUserOrderByCreatedAtDesc(user);
        long totalModelsTrained = 0;
        for (Experiment e : experiments) {
            for (ExperimentVersion v : e.getVersions()) {
                totalModelsTrained += 3; // Typically Ridge, Tree, Forest
            }
            timeline.add(TimelineEventDto.builder()
                    .eventType("MODEL_TRAINED")
                    .title("AutoML Experiment: " + e.getName())
                    .description("Target: " + e.getTargetColumn() + " (" + e.getProblemType() + ")")
                    .datasetId(e.getDataset().getId())
                    .datasetName(e.getDataset().getName())
                    .timestamp(e.getCreatedAt())
                    .build());
        }

        List<Report> reports = reportRepository.findByUserOrderByCreatedAtDesc(user);
        for (Report r : reports.stream().limit(5).toList()) {
            timeline.add(TimelineEventDto.builder()
                    .eventType("REPORT_EXPORTED")
                    .title("Generated Scientific Report: " + r.getTitle())
                    .description(r.getSummary())
                    .datasetId(r.getDataset().getId())
                    .datasetName(r.getDataset().getName())
                    .timestamp(r.getCreatedAt())
                    .build());
        }

        timeline.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));

        double avgQuality = qualityCount > 0 ? totalQualityScore / qualityCount : 0.0;
        avgQuality = Math.round(avgQuality * 10.0) / 10.0;

        return DashboardStatsDto.builder()
                .totalDatasets(totalDatasets)
                .totalObservations(totalObservations)
                .totalAssociations(totalAssoc)
                .totalResearchQuestions(questions.size())
                .totalExperiments(experiments.size())
                .totalModelsTrained(totalModelsTrained)
                .totalFindings(totalFindings)
                .totalReports(reports.size())
                .averageQualityScore(avgQuality)
                .recentActivity(timeline.stream().limit(12).collect(Collectors.toList()))
                .build();
    }
}
