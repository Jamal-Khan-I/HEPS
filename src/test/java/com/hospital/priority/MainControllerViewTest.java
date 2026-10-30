package com.hospital.priority;

import com.hospital.priority.controller.MainController;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNull;

public class MainControllerViewTest {

    @Test
    public void testShowRegisterPatientFromMain() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Runnable test = () -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main_layout.fxml"));
                Parent root = loader.load();
                MainController mc = loader.getController();
                mc.showRegisterPatient(null);
                System.out.println("SHOW REGISTER PATIENT SUCCESS!");
            } catch (Throwable t) {
                System.err.println("FAILED SHOW REGISTER PATIENT:");
                t.printStackTrace();
                if (t.getCause() != null) {
                    System.err.println("CAUSE 1: " + t.getCause());
                    t.getCause().printStackTrace();
                    if (t.getCause().getCause() != null) {
                        System.err.println("CAUSE 2: " + t.getCause().getCause());
                        t.getCause().getCause().printStackTrace();
                    }
                }
                error.set(t);
            } finally {
                latch.countDown();
            }
        };

        try {
            Platform.startup(test);
        } catch (IllegalStateException e) {
            Platform.runLater(test);
        }

        latch.await(10, TimeUnit.SECONDS);
        assertNull(error.get(), () -> "Failed in testShowRegisterPatientFromMain: " + error.get());
    }
}

