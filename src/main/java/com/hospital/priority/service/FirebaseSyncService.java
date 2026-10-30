package com.hospital.priority.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.hospital.priority.model.Bay;
import com.hospital.priority.model.Order;
import com.hospital.priority.model.Patient;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * ============================================================================
 * HEPS 2.0 FIREBASE REAL-TIME CLOUD CONNECTOR
 * ============================================================================
 * Dual-store real-time synchronizer. Streams patient admissions, triage state,
 * vitals, and bay allocations to Firebase Realtime Database / Firestore REST API.
 *
 * Guarantees zero UI blocking by dispatching all cloud sync calls asynchronously.
 */
public class FirebaseSyncService {

    private static FirebaseSyncService instance;

    private boolean enabled = false;
    private String databaseUrl = "";
    private String authToken = "";
    private final HttpClient httpClient;
    private final Gson gson;

    private FirebaseSyncService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.gson = new Gson();
        loadConfiguration();
    }

    public static synchronized FirebaseSyncService getInstance() {
        if (instance == null) {
            instance = new FirebaseSyncService();
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
            System.err.println("Notice: Could not load Firebase configuration: " + e.getMessage());
        }

        this.enabled = Boolean.parseBoolean(props.getProperty("firebase.enabled", "false").trim());
        this.databaseUrl = props.getProperty("firebase.database.url", "").trim();
        this.authToken = props.getProperty("firebase.auth.token", "").trim();

        if (databaseUrl.endsWith("/")) {
            databaseUrl = databaseUrl.substring(0, databaseUrl.length() - 1);
        }
    }

    public boolean isEnabled() {
        return enabled && !databaseUrl.isEmpty() && !databaseUrl.contains("your-project-id");
    }

    public String getStatusDescription() {
        if (isEnabled()) {
            return "Firebase Cloud Sync: Connected";
        }
        return "Cloud Sync: Standalone (Local MySQL)";
    }

    /**
     * Asynchronously syncs patient entity to Firebase.
     */
    public CompletableFuture<Void> syncPatientAsync(Patient patient) {
        if (!isEnabled() || patient == null) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                String token = patient.getTokenNo() != null ? patient.getTokenNo() : "TEMP-" + System.currentTimeMillis();
                String path = "/patients/" + sanitizeKey(token) + ".json";
                if (!authToken.isEmpty()) {
                    path += "?auth=" + authToken;
                }

                JsonObject obj = new JsonObject();
                obj.addProperty("tokenNo", patient.getTokenNo());
                obj.addProperty("uhid", patient.getUhid());
                obj.addProperty("fullName", patient.getFullName());
                obj.addProperty("age", patient.getAge());
                obj.addProperty("gender", patient.getGender());
                obj.addProperty("phone", patient.getPhone());
                obj.addProperty("symptoms", patient.getSymptoms());
                obj.addProperty("emergencyLevel", patient.getEmergencyLevel() != null ? patient.getEmergencyLevel().name() : "LOW");
                obj.addProperty("status", patient.getStatus() != null ? patient.getStatus().name() : "WAITING");
                obj.addProperty("assignedSpecialty", patient.getAssignedSpecialty() != null ? patient.getAssignedSpecialty().name() : "GENERAL_EMERGENCY");
                obj.addProperty("assignedBay", patient.getAssignedBay());
                obj.addProperty("assignedDoctor", patient.getAssignedDoctorName());
                obj.addProperty("lastSyncTime", System.currentTimeMillis());

                if (patient.getVitals() != null) {
                    JsonObject vitals = new JsonObject();
                    vitals.addProperty("spo2", patient.getVitals().getSpo2());
                    vitals.addProperty("heartRate", patient.getVitals().getHeartRate());
                    vitals.addProperty("temperature", patient.getVitals().getTemperature());
                    vitals.addProperty("systolicBp", patient.getVitals().getSystolicBp());
                    vitals.addProperty("diastolicBp", patient.getVitals().getDiastolicBp());
                    vitals.addProperty("respiratoryRate", patient.getVitals().getRespiratoryRate());
                    obj.add("vitals", vitals);
                }

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(databaseUrl + path))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(6))
                        .PUT(HttpRequest.BodyPublishers.ofString(obj.toString()))
                        .build();

                HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                    System.err.println("Notice: Firebase sync returned status " + resp.statusCode());
                }
            } catch (Exception e) {
                System.err.println("Notice: Firebase patient sync failed: " + e.getMessage());
            }
        });
    }

    /**
     * Asynchronously syncs bay status to Firebase.
     */
    public CompletableFuture<Void> syncBayAsync(Bay bay) {
        if (!isEnabled() || bay == null) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                String path = "/bays/" + sanitizeKey(bay.getName()) + ".json";
                if (!authToken.isEmpty()) {
                    path += "?auth=" + authToken;
                }

                JsonObject obj = new JsonObject();
                obj.addProperty("name", bay.getName());
                obj.addProperty("status", bay.getStatus().name());
                obj.addProperty("isAvailable", bay.isAvailable());
                String patientName = bay.getCurrentPatientName();
                if (patientName != null) {
                    obj.addProperty("currentPatient", patientName);
                }
                obj.addProperty("lastUpdated", System.currentTimeMillis());

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(databaseUrl + path))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(6))
                        .PUT(HttpRequest.BodyPublishers.ofString(obj.toString()))
                        .build();

                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                System.err.println("Notice: Firebase bay sync failed: " + e.getMessage());
            }
        });
    }

    private String sanitizeKey(String key) {
        if (key == null) return "null";
        return key.replace(".", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_").replace("/", "_");
    }
}

