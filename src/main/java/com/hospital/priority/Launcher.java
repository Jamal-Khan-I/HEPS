package com.hospital.priority;

/**
 * ============================================================================
 * HEPS STANDALONE EXECUTABLE BOOTSTRAP LAUNCHER
 * ============================================================================
 * Acts as the native entry point for packaging into a Windows .exe application.
 * By not extending javafx.application.Application directly, it allows JavaFX 21
 * to bootstrap seamlessly from a standalone packaged runtime or fat JAR.
 */
public class Launcher {

    public static void main(String[] args) {
        App.main(args);
    }
}
