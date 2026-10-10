package com.autoscientist.service;

import com.autoscientist.dto.dataset.WorkflowStatusDto;
import com.autoscientist.dto.dataset.WorkflowStatusDto.StageStatusDto;
import com.autoscientist.entity.*;
import com.autoscientist.exception.ResourceNotFoundException;
import com.autoscientist.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final DatasetRepository datasetRepository;
    private final DataQualityReportRepository dataQualityReportRepository;
    private final AssociationRepository associationRepository;
    private final ResearchQuestionRepository researchQuestionRepository;
    private final StatisticalTestRepository statisticalTestRepository;
    private final ExperimentRepository experimentRepository;
    private final FindingRepository findingRepository;
    private final ReportRepository reportRepository;

    @Transactional(readOnly = true)
    public WorkflowStatusDto getWorkflowStatus(Long datasetId, User user) {
        Dataset dataset = datasetRepository.findByIdAndOwner(datasetId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Dataset not found with id: " + datasetId));

        Map<String, StageStatusDto> stages = new LinkedHashMap<>();

        // 1. Upload
        stages.put("upload", StageStatusDto.builder()
                .stageKey("upload")
                .label("Upload Dataset")
                .completed(true)
                .count(dataset.getRowCount() != null ? dataset.getRowCount() : 0)
                .summary(String.format("%d rows, %d columns ingested",
                        dataset.getRowCount() != null ? dataset.getRowCount() : 0,
                        dataset.getColumnCount() != null ? dataset.getColumnCount() : 0))
                .prerequisitesMet(true)
                .build());

        // 2. Dataset Overview
        boolean overviewDone = dataset.getRowCount() != null && dataset.getRowCount() > 0 &&
                dataset.getColumnCount() != null && dataset.getColumnCount() > 0;
        stages.put("overview", StageStatusDto.builder()
                .stageKey("overview")
                .label("Dataset Overview")
                .completed(overviewDone)
                .count(dataset.getColumnCount() != null ? dataset.getColumnCount() : 0)
                .summary(overviewDone ? dataset.getColumnCount() + " variables profiled" : "Pending profiling")
                .prerequisitesMet(true)
                .build());

        // 3. Data Quality
        Optional<DataQualityReport> qualityOpt = dataQualityReportRepository.findByDataset(dataset);
        boolean qualityDone = qualityOpt.isPresent() && qualityOpt.get().getOverallScore() != null;
        stages.put("quality", StageStatusDto.builder()
                .stageKey("quality")
                .label("Data Quality")
                .completed(qualityDone)
                .count(qualityOpt.map(q -> q.getIssues() != null ? q.getIssues().size() : 0).orElse(0))
                .summary(qualityDone ? String.format("Audit score: %.1f / 100", qualityOpt.get().getOverallScore()) : "Audit pending")
                .prerequisitesMet(true)
                .build());

        // 4. Automated EDA
        boolean edaDone = overviewDone;
        stages.put("eda", StageStatusDto.builder()
                .stageKey("eda")
                .label("Automated EDA")
                .completed(edaDone)
                .count(dataset.getColumnCount() != null ? dataset.getColumnCount() : 0)
                .summary(edaDone ? "Distributions & correlation matrix computed" : "EDA pending")
                .prerequisitesMet(true)
                .build());

        // 5. Associations
        List<Association> associations = associationRepository.findByDatasetOrderByRankScoreDesc(dataset);
        boolean associationsDone = !associations.isEmpty();
        stages.put("associations", StageStatusDto.builder()
                .stageKey("associations")
                .label("Associations")
                .completed(associationsDone)
                .count(associations.size())
                .summary(associationsDone ? associations.size() + " significant association(s) detected" : "No associations computed yet")
                .prerequisitesMet(true)
                .build());

        // 6. Research Questions
        List<ResearchQuestion> questions = researchQuestionRepository.findByDatasetOrderByCreatedAtDesc(dataset);
        boolean questionsDone = !questions.isEmpty();
        stages.put("research", StageStatusDto.builder()
                .stageKey("research")
                .label("Research Questions")
                .completed(questionsDone)
                .count(questions.size())
                .summary(questionsDone ? questions.size() + " question(s) formulated" : "No questions formulated yet")
                .prerequisitesMet(true)
                .build());

        // 7. Hypotheses
        int totalHypotheses = 0;
        for (ResearchQuestion q : questions) {
            if (q.getHypotheses() != null) {
                totalHypotheses += q.getHypotheses().size();
            }
        }
        boolean hypothesesDone = totalHypotheses > 0;
        stages.put("hypotheses", StageStatusDto.builder()
                .stageKey("hypotheses")
                .label("Hypotheses")
                .completed(hypothesesDone)
                .count(totalHypotheses)
                .summary(hypothesesDone ? totalHypotheses + " hypothesis pair(s) generated" : "No hypotheses formulated yet")
                .prerequisitesMet(questionsDone)
                .prerequisiteMessage("Hypotheses require at least one formulated research question.")
                .prerequisiteStage("research")
                .build());

        // 8. Statistical Testing
        List<StatisticalTest> tests = statisticalTestRepository.findByDatasetOrderByCreatedAtDesc(dataset);
        boolean testsDone = !tests.isEmpty();
        stages.put("statistics", StageStatusDto.builder()
                .stageKey("statistics")
                .label("Statistical Testing")
                .completed(testsDone)
                .count(tests.size())
                .summary(testsDone ? tests.size() + " statistical test(s) executed" : "No tests executed yet")
                .prerequisitesMet(hypothesesDone)
                .prerequisiteMessage("Statistical testing requires at least one formulated hypothesis.")
                .prerequisiteStage("hypotheses")
                .build());

        // 9. Machine Learning
        List<Experiment> experiments = experimentRepository.findByDatasetOrderByCreatedAtDesc(dataset);
        boolean mlDone = !experiments.isEmpty();
        stages.put("ml", StageStatusDto.builder()
                .stageKey("ml")
                .label("Machine Learning")
                .completed(mlDone)
                .count(experiments.size())
                .summary(mlDone ? experiments.size() + " AutoML benchmark(s) completed" : "No models trained yet")
                .prerequisitesMet(dataset.getColumnCount() != null && dataset.getColumnCount() >= 2)
                .prerequisiteMessage("Machine learning requires at least two variables in the dataset.")
                .prerequisiteStage("overview")
                .build());

        // 10. Visualizations
        boolean visualsDone = overviewDone;
        stages.put("visualizations", StageStatusDto.builder()
                .stageKey("visualizations")
                .label("Visualizations")
                .completed(visualsDone)
                .count(dataset.getColumnCount() != null ? dataset.getColumnCount() : 0)
                .summary("Interactive charts and statistical distributions ready")
                .prerequisitesMet(true)
                .build());

        // 11. Explainability/XAI
        boolean xaiDone = mlDone;
        stages.put("xai", StageStatusDto.builder()
                .stageKey("xai")
                .label("Explainability/XAI")
                .completed(xaiDone)
                .count(experiments.size())
                .summary(xaiDone ? "Permutation importance, fairness & what-if simulator ready" : "No model available to explain")
                .prerequisitesMet(mlDone)
                .prerequisiteMessage("Model explanation is unavailable because no model has been trained.")
                .prerequisiteStage("ml")
                .build());

        // 12. Findings
        List<Finding> findings = findingRepository.findByDatasetOrderByCreatedAtDesc(dataset);
        boolean findingsDone = !findings.isEmpty();
        stages.put("findings", StageStatusDto.builder()
                .stageKey("findings")
                .label("Findings")
                .completed(findingsDone)
                .count(findings.size())
                .summary(findingsDone ? findings.size() + " scientific finding(s) synthesized" : "No findings synthesized yet")
                .prerequisitesMet(testsDone || mlDone)
                .prerequisiteMessage("Scientific findings require statistical tests or predictive model evaluations.")
                .prerequisiteStage("statistics")
                .build());

        // 13. Report
        List<Report> reports = reportRepository.findByDatasetOrderByCreatedAtDesc(dataset);
        boolean reportDone = !reports.isEmpty();
        stages.put("report", StageStatusDto.builder()
                .stageKey("report")
                .label("Report")
                .completed(reportDone)
                .count(reports.size())
                .summary(reportDone ? reports.size() + " scientific report(s) generated" : "Report not yet generated")
                .prerequisitesMet(true)
                .build());

        // Determine recommended next stage
        String recommended = "report";
        String[] stageSequence = {"overview", "quality", "eda", "associations", "research", "hypotheses", "statistics", "ml", "visualizations", "xai", "findings", "report"};
        for (String sKey : stageSequence) {
            StageStatusDto st = stages.get(sKey);
            if (st != null && !st.isCompleted()) {
                recommended = sKey;
                break;
            }
        }

        return WorkflowStatusDto.builder()
                .datasetId(dataset.getId())
                .datasetName(dataset.getName())
                .rowCount(dataset.getRowCount() != null ? dataset.getRowCount() : 0)
                .columnCount(dataset.getColumnCount() != null ? dataset.getColumnCount() : 0)
                .stages(stages)
                .currentRecommendedStage(recommended)
                .build();
    }
}
