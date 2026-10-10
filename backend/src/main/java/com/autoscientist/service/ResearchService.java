package com.autoscientist.service;

import com.autoscientist.analysis.findings.EvidenceEngine;
import com.autoscientist.analysis.findings.QuestionAnalysisEngine;
import com.autoscientist.analysis.statistics.StatisticalTestEngine;
import com.autoscientist.dto.research.*;
import com.autoscientist.entity.*;
import com.autoscientist.exception.BadRequestException;
import com.autoscientist.exception.ResourceNotFoundException;
import com.autoscientist.exception.UnauthorizedException;
import com.autoscientist.repository.*;
import com.autoscientist.util.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResearchService {

    private final ResearchQuestionRepository researchQuestionRepository;
    private final HypothesisRepository hypothesisRepository;
    private final StatisticalTestRepository statisticalTestRepository;
    private final FindingRepository findingRepository;
    private final DatasetService datasetService;
    private final FileStorageService fileStorageService;
    private final QuestionAnalysisEngine questionAnalysisEngine;
    private final StatisticalTestEngine statisticalTestEngine;
    private final EvidenceEngine evidenceEngine;

    @Transactional
    public ResearchQuestionDto createResearchQuestion(Long datasetId, ResearchQuestionRequest request, User user) {
        Dataset dataset = datasetService.getDatasetEntityWithOwnership(datasetId, user);

        ResearchQuestion rq = ResearchQuestion.builder()
                .dataset(dataset)
                .user(user)
                .questionText(request.getQuestionText().trim())
                .status("ACTIVE")
                .build();

        rq = researchQuestionRepository.save(rq);

        QuestionAnalysisEngine.QuestionAnalysisResult analysis =
                questionAnalysisEngine.analyzeQuestion(
                        request.getQuestionText(),
                        request.getPredictorVariable(),
                        request.getTargetVariable(),
                        dataset.getColumns(),
                        rq
                );

        rq.setPredictorVariable(analysis.getPredictorVariable());
        rq.setTargetVariable(analysis.getTargetVariable());
        rq.setQuestionType(analysis.getQuestionType());
        rq.setSuggestedTest(analysis.getSuggestedTest());

        List<Hypothesis> savedHypotheses = hypothesisRepository.saveAll(analysis.getHypotheses());
        rq.setHypotheses(savedHypotheses);

        rq = researchQuestionRepository.save(rq);
        return mapToRqDto(rq);
    }

    @Transactional
    public FindingDto executeHypothesisTest(Long rqId, Long hypothesisId, User user) {
        ResearchQuestion rq = researchQuestionRepository.findById(rqId)
                .orElseThrow(() -> new ResourceNotFoundException("Research question not found with id: " + rqId));

        if (!rq.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Unauthorized access to research question.");
        }

        Hypothesis hypothesis = hypothesisRepository.findById(hypothesisId)
                .orElseThrow(() -> new ResourceNotFoundException("Hypothesis not found with id: " + hypothesisId));

        Dataset dataset = rq.getDataset();

        // Read columns data from dataset CSV
        Map<String, List<String>> colData = parseColumns(dataset.getStoredFilename(), Arrays.asList(hypothesis.getVar1(), hypothesis.getVar2()));
        List<String> raw1 = colData.get(hypothesis.getVar1());
        List<String> raw2 = colData.get(hypothesis.getVar2());

        StatisticalTestEngine.TestResult result;
        String testType = hypothesis.getSuggestedTest();

        if (testType.equals("PEARSON")) {
            result = statisticalTestEngine.computePearsonCorrelation(hypothesis.getVar1(), hypothesis.getVar2(), raw1, raw2);
        } else if (testType.equals("WELCH_T_TEST")) {
            result = statisticalTestEngine.computeWelchTTest(hypothesis.getVar1(), hypothesis.getVar2(), raw1, raw2);
        } else if (testType.equals("ONE_WAY_ANOVA")) {
            result = statisticalTestEngine.computeOneWayAnova(hypothesis.getVar1(), hypothesis.getVar2(), raw1, raw2);
        } else {
            result = statisticalTestEngine.computeChiSquare(hypothesis.getVar1(), hypothesis.getVar2(), raw1, raw2);
        }

        StatisticalTest test = StatisticalTest.builder()
                .hypothesis(hypothesis)
                .dataset(dataset)
                .testType(result.getTestType())
                .var1(hypothesis.getVar1())
                .var2(hypothesis.getVar2())
                .statistic(result.getStatistic())
                .pValue(result.getPValue())
                .alpha(0.05)
                .degreesOfFreedom(result.getDegreesOfFreedom())
                .effectSize(result.getEffectSize())
                .effectSizeType(result.getEffectSizeType())
                .sampleSize(result.getSampleSize())
                .isStatisticallySignificant(result.isSignificant())
                .status("COMPLETED")
                .interpretation(result.getInterpretation())
                .build();

        test = statisticalTestRepository.save(test);

        // Generate Finding and Provenance Graph Nodes
        Finding finding = evidenceEngine.createFinding(dataset, rq, hypothesis, test);
        finding = findingRepository.save(finding);

        return mapToFindingDto(finding);
    }

    @Transactional(readOnly = true)
    public List<ResearchQuestionDto> getDatasetResearchQuestions(Long datasetId, User user) {
        Dataset dataset = datasetService.getDatasetEntityWithOwnership(datasetId, user);
        return researchQuestionRepository.findByDatasetOrderByCreatedAtDesc(dataset)
                .stream()
                .map(this::mapToRqDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ResearchQuestionDto getResearchQuestion(Long id, User user) {
        ResearchQuestion rq = researchQuestionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Research question not found with id: " + id));

        if (!rq.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Unauthorized access to research question.");
        }

        return mapToRqDto(rq);
    }

    private Map<String, List<String>> parseColumns(String storedFilename, List<String> targets) {
        Map<String, List<String>> map = new HashMap<>();
        targets.forEach(t -> map.put(t, new ArrayList<>()));

        try (InputStream in = fileStorageService.getInputStream(storedFilename);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            for (CSVRecord record : parser) {
                for (String t : targets) {
                    if (record.isMapped(t)) {
                        map.get(t).add(record.get(t));
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed reading columns from dataset: " + e.getMessage(), e);
        }

        return map;
    }

    public ResearchQuestionDto mapToRqDto(ResearchQuestion rq) {
        List<HypothesisDto> hypDtos = rq.getHypotheses().stream()
                .map(this::mapToHypDto)
                .collect(Collectors.toList());

        return ResearchQuestionDto.builder()
                .id(rq.getId())
                .datasetId(rq.getDataset().getId())
                .questionText(rq.getQuestionText())
                .predictorVariable(rq.getPredictorVariable())
                .targetVariable(rq.getTargetVariable())
                .questionType(rq.getQuestionType())
                .suggestedTest(rq.getSuggestedTest())
                .status(rq.getStatus())
                .hypotheses(hypDtos)
                .createdAt(rq.getCreatedAt())
                .build();
    }

    public HypothesisDto mapToHypDto(Hypothesis h) {
        List<StatisticalTestDto> testDtos = h.getStatisticalTests().stream()
                .map(this::mapToTestDto)
                .collect(Collectors.toList());

        return HypothesisDto.builder()
                .id(h.getId())
                .researchQuestionId(h.getResearchQuestion().getId())
                .nullHypothesis(h.getNullHypothesis())
                .altHypothesis(h.getAltHypothesis())
                .rationale(h.getRationale())
                .suggestedTest(h.getSuggestedTest())
                .var1(h.getVar1())
                .var2(h.getVar2())
                .confidence(h.getConfidence())
                .rankOrder(h.getRankOrder())
                .statisticalTests(testDtos)
                .createdAt(h.getCreatedAt())
                .build();
    }

    public StatisticalTestDto mapToTestDto(StatisticalTest t) {
        return StatisticalTestDto.builder()
                .id(t.getId())
                .hypothesisId(t.getHypothesis().getId())
                .datasetId(t.getDataset().getId())
                .testType(t.getTestType())
                .var1(t.getVar1())
                .var2(t.getVar2())
                .statistic(t.getStatistic())
                .pValue(t.getPValue())
                .alpha(t.getAlpha())
                .degreesOfFreedom(t.getDegreesOfFreedom())
                .effectSize(t.getEffectSize())
                .effectSizeType(t.getEffectSizeType())
                .sampleSize(t.getSampleSize())
                .isStatisticallySignificant(t.getIsStatisticallySignificant())
                .status(t.getStatus())
                .interpretation(t.getInterpretation())
                .createdAt(t.getCreatedAt())
                .build();
    }

    public FindingDto mapToFindingDto(Finding f) {
        List<EvidenceNodeDto> nodeDtos = f.getEvidenceNodes().stream()
                .map(n -> EvidenceNodeDto.builder()
                        .id(n.getId())
                        .nodeType(n.getNodeType())
                        .referenceId(n.getReferenceId())
                        .description(n.getDescription())
                        .dataPayloadJson(n.getDataPayloadJson())
                        .createdAt(n.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return FindingDto.builder()
                .id(f.getId())
                .datasetId(f.getDataset().getId())
                .researchQuestionId(f.getResearchQuestion() != null ? f.getResearchQuestion().getId() : null)
                .hypothesisId(f.getHypothesis() != null ? f.getHypothesis().getId() : null)
                .statisticalTestId(f.getStatisticalTest() != null ? f.getStatisticalTest().getId() : null)
                .title(f.getTitle())
                .summary(f.getSummary())
                .evidenceText(f.getEvidenceText())
                .confidenceLevel(f.getConfidenceLevel())
                .causalityDisclaimer(f.getCausalityDisclaimer())
                .evidenceNodes(nodeDtos)
                .createdAt(f.getCreatedAt())
                .build();
    }
}
