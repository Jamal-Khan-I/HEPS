package com.hospital.priority.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Diagnostic and therapeutic clinical order (Page 10, 11).
 * Supports Lab tests, Imaging, 12-lead ECG, Medications, IV Fluids, and Procedures.
 */
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum OrderType {
        LAB("Lab"),
        IMAGING("Imaging"),
        CARDIAC_ECG("Cardiac"),
        MEDICATION("Medication"),
        IV_FLUIDS("IV Fluids"),
        PROCEDURE("Procedure");

        private final String displayName;

        OrderType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum OrderStatus {
        ORDERED("Ordered", "#64748B"),
        IN_PROGRESS("In progress", "#F59E0B"),
        RESULTED("Resulted", "#10B981");

        private final String displayName;
        private final String colorHex;

        OrderStatus(String displayName, String colorHex) {
            this.displayName = displayName;
            this.colorHex = colorHex;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getColorHex() {
            return colorHex;
        }
    }

    private int orderId;
    private int patientId;
    private String tokenNo;
    private String patientName;
    private String bayName;
    private OrderType orderType;
    private String orderName;
    private OrderStatus status;
    private boolean abnormal;
    private LocalDateTime orderedAt;
    private LocalDateTime resultedAt;
    private String doctorName;
    private List<OrderResultItem> resultItems = new ArrayList<>();

    public static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public Order() {
        this.status = OrderStatus.ORDERED;
        this.orderedAt = LocalDateTime.now();
    }

    public Order(int patientId, String tokenNo, String patientName, String bayName,
                 OrderType orderType, String orderName, String doctorName) {
        this.patientId = patientId;
        this.tokenNo = tokenNo;
        this.patientName = patientName;
        this.bayName = bayName;
        this.orderType = orderType;
        this.orderName = orderName;
        this.doctorName = doctorName;
        this.status = OrderStatus.ORDERED;
        this.orderedAt = LocalDateTime.now();
        this.abnormal = false;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getPatientId() {
        return patientId;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
    }

    public String getTokenNo() {
        return tokenNo;
    }

    public void setTokenNo(String tokenNo) {
        this.tokenNo = tokenNo;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getBayName() {
        return bayName != null ? bayName : "Bay 1";
    }

    public void setBayName(String bayName) {
        this.bayName = bayName;
    }

    public OrderType getOrderType() {
        return orderType != null ? orderType : OrderType.LAB;
    }

    public OrderType getType() {
        return getOrderType();
    }

    public void setOrderType(OrderType orderType) {
        this.orderType = orderType;
    }

    public String getOrderName() {
        return orderName;
    }

    public void setOrderName(String orderName) {
        this.orderName = orderName;
    }

    public OrderStatus getStatus() {
        return status != null ? status : OrderStatus.ORDERED;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public boolean isAbnormal() {
        return abnormal;
    }

    public void setAbnormal(boolean abnormal) {
        this.abnormal = abnormal;
    }

    public LocalDateTime getOrderedAt() {
        return orderedAt;
    }

    public void setOrderedAt(LocalDateTime orderedAt) {
        this.orderedAt = orderedAt;
    }

    public LocalDateTime getResultedAt() {
        return resultedAt;
    }

    public void setResultedAt(LocalDateTime resultedAt) {
        this.resultedAt = resultedAt;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public List<OrderResultItem> getResultItems() {
        return resultItems;
    }

    public void setResultItems(List<OrderResultItem> resultItems) {
        this.resultItems = resultItems != null ? resultItems : new ArrayList<>();
    }

    public void addResultItem(OrderResultItem item) {
        if (item != null) {
            this.resultItems.add(item);
            if ("High".equalsIgnoreCase(item.getFlag()) || "Low".equalsIgnoreCase(item.getFlag()) || "Abnormal".equalsIgnoreCase(item.getFlag())) {
                this.abnormal = true;
            }
        }
    }

    public String getFormattedOrderedTime() {
        if (orderedAt == null) return "";
        return "Ordered " + orderedAt.format(TIME_FMT);
    }

    public String getFormattedResultedTime() {
        if (resultedAt == null) return "";
        return "resulted " + resultedAt.format(TIME_FMT);
    }
}
