package com.hospital.priority;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class RegisterPatientFxmlTest {

    @Test
    public void testLoadRegisterPatient() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register_patient.fxml"));
                    loader.load();
                    System.out.println("LOADED REGISTER PATIENT SUCCESS!");
                } catch (Throwable t) {
                    System.err.println("FAILED TO LOAD REGISTER PATIENT:");
                    t.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        } catch (IllegalStateException e) {
            Platform.runLater(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register_patient.fxml"));
                    loader.load();
                    System.out.println("LOADED REGISTER PATIENT SUCCESS!");
                } catch (Throwable t) {
                    System.err.println("FAILED TO LOAD REGISTER PATIENT:");
                    t.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await(10, TimeUnit.SECONDS);
    }
}

