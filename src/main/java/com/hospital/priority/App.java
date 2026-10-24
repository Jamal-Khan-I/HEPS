package com.hospital.priority;

import com.hospital.priority.service.PatientService;
import com.hospital.priority.service.QueueService;
import com.hospital.priority.util.DBConnection;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Main JavaFX Application Entry Point.
 * Orchestrates application lifecycle, database bootstrapping, and PriorityQueue initialization.
 */
public class App extends Application {

    @Override
    public void init() {
        // Step 1: Check database connectivity and bootstrap in-memory PriorityQueue
        System.out.println(">>> Initializing Hospital Emergency Priority System...");
        try {
            if (DBConnection.testConnection()) {
                System.out.println(">>> Database connected successfully. Bootstrapping PriorityQueue from MySQL...");
                QueueService.getInstance().initializeQueueFromDatabase();
                PatientService.getInstance().initializeSequence();
                System.out.println(">>> PriorityQueue successfully loaded with "
                        + QueueService.getInstance().getQueueSize() + " waiting patients.");
            } else {
                System.err.println(">>> Warning: MySQL database connection check failed. Using empty in-memory queue.");
            }
        } catch (Exception e) {
            System.err.println(">>> Notice during initialization: " + e.getMessage());
        }
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1280, 760);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

            primaryStage.setTitle("HEPS – City General Hospital Emergency Department");
            primaryStage.setMinWidth(1180);
            primaryStage.setMinHeight(720);
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showFatalError("Application Startup Error", "Failed to load primary interface:\n" + e.getMessage());
        }
    }

    private void showFatalError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("Fatal Startup Failure");
        alert.setContentText(message);
        alert.showAndWait();
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
