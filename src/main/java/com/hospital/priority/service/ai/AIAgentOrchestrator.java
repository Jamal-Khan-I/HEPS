package com.hospital.priority.service.ai;

import com.hospital.priority.model.AIAssessmentResult;
import com.hospital.priority.model.EmergencyLevel;
import com.hospital.priority.model.Specialty;
import com.hospital.priority.model.Vitals;
import com.hospital.priority.service.JavaTriageRuleEngine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * HEPS 2.0 AI AGENT ORCHESTRATOR
 * ============================================================================
 * Coordinates the 4 Specialized Clinical AI Agents:
 * 1. Intake Agent (Extracts symptoms and clinical presentation)
 * 2. Triage Agent (Evaluates clinical urgency)
 * 3. Specialty Agent (Predicts required medical department)
 * 4. Risk Agent (Scans for occult red flags and high-risk alerts)
 *
 * KEY FEATURES:
 * - Deterministic Safety Integration: Cross-validates AI outputs against JavaTriageRuleEngine.
 * - Disagreement Detection: Identifies conflicts between AI and deterministic rules.
 * - Zero-Downtime Fallback: Works 100% locally with intelligent reasoning logic.
 */
public class AIAgentOrchestrator {

    private static AIAgentOrchestrator instance;

    private final IntakeAgent intakeAgent;
    private final TriageAgent triageAgent;
    private final SpecialtyAgent specialtyAgent;
    private final RiskAgent riskAgent;
    private final JavaTriageRuleEngine ruleEngine;

    private AIAgentOrchestrator() {
        this.intakeAgent = new IntakeAgent();
        this.triageAgent = new TriageAgent();
        this.specialtyAgent = new SpecialtyAgent();
        this.riskAgent = new RiskAgent();
        this.ruleEngine = JavaTriageRuleEngine.getInstance();
    }

    public static synchronized AIAgentOrchestrator getInstance() {
        if (instance == null) {
            instance = new AIAgentOrchestrator();
        }
        return instance;
    }

    /**
     * Conducts a comprehensive multi-agent triage assessment.
     */
    public AIAssessmentResult assessPatient(String fullName, int age, String gender,
                                            String symptoms, Vitals vitals) {
        long startTime = System.currentTimeMillis();

        AIAssessmentResult result = new AIAssessmentResult();

        // 1. INTAKE AGENT EXECUTION
        String intakeSummary = intakeAgent.analyze(fullName, age, gender, symptoms, vitals);
        result.setIntakeSummary(intakeSummary);

        // 2. TRIAGE AGENT EXECUTION
        TriageAgent.TriageRecommendation triageRec = triageAgent.assess(age, symptoms, vitals);
        result.setSuggestedLevel(triageRec.getLevel());
        result.setConfidenceScore(triageRec.getConfidence());

        // 3. SPECIALTY AGENT EXECUTION
        SpecialtyAgent.SpecialtyRecommendation specRec = specialtyAgent.recommend(age, symptoms, vitals);
        result.setSuggestedSpecialty(specRec.getSpecialty());

        // 4. RISK AGENT EXECUTION
        List<String> redFlags = riskAgent.detectRedFlags(age, gender, symptoms, vitals);
        result.setRiskFlags(redFlags);

        // Aggregate Agent Outputs for Audit Log
        Map<String, String> outputs = new HashMap<>();
        outputs.put("IntakeAgent", intakeSummary);
        outputs.put("TriageAgent", triageRec.toString());
        outputs.put("SpecialtyAgent", specRec.toString());
        outputs.put("RiskAgent", String.join("; ", redFlags));
        result.setAgentOutputs(outputs);

        // 5. DETERMINISTIC JAVA RULE ENGINE SAFETY CHECK
        JavaTriageRuleEngine.RuleEvaluation ruleEval = ruleEngine.evaluate(age, symptoms, vitals);
        result.setRuleSuggestedLevel(ruleEval.getRecommendedLevel());
        result.setRuleSuggestedSpecialty(ruleEval.getRecommendedSpecialty());

        // 6. DISAGREEMENT DETECTION
        detectDisagreements(result, triageRec, ruleEval, redFlags);

        result.setLatencyMs(System.currentTimeMillis() - startTime);
        return result;
    }

    private void detectDisagreements(AIAssessmentResult result,
                                     TriageAgent.TriageRecommendation aiTriage,
                                     JavaTriageRuleEngine.RuleEvaluation ruleEval,
                                     List<String> redFlags) {

        boolean disagreement = false;
        StringBuilder notes = new StringBuilder();

        // Check Priority Conflict: AI recommended lower urgency than Java safety rules
        if (ruleEval.getRecommendedLevel().getRank() < aiTriage.getLevel().getRank()) {
            disagreement = true;
            notes.append("Safety Guardrail Triggered: Java Rule Engine detected vital indicators [")
                    .append(String.join(", ", ruleEval.getTriggeredRules()))
                    .append("] requiring ").append(ruleEval.getRecommendedLevel())
                    .append(" priority, higher than AI's ").append(aiTriage.getLevel())
                    .append(" suggestion. ");
        }

        // Check Risk Conflict: AI recommended LOW, but Risk Agent flagged cardiac or pre-shock signs
        if (aiTriage.getLevel() == EmergencyLevel.LOW) {
            for (String flag : redFlags) {
                if (flag.toLowerCase().contains("cardiac") || flag.toLowerCase().contains("pre-shock")
                        || flag.toLowerCase().contains("anaphylaxis") || flag.toLowerCase().contains("vulnerability")) {
                    disagreement = true;
                    notes.append("Risk Conflict: AI Triage suggested LOW, but Risk Agent flagged: \"")
                            .append(flag).append("\". Elevation advised. ");
                    break;
                }
            }
        }

        // Check Specialty Conflict
        if (result.getSuggestedSpecialty() != ruleEval.getRecommendedSpecialty()) {
            notes.append("Department Note: AI suggested ")
                    .append(result.getSuggestedSpecialty().getDisplayName())
                    .append(", while Rule Engine mapped to ")
                    .append(ruleEval.getRecommendedSpecialty().getDisplayName())
                    .append(". ");
        }

        result.setDisagreementDetected(disagreement);
        result.setDisagreementNotes(notes.length() > 0 ? notes.toString().trim() : "Consensus reached across all clinical agents.");
    }
}
