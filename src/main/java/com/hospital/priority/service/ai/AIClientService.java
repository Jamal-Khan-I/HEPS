package com.hospital.priority.service.ai;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hospital.priority.model.AIAssessmentResult;
import com.hospital.priority.model.EmergencyLevel;
import com.hospital.priority.model.Specialty;
import com.hospital.priority.model.Vitals;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * ============================================================================
 * HEPS 2.0 AI CLINICAL CLIENT SERVICE
 * ============================================================================
 * Connects to advanced LLM inference endpoints for zero-shot clinical narrative
 * extraction, triage assessment, and dynamic department scheduling.
 *
 * NOTE: UI and user logs refer strictly to "AI" to maintain hospital product branding.
 */
public class AIClientService {

    private static AIClientService instance;

    private String apiKey;
    private String modelName;
    private String endpointUrl;
    private final HttpClient httpClient;
    private final Gson gson;

    private AIClientService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.gson = new Gson();
        loadConfiguration();
    }

    public static synchronized AIClientService getInstance() {
        if (instance == null) {
            instance = new AIClientService();
        }
        return instance;
    }

    private void loadConfiguration() {
        Properties props = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/config.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception e) {
            System.err.println("Notice: Could not load AI configuration: " + e.getMessage());
        }

        this.apiKey = props.getProperty("ai.api.key", "").trim();
        this.modelName = props.getProperty("ai.model", "openai/gpt-oss-120b").trim();
        this.endpointUrl = props.getProperty("ai.endpoint", "https://api.groq.com/openai/v1/chat/completions").trim();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty() && !apiKey.startsWith("your_");
    }

    /**
     * Extracts structured clinical entities from an unstructured free-text narrative.
     *
     * @param rawNarrative Unstructured patient history or clinical intake text.
     * @return AIClinicalExtraction populated with clinical parameters.
     */
    public CompletableFuture<AIClinicalExtraction> extractClinicalNarrativeAsync(String rawNarrative) {
        return CompletableFuture.supplyAsync(() -> extractClinicalNarrative(rawNarrative));
    }

    public AIClinicalExtraction extractClinicalNarrative(String rawNarrative) {
        if (rawNarrative == null || rawNarrative.trim().isEmpty()) {
            return new AIClinicalExtraction();
        }

        if (!isConfigured()) {
            return fallbackExtraction(rawNarrative);
        }

        try {
            String systemPrompt = "You are an expert Hospital Emergency Triage AI. "
                    + "Analyze the unstructured clinical narrative and extract all patient details and vital signs. "
                    + "Respond ONLY with a valid JSON object matching this schema: {"
                    + "\"fullName\": string or null,"
                    + "\"age\": integer or null,"
                    + "\"gender\": \"Male\"|\"Female\"|\"Other\" or null,"
                    + "\"phone\": string or null,"
                    + "\"arrivalMode\": \"Ambulance\"|\"Walk-in\"|\"Referred\"|\"Police\"|\"Brought by relatives\" or null,"
                    + "\"chiefComplaint\": string,"
                    + "\"spo2\": integer or null,"
                    + "\"heartRate\": integer or null,"
                    + "\"temperature\": float or null,"
                    + "\"systolicBp\": integer or null,"
                    + "\"diastolicBp\": integer or null,"
                    + "\"respiratoryRate\": integer or null,"
                    + "\"gcs\": integer (3-15, Glasgow Coma Scale, default 15 if alert or not specified),"
                    + "\"painScore\": integer (0-10, MUST infer clinical pain score from severity, e.g. severe/radiating chest pain=8, fracture=7-9, mild=1-3, none=0),"
                    + "\"allergies\": string or \"None\","
                    + "\"knownConditions\": string or \"None\","
                    + "\"urgency\": \"CRITICAL\"|\"HIGH\"|\"MEDIUM\"|\"LOW\","
                    + "\"specialty\": \"CARDIOLOGY\"|\"PULMONOLOGY\"|\"NEUROLOGY\"|\"TRAUMA_SURGERY\"|\"ORTHOPEDICS\"|\"PEDIATRICS\"|\"GENERAL_EMERGENCY\","
                    + "\"reasoning\": string"
                    + "}";

            JsonObject payload = new JsonObject();
            payload.addProperty("model", modelName);

            com.google.gson.JsonArray messages = new com.google.gson.JsonArray();

            JsonObject sysMsg = new JsonObject();
            sysMsg.addProperty("role", "system");
            sysMsg.addProperty("content", systemPrompt);
            messages.add(sysMsg);

            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", "user");
            userMsg.addProperty("content", "Extract clinical entities from this narrative:\n" + rawNarrative);
            messages.add(userMsg);

            payload.add("messages", messages);

            JsonObject respFormat = new JsonObject();
            respFormat.addProperty("type", "json_object");
            payload.add("response_format", respFormat);
            payload.addProperty("temperature", 0.1);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpointUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "HEPS-AI/1.0")
                    .timeout(Duration.ofSeconds(12))
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                String content = jsonResponse.getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString();

                return parseExtractionJson(content, rawNarrative);
            } else {
                System.err.println("Notice: AI API returned HTTP " + response.statusCode() + ": " + response.body());
                return fallbackExtraction(rawNarrative);
            }

        } catch (Exception e) {
            System.err.println("Notice: AI extraction failed, falling back to local analysis: " + e.getMessage());
            return fallbackExtraction(rawNarrative);
        }
    }

    private AIClinicalExtraction parseExtractionJson(String json, String rawFallback) {
        AIClinicalExtraction ext = new AIClinicalExtraction();
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

            if (obj.has("fullName") && !obj.get("fullName").isJsonNull()) ext.setFullName(obj.get("fullName").getAsString());
            if (obj.has("age") && !obj.get("age").isJsonNull()) ext.setAge(obj.get("age").getAsInt());
            if (obj.has("gender") && !obj.get("gender").isJsonNull()) ext.setGender(obj.get("gender").getAsString());
            if (obj.has("phone") && !obj.get("phone").isJsonNull()) ext.setPhone(obj.get("phone").getAsString());
            if (obj.has("arrivalMode") && !obj.get("arrivalMode").isJsonNull()) ext.setArrivalMode(obj.get("arrivalMode").getAsString());
            if (obj.has("chiefComplaint") && !obj.get("chiefComplaint").isJsonNull()) ext.setChiefComplaint(obj.get("chiefComplaint").getAsString());
            if (obj.has("spo2") && !obj.get("spo2").isJsonNull()) ext.setSpo2(obj.get("spo2").getAsInt());
            if (obj.has("heartRate") && !obj.get("heartRate").isJsonNull()) ext.setHeartRate(obj.get("heartRate").getAsInt());
            if (obj.has("temperature") && !obj.get("temperature").isJsonNull()) ext.setTemperature(obj.get("temperature").getAsDouble());
            if (obj.has("systolicBp") && !obj.get("systolicBp").isJsonNull()) ext.setSystolicBp(obj.get("systolicBp").getAsInt());
            if (obj.has("diastolicBp") && !obj.get("diastolicBp").isJsonNull()) ext.setDiastolicBp(obj.get("diastolicBp").getAsInt());
            if (obj.has("respiratoryRate") && !obj.get("respiratoryRate").isJsonNull()) ext.setRespiratoryRate(obj.get("respiratoryRate").getAsInt());
            if (obj.has("gcs") && !obj.get("gcs").isJsonNull()) ext.setGcs(obj.get("gcs").getAsInt());
            if (obj.has("painScore") && !obj.get("painScore").isJsonNull()) ext.setPainScore(obj.get("painScore").getAsInt());
            if (obj.has("allergies") && !obj.get("allergies").isJsonNull()) ext.setAllergies(obj.get("allergies").getAsString());
            if (obj.has("knownConditions") && !obj.get("knownConditions").isJsonNull()) ext.setKnownConditions(obj.get("knownConditions").getAsString());
            if (obj.has("reasoning") && !obj.get("reasoning").isJsonNull()) ext.setReasoning(obj.get("reasoning").getAsString());

            if (obj.has("urgency") && !obj.get("urgency").isJsonNull()) {
                try {
                    ext.setUrgency(EmergencyLevel.valueOf(obj.get("urgency").getAsString().toUpperCase()));
                } catch (Exception ignored) {
                }
            }

            if (obj.has("specialty") && !obj.get("specialty").isJsonNull()) {
                try {
                    ext.setSpecialty(Specialty.valueOf(obj.get("specialty").getAsString().toUpperCase()));
                } catch (Exception ignored) {
                }
            }

        } catch (Exception e) {
            System.err.println("Notice: Could not parse AI JSON output: " + e.getMessage());
            return fallbackExtraction(rawFallback);
        }

        if (ext.getChiefComplaint() == null || ext.getChiefComplaint().isEmpty()) {
            ext.setChiefComplaint(rawFallback);
        }
        enrichExtractionWithRegex(ext, rawFallback);
        return ext;
    }

    private AIClinicalExtraction fallbackExtraction(String text) {
        AIClinicalExtraction ext = new AIClinicalExtraction();
        String lower = text.toLowerCase();
        ext.setChiefComplaint(text);

        // Simple local extraction fallbacks for age and vitals if regex matches
        if (lower.contains("male") && !lower.contains("female")) ext.setGender("Male");
        else if (lower.contains("female")) ext.setGender("Female");

        if (lower.contains("ambulance")) ext.setArrivalMode("Ambulance");
        else ext.setArrivalMode("Walk-in");

        if (lower.contains("chest") || lower.contains("heart") || lower.contains("cardiac")) {
            ext.setSpecialty(Specialty.CARDIOLOGY);
            ext.setUrgency(EmergencyLevel.CRITICAL);
            ext.setReasoning("Cardiovascular symptom profile identified.");
        } else if (lower.contains("breath") || lower.contains("asthma") || lower.contains("wheez")) {
            ext.setSpecialty(Specialty.PULMONOLOGY);
            ext.setUrgency(EmergencyLevel.HIGH);
            ext.setReasoning("Pulmonary respiratory distress presentation.");
        } else if (lower.contains("stroke") || lower.contains("paralysis") || lower.contains("slurr")) {
            ext.setSpecialty(Specialty.NEUROLOGY);
            ext.setUrgency(EmergencyLevel.HIGH);
            ext.setReasoning("Acute focal neurological presentation.");
        } else if (lower.contains("fracture") || lower.contains("bone") || lower.contains("sprain")) {
            ext.setSpecialty(Specialty.ORTHOPEDICS);
            ext.setUrgency(EmergencyLevel.MEDIUM);
            ext.setReasoning("Musculoskeletal orthopedic presentation.");
        } else if (lower.contains("bleed") || lower.contains("trauma") || lower.contains("accident")) {
            ext.setSpecialty(Specialty.TRAUMA_SURGERY);
            ext.setUrgency(EmergencyLevel.HIGH);
            ext.setReasoning("Trauma and active injury presentation.");
        } else {
            ext.setSpecialty(Specialty.GENERAL_EMERGENCY);
            ext.setUrgency(EmergencyLevel.MEDIUM);
            ext.setReasoning("General emergency clinical evaluation.");
        }

        enrichExtractionWithRegex(ext, text);
        return ext;
    }

    private void enrichExtractionWithRegex(AIClinicalExtraction ext, String text) {
        if (text == null || text.trim().isEmpty()) return;

        // GCS (e.g. "15 GCS", "GCS 15", "GCS: 15")
        if (ext.getGcs() == null) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)(?:gcs\\s*[:=]?\\s*(\\d{1,2})|(\\d{1,2})\\s*gcs)").matcher(text);
            if (m.find()) {
                String val = m.group(1) != null ? m.group(1) : m.group(2);
                try {
                    int g = Integer.parseInt(val);
                    if (g >= 3 && g <= 15) ext.setGcs(g);
                } catch (Exception ignored) {}
            }
        }
        if (ext.getGcs() == null) {
            ext.setGcs(15);
        }

        // Pain score (e.g. "pain 8", "pain: 8", "8/10 pain")
        if (ext.getPainScore() == null) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)(?:pain\\s*(?:score)?\\s*[:=]?\\s*(\\d{1,2})|(\\d{1,2})\\s*(?:/\\s*10)?\\s*pain)").matcher(text);
            if (m.find()) {
                String val = m.group(1) != null ? m.group(1) : m.group(2);
                try {
                    int p = Integer.parseInt(val);
                    if (p >= 0 && p <= 10) ext.setPainScore(p);
                } catch (Exception ignored) {}
            }
        }
        // If pain score is still null, clinically infer from patient distress / symptom keywords
        if (ext.getPainScore() == null) {
            String lower = text.toLowerCase();
            if (lower.contains("severe") || lower.contains("crushing") || lower.contains("radiating") || lower.contains("unbearable")) {
                ext.setPainScore(8);
            } else if (lower.contains("chest pain") || lower.contains("fracture") || lower.contains("trauma") || lower.contains("laceration") || lower.contains("bleeding")) {
                ext.setPainScore(7);
            } else if (lower.contains("pain") || lower.contains("ache") || lower.contains("fever") || lower.contains("distress")) {
                ext.setPainScore(5);
            } else {
                ext.setPainScore(0);
            }
        }

        // SpO2 (e.g. "SpO2 91%", "91% SpO2")
        if (ext.getSpo2() == null) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)(?:spo2|o2|sat(?:uration)?)\\s*[:=]?\\s*(\\d{2,3})%?").matcher(text);
            if (m.find()) {
                try {
                    int s = Integer.parseInt(m.group(1));
                    if (s >= 50 && s <= 100) ext.setSpo2(s);
                } catch (Exception ignored) {}
            }
        }

        // Heart Rate (e.g. "HR 112", "pulse 112")
        if (ext.getHeartRate() == null) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)(?:hr|pulse|heart\\s*rate)\\s*[:=]?\\s*(\\d{2,3})").matcher(text);
            if (m.find()) {
                try {
                    int hr = Integer.parseInt(m.group(1));
                    if (hr >= 30 && hr <= 250) ext.setHeartRate(hr);
                } catch (Exception ignored) {}
            }
        }

        // Blood Pressure (e.g. "BP 160/95" or "160/95")
        if (ext.getSystolicBp() == null || ext.getDiastolicBp() == null) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)(?:bp|blood\\s*pressure)?\\s*[:=]?\\s*(\\d{2,3})\\s*/\\s*(\\d{2,3})").matcher(text);
            if (m.find()) {
                try {
                    int sys = Integer.parseInt(m.group(1));
                    int dia = Integer.parseInt(m.group(2));
                    if (sys >= 60 && sys <= 260 && dia >= 30 && dia <= 160) {
                        ext.setSystolicBp(sys);
                        ext.setDiastolicBp(dia);
                    }
                } catch (Exception ignored) {}
            }
        }

        // Temperature (e.g. "98.6 F", "102.4F", "37 C")
        if (ext.getTemperature() == null) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)(\\d{2,3}(?:\\.\\d+)?)\\s*(?:f|c|deg|degree|fahrenheit|celsius)").matcher(text);
            if (m.find()) {
                try {
                    double t = Double.parseDouble(m.group(1));
                    if (t >= 30.0 && t <= 110.0) ext.setTemperature(t);
                } catch (Exception ignored) {}
            }
        }

        // Respiratory Rate (e.g. "16/min", "16 resp", "rr 16")
        if (ext.getRespiratoryRate() == null) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)(?:rr|resp(?:iratory)?\\s*rate)?\\s*[:=]?\\s*(\\d{1,2})\\s*(?:/min|bpm|resp)").matcher(text);
            if (m.find()) {
                try {
                    int rr = Integer.parseInt(m.group(1));
                    if (rr >= 5 && rr <= 60) ext.setRespiratoryRate(rr);
                } catch (Exception ignored) {}
            }
        }
    }
}

