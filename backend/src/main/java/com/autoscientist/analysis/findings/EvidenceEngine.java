package com.autoscientist.analysis.findings;

import com.autoscientist.entity.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class EvidenceEngine {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Finding createFinding(Dataset dataset, ResearchQuestion researchQuestion,
                                 Hypothesis hypothesis, StatisticalTest test) {

        boolean isSig = test.getIsStatisticallySignificant();
        double p = test.getPValue();
        double effect = test.getEffectSize();
        int n = test.getSampleSize();

        String confidenceLevel;
        if (!isSig) {
            confidenceLevel = "INSUFFICIENT";
        } else if (p < 0.01 && effect >= 0.3 && n >= 25) {
            confidenceLevel = "HIGH";
        } else if (p < 0.05 && effect >= 0.15 && n >= 15) {
            confidenceLevel = "MODERATE";
        } else {
            confidenceLevel = "WEAK";
        }

        String title;
        String summary;

        if (isSig) {
            title = String.format("Statistically Significant Association: %s and %s", test.getVar1(), test.getVar2());
            summary = String.format("Empirical analysis rejected the null hypothesis (p=%.4f < 0.05) with %s effect size (%s=%.3f) across N=%d observations.",
                    p, confidenceLevel.toLowerCase(), test.getEffectSizeType(), effect, n);
        } else {
            title = String.format("Insufficient Evidence of Association: %s and %s", test.getVar1(), test.getVar2());
            summary = String.format("Failed to reject the null hypothesis (p=%.4f >= 0.05). There is insufficient empirical evidence to support an association between %s and %s in this cohort.",
                    p, test.getVar1(), test.getVar2());
        }

        StringBuilder evidenceText = new StringBuilder();
        evidenceText.append(String.format("1. Statistical Method: %s\n", test.getTestType()));
        evidenceText.append(String.format("2. Observed Test Statistic: %.4f\n", test.getStatistic()));
        if (test.getDegreesOfFreedom() != null) {
            evidenceText.append(String.format("3. Degrees of Freedom: %.1f\n", test.getDegreesOfFreedom()));
        }
        evidenceText.append(String.format("4. Calculated Two-Tailed p-value: %.4f (alpha threshold = %.2f)\n", p, test.getAlpha()));
        evidenceText.append(String.format("5. Effect Size (%s): %.4f\n", test.getEffectSizeType(), effect));
        evidenceText.append(String.format("6. Paired Complete-Case Sample Size: %d observations\n", n));
        evidenceText.append(String.format("7. Substantive Context: %s\n", test.getInterpretation()));

        Finding finding = Finding.builder()
                .dataset(dataset)
                .researchQuestion(researchQuestion)
                .hypothesis(hypothesis)
                .statisticalTest(test)
                .title(title)
                .summary(summary)
                .evidenceText(evidenceText.toString())
                .confidenceLevel(confidenceLevel)
                .causalityDisclaimer("Empirical findings reflect observed statistical associations and do not substantiate causal mechanisms without experimental intervention.")
                .evidenceNodes(new ArrayList<>())
                .build();

        // 1. Evidence Node: Dataset
        Map<String, Object> dsMeta = new HashMap<>();
        dsMeta.put("datasetId", dataset.getId());
        dsMeta.put("originalFilename", dataset.getOriginalFilename());
        dsMeta.put("rowCount", dataset.getRowCount());
        dsMeta.put("columnCount", dataset.getColumnCount());

        EvidenceNode dsNode = EvidenceNode.builder()
                .finding(finding)
                .nodeType("DATASET")
                .referenceId(dataset.getId())
                .description(String.format("Empirical dataset '%s' (N=%d rows, %d cols)", dataset.getName(), dataset.getRowCount(), dataset.getColumnCount()))
                .dataPayloadJson(safeJson(dsMeta))
                .build();
        finding.getEvidenceNodes().add(dsNode);

        // 2. Evidence Node: Hypothesis
        Map<String, Object> hypMeta = new HashMap<>();
        hypMeta.put("hypothesisId", hypothesis.getId());
        hypMeta.put("nullHypothesis", hypothesis.getNullHypothesis());
        hypMeta.put("altHypothesis", hypothesis.getAltHypothesis());

        EvidenceNode hypNode = EvidenceNode.builder()
                .finding(finding)
                .nodeType("HYPOTHESIS")
                .referenceId(hypothesis.getId())
                .description(String.format("Hypothesis #%d: %s", hypothesis.getRankOrder(), hypothesis.getAltHypothesis()))
                .dataPayloadJson(safeJson(hypMeta))
                .build();
        finding.getEvidenceNodes().add(hypNode);

        // 3. Evidence Node: Statistical Test
        Map<String, Object> testMeta = new HashMap<>();
        testMeta.put("testId", test.getId());
        testMeta.put("testType", test.getTestType());
        testMeta.put("statistic", test.getStatistic());
        testMeta.put("pValue", test.getPValue());
        testMeta.put("effectSize", test.getEffectSize());
        testMeta.put("sampleSize", test.getSampleSize());

        EvidenceNode testNode = EvidenceNode.builder()
                .finding(finding)
                .nodeType("STAT_TEST")
                .referenceId(test.getId())
                .description(String.format("%s test: stat=%.3f, p=%.4f, %s=%.3f (N=%d)",
                        test.getTestType(), test.getStatistic(), test.getPValue(), test.getEffectSizeType(), test.getEffectSize(), test.getSampleSize()))
                .dataPayloadJson(safeJson(testMeta))
                .build();
        finding.getEvidenceNodes().add(testNode);

        return finding;
    }

    private String safeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
