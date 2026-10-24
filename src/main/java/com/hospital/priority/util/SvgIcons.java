package com.hospital.priority.util;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.SVGPath;

/**
 * ============================================================================
 * VECTOR SVG ICON REPOSITORY (Pages 1 - 18)
 * ============================================================================
 * High-performance, resolution-independent vector icons rendered via pure JavaFX SVGPath.
 * Completely replaces emojis and prevents broken unicode boxes across all platforms.
 */
public class SvgIcons {

    // Common SVG Paths (Tabler / Feather / Material Design line paths)
    public static final String BRAND_PULSE = "M3 12h4l3 8 4-16 3 8h4";
    public static final String COMMAND_CENTER = "M4 4h6v6H4zm10 0h6v6h-6zM4 14h6v6H4zm10 0h6v6h-6z";
    public static final String REGISTER_PATIENT = "M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2 M8.5 7a4 4 0 1 0 0-8 4 4 0 0 0 0 8z M20 8v6M23 11h-6";
    public static final String EMERGENCY_QUEUE = "M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01";
    public static final String BAY_BOARD = "M3 7v11M3 14h18M21 7v11M7 10h10a4 4 0 0 1 4 4v0H3v0a4 4 0 0 1 4-4z";
    public static final String MASS_CASUALTY = "M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0zM12 9v4M12 17h.01";
    public static final String MY_PATIENTS = "M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2 M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z M23 21v-2a4 4 0 0 0-3-3.87 M16 3.13a4 4 0 0 1 0 7.75";
    public static final String ORDERS_RESULTS = "M10 2v7.31M14 2v7.31M8.5 2h7M14 9.3l4.9 8.27A2 2 0 0 1 17.2 21H6.8a2 2 0 0 1-1.7-3.43L10 9.3";
    public static final String PATIENT_RECORDS = "M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z";
    public static final String DISCHARGE = "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9";
    public static final String REPORTS = "M18 20V10M12 20V4M6 20v-6";
    public static final String ALERTS_BELL = "M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 0 1-3.46 0";
    public static final String ADMIN_SETTINGS = "M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z";
    public static final String SEARCH = "M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16z M21 21l-4.35-4.35";
    public static final String CLOCK = "M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z M12 6v6l4 2";
    public static final String CHECK = "M20 6L9 17l-5-5";
    public static final String PRINTER = "M6 9V2h12v7M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2M6 14h12v8H6z";
    public static final String LOGOUT = "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9";
    public static final String CHEVRON_RIGHT = "M9 18l6-6-6-6";
    public static final String USER = "M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2 M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z";
    public static final String LOCK = "M19 11H5a2 2 0 0 0-2 2v7a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7a2 2 0 0 0-2-2zM7 11V7a5 5 0 0 1 10 0v4";
    public static final String EYE = "M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8zM12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6z";
    public static final String ECG = "M22 12h-4l-3 9L9 3l-3 9H2";
    public static final String PLUS = "M12 5v14M5 12h14";
    public static final String REFRESH = "M23 4v6h-6M1 20v-6h6M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15";
    public static final String RADAR = "M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2zm0 4a6 6 0 1 0 6 6 6 6 0 0 0-6-6zm0 4a2 2 0 1 0 2 2 2 2 0 0 0-2-2z";

    /**
     * Creates an SVG icon node with specified path and stroke color.
     */
    public static Node createIcon(String pathContent, String strokeColorHex, double size) {
        SVGPath path = new SVGPath();
        path.setContent(pathContent);
        path.setFill(Color.TRANSPARENT);
        path.setStroke(Color.web(strokeColorHex));
        path.setStrokeWidth(2.0);

        Region iconBox = new Region();
        iconBox.setShape(path);
        iconBox.setMinSize(size, size);
        iconBox.setPrefSize(size, size);
        iconBox.setMaxSize(size, size);

        // If path is fill-based vs stroke-based, SVGPath directly in an HBox works wonderfully
        HBox container = new HBox(path);
        container.setAlignment(Pos.CENTER);
        container.setMinSize(size, size);
        container.setPrefSize(size, size);
        container.setMaxSize(size, size);
        return container;
    }

    /**
     * Creates a filled SVG icon (for solid badges or icons).
     */
    public static SVGPath createFilledIcon(String pathContent, String fillHex, double scale) {
        SVGPath path = new SVGPath();
        path.setContent(pathContent);
        path.setFill(Color.web(fillHex));
        path.setStroke(null);
        path.setScaleX(scale);
        path.setScaleY(scale);
        return path;
    }

    /**
     * Creates a stroked line SVGPath.
     */
    public static SVGPath createStrokedPath(String pathContent, String strokeHex, double strokeWidth) {
        SVGPath path = new SVGPath();
        path.setContent(pathContent);
        path.setFill(Color.TRANSPARENT);
        path.setStroke(Color.web(strokeHex));
        path.setStrokeWidth(strokeWidth);
        return path;
    }
}
