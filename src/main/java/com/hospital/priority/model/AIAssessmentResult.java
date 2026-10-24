package com.hospital.priority.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * HEPS 2.0 MULTI-AGENT ASSESSMENT RESULT
 * ============================================================================
 * Aggregates insights from:
 * 1. Intake Agent (symptom extraction & clinical summary)
 * 2. Triage Agent (clinical urgency recommendation)
 * 3. Specialty Agent (specialist recommendation)
 * 4. Risk Agent (hidden red flags & high-risk warnings)
 * 5. Java Triage Rule Engine (deterministic safety check & disagreement detector)
 */
public class AIAssessmentResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private EmergencyLevel suggestedLevel;
    private Specialty suggestedSpecialty;
    private double confidenceScore;
    private List<String> riskFlags = new ArrayList<>();
    private String intakeSummary;
    private Map<String, String> agentOutputs = new HashMap<>();

    private EmergencyLevel ruleSuggestedLevel;
    private Specialty ruleSuggestedSpecialty;
    private boolean disagreementDetected;
    private String disagreementNotes;

    private boolean aiGenerated; // true if from LLM, false if from Fallback Java Rule Engine
    private long latencyMs;

    public AIAssessmentResult() {
        this.confidenceScore = 0.85;
        this.aiGenerated = true;
    }

    public EmergencyLevel getSuggestedLevel() {
        return suggestedLevel;
    }

    public void setSuggestedLevel(EmergencyLevel suggestedLevel) {
        this.suggestedLevel = suggestedLevel;
    }

    public Specialty getSuggestedSpecialty() {
        return suggestedSpecialty;
    }

    public void setSuggestedSpecialty(Specialty suggestedSpecialty) {
        this.suggestedSpecialty = suggestedSpecialty;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public List<String> getRiskFlags() {
        return riskFlags;
    }

    public void setRiskFlags(List<String> riskFlags) {
        this.riskFlags = riskFlags != null ? riskFlags : new ArrayList<>();
    }

    public String getIntakeSummary() {
        return intakeSummary;
    }

    public void setIntakeSummary(String intakeSummary) {
        this.intakeSummary = intakeSummary;
    }

    public Map<String, String> getAgentOutputs() {
        return agentOutputs;
    }

    public void setAgentOutputs(Map<String, String> agentOutputs) {
        this.agentOutputs = agentOutputs != null ? agentOutputs : new HashMap<>();
    }

    public EmergencyLevel getRuleSuggestedLevel() {
        return ruleSuggestedLevel;
    }

    public void setRuleSuggestedLevel(EmergencyLevel ruleSuggestedLevel) {
        this.ruleSuggestedLevel = ruleSuggestedLevel;
    }

    public Specialty getRuleSuggestedSpecialty() {
        return ruleSuggestedSpecialty;
    }

    public void setRuleSuggestedSpecialty(Specialty ruleSuggestedSpecialty) {
        this.ruleSuggestedSpecialty = ruleSuggestedSpecialty;
    }

    public boolean isDisagreementDetected() {
        return disagreementDetected;
    }

    public void setDisagreementDetected(boolean disagreementDetected) {
        this.disagreementDetected = disagreementDetected;
    }

    public String getDisagreementNotes() {
        return disagreementNotes;
    }

    public void setDisagreementNotes(String disagreementNotes) {
        this.disagreementNotes = disagreementNotes;
    }

    public boolean isAiGenerated() {
        return aiGenerated;
    }

    public void setAiGenerated(boolean aiGenerated) {
        this.aiGenerated = aiGenerated;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = latencyMs;
    }

    /**
     * Determines the final recommended priority taking deterministic safety into account.
     * If Java Rule Engine detected a critical vital sign, it takes absolute precedence over AI!
     */
    public EmergencyLevel getFinalRecommendedLevel() {
        if (ruleSuggestedLevel == EmergencyLevel.CRITICAL) {
            return EmergencyLevel.CRITICAL; // Safety first!
        }
        return suggestedLevel != null ? suggestedLevel : ruleSuggestedLevel;
    }
}
