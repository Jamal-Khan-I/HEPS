package com.hospital.priority;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNull;

public class RegisterPatientFxmlTest {

    @Test
    public void testLoadRegisterPatient() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Runnable test = () -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register_patient.fxml"));
                loader.load();
                System.out.println("LOADED REGISTER PATIENT SUCCESS!");
            } catch (Throwable t) {
                System.err.println("FAILED TO LOAD REGISTER PATIENT:");
                t.printStackTrace();
                if (t.getCause() != null) {
                    System.err.println("CAUSE:");
                    t.getCause().printStackTrace();
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
        assertNull(error.get(), () -> "Failed to load /fxml/register_patient.fxml: " + error.get().getMessage());
    }
}


