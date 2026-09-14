package com.hospital.priority;

import com.hospital.priority.service.QueueService;
import com.hospital.priority.util.DBConnection;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ScreenCaptureTest {

    private static final String ARTIFACTS_DIR = "C:\\Users\\Jamal Khan\\.gemini\\antigravity-ide\\brain\\4058423c-ebee-416c-9859-8057a2fa01a3";

    @Test
    @DisplayName("Render and capture screenshots of all four clinical screens")
    public void captureScreenshots() throws Exception {
        if (!DBConnection.testConnection()) {
            System.out.println("Notice: Database not connected, skipping screen capture.");
            return;
        }

        QueueService.getInstance().initializeQueueFromDatabase();

        CountDownLatch latch = new CountDownLatch(1);

        try {
            Platform.startup(() -> {
                try {
                    captureView("/fxml/dashboard.fxml", "screen_overview.png");
                    captureView("/fxml/register_patient.fxml", "screen_register_patient.png");
                    captureView("/fxml/emergency_queue.fxml", "screen_emergency_queue.png");
                    captureView("/fxml/patient_history.fxml", "screen_patient_records.png");
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        } catch (IllegalStateException e) {
            // Toolkit already started
            Platform.runLater(() -> {
                try {
                    captureView("/fxml/dashboard.fxml", "screen_overview.png");
                    captureView("/fxml/register_patient.fxml", "screen_register_patient.png");
                    captureView("/fxml/emergency_queue.fxml", "screen_emergency_queue.png");
                    captureView("/fxml/patient_history.fxml", "screen_patient_records.png");
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(15, TimeUnit.SECONDS);
    }

    private void captureView(String fxmlPath, String outputFilename) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1100, 720);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        root.applyCss();
        root.layout();

        WritableImage snapshot = root.snapshot(new SnapshotParameters(), null);
        int w = (int) snapshot.getWidth();
        int h = (int) snapshot.getHeight();
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        PixelReader pr = snapshot.getPixelReader();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                bi.setRGB(x, y, pr.getArgb(x, y));
            }
        }

        File outFile = new File(ARTIFACTS_DIR, outputFilename);
        ImageIO.write(bi, "png", outFile);
        System.out.println(">>> Saved screenshot to: " + outFile.getAbsolutePath());
    }
}
