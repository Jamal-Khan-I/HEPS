package com.hospital.priority.service;

import com.hospital.priority.model.Order;
import com.hospital.priority.model.OrderResultItem;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ============================================================================
 * CLINICAL ORDER & DIAGNOSTIC RESULTS SERVICE (Pages 10 & 11)
 * ============================================================================
 * Manages orders for laboratory tests, imaging, cardiac ECG, medications, and IV fluids.
 * Tracks order statuses (ORDERED -> IN_PROGRESS -> RESULTED) and flags abnormal findings.
 */
public class OrderService {

    private static OrderService instance;

    private final List<Order> orders = new CopyOnWriteArrayList<>();

    private OrderService() {
        seedInitialOrders();
    }

    public static synchronized OrderService getInstance() {
        if (instance == null) {
            instance = new OrderService();
        }
        return instance;
    }

    /**
     * Seeds initial orders matching the active clinical cohort for authentic realism.
     */
    private void seedInitialOrders() {
        orders.clear();

        // 1. Ramesh Kumar (Bay 1 Resus, TK-2026-0101, Cardiology)
        Order o1 = new Order(1, "TK-2026-0101", "Ramesh Kumar", "Bay 1 · Resus",
                Order.OrderType.CARDIAC_ECG, "12-lead ECG", "Dr. Rajesh Sharma");
        o1.setOrderId(1);
        o1.setStatus(Order.OrderStatus.RESULTED);
        o1.setOrderedAt(LocalDateTime.now().minusMinutes(24));
        o1.setResultedAt(LocalDateTime.now().minusMinutes(18));
        o1.addResultItem(new OrderResultItem("Rhythm", "Sinus Tachycardia", "Normal Sinus", "Normal"));
        o1.addResultItem(new OrderResultItem("ST Segment", "ST Elevation 2.5mm V2-V4", "Isoelectric", "High"));
        orders.add(o1);

        Order o2 = new Order(1, "TK-2026-0101", "Ramesh Kumar", "Bay 1 · Resus",
                Order.OrderType.LAB, "High-Sensitivity Troponin I", "Dr. Rajesh Sharma");
        o2.setOrderId(2);
        o2.setStatus(Order.OrderStatus.IN_PROGRESS);
        o2.setAbnormal(true);
        o2.setOrderedAt(LocalDateTime.now().minusMinutes(22));
        o2.setResultedAt(LocalDateTime.now().minusMinutes(2));
        o2.addResultItem(new OrderResultItem("Troponin I", "0.48 ng/mL", "under 0.04", "High"));
        o2.addResultItem(new OrderResultItem("CK-MB", "9.2 ng/mL", "0 to 5", "High"));
        o2.addResultItem(new OrderResultItem("Potassium", "4.2 mmol/L", "3.5 to 5.0", "Normal"));
        orders.add(o2);

        Order o3 = new Order(1, "TK-2026-0101", "Ramesh Kumar", "Bay 1 · Resus",
                Order.OrderType.MEDICATION, "Aspirin 300 mg (chewable)", "Dr. Rajesh Sharma");
        o3.setOrderId(3);
        o3.setStatus(Order.OrderStatus.RESULTED);
        o3.setOrderedAt(LocalDateTime.now().minusMinutes(20));
        orders.add(o3);

        // 2. Anita Joshi (Bay 3 Acute, TK-2026-0102, Orthopedics)
        Order o4 = new Order(2, "TK-2026-0102", "Anita Joshi", "Bay 3 · Acute",
                Order.OrderType.IMAGING, "X-ray Right Leg (AP & Lateral)", "Dr. Vikram Patel");
        o4.setOrderId(4);
        o4.setStatus(Order.OrderStatus.IN_PROGRESS);
        o4.setOrderedAt(LocalDateTime.now().minusMinutes(17));
        orders.add(o4);

        Order o5 = new Order(2, "TK-2026-0102", "Anita Joshi", "Bay 3 · Acute",
                Order.OrderType.MEDICATION, "IV Paracetamol 1g infusion", "Dr. Vikram Patel");
        o5.setOrderId(5);
        o5.setStatus(Order.OrderStatus.RESULTED);
        o5.setOrderedAt(LocalDateTime.now().minusMinutes(15));
        orders.add(o5);

        // 3. Meera Nair (Bay 2 Resus, TK-2026-0103, Pulmonology)
        Order o6 = new Order(3, "TK-2026-0103", "Meera Nair", "Bay 2 · Resus",
                Order.OrderType.LAB, "Arterial Blood Gas (ABG)", "Dr. Ananya Sen");
        o6.setOrderId(6);
        o6.setStatus(Order.OrderStatus.IN_PROGRESS);
        o6.setAbnormal(true);
        o6.setOrderedAt(LocalDateTime.now().minusMinutes(14));
        o6.addResultItem(new OrderResultItem("pH", "7.31", "7.35 to 7.45", "Low"));
        o6.addResultItem(new OrderResultItem("PaO2", "64 mmHg", "75 to 100", "Low"));
        o6.addResultItem(new OrderResultItem("PaCO2", "48 mmHg", "35 to 45", "High"));
        orders.add(o6);

        // 4. Robert D'Souza (Bay 4 Acute, TK-2026-0104, Neurology)
        Order o7 = new Order(4, "TK-2026-0104", "Robert D'Souza", "Bay 4 · Acute",
                Order.OrderType.IMAGING, "Non-Contrast Brain CT (Stroke Protocol)", "Dr. Priya Nair");
        o7.setOrderId(7);
        o7.setStatus(Order.OrderStatus.ORDERED);
        o7.setOrderedAt(LocalDateTime.now().minusMinutes(10));
        orders.add(o7);

        // 5. Vikram Malhotra (Bay 5 Acute, TK-2026-0105, Trauma Surgery)
        Order o8 = new Order(5, "TK-2026-0105", "Vikram Malhotra", "Bay 5 · Acute",
                Order.OrderType.PROCEDURE, "Wound Exploration & Primary Closure", "Dr. Arjun Mehta");
        o8.setOrderId(8);
        o8.setStatus(Order.OrderStatus.ORDERED);
        o8.setOrderedAt(LocalDateTime.now().minusMinutes(6));
        orders.add(o8);
    }

    public List<Order> getAllOrders() {
        return Collections.unmodifiableList(orders);
    }

    public List<Order> getOrdersForPatient(String tokenNo) {
        if (tokenNo == null) return Collections.emptyList();
        List<Order> list = new ArrayList<>();
        for (Order o : orders) {
            if (tokenNo.equalsIgnoreCase(o.getTokenNo()) || (o.getTokenNo() != null && o.getTokenNo().endsWith(tokenNo))) {
                list.add(o);
            }
        }
        return list;
    }

    public List<Order> getPendingOrders() {
        List<Order> list = new ArrayList<>();
        for (Order o : orders) {
            if (o.getStatus() != Order.OrderStatus.RESULTED) {
                list.add(o);
            }
        }
        return list;
    }

    public List<Order> getResultedOrders() {
        List<Order> list = new ArrayList<>();
        for (Order o : orders) {
            if (o.getStatus() == Order.OrderStatus.RESULTED) {
                list.add(o);
            }
        }
        return list;
    }

    public synchronized Order createOrder(int patientId, String tokenNo, String patientName,
                                         String bayName, Order.OrderType type, String orderName, String doctorName) {
        Order o = new Order(patientId, tokenNo, patientName, bayName, type, orderName, doctorName);
        o.setOrderId(orders.size() + 1);
        orders.add(0, o);
        AuditService.getInstance().log("ORDER_PLACED", tokenNo, null, null, orderName, "Ordered by " + doctorName);
        return o;
    }

    public synchronized void acknowledgeResult(int orderId, String staffName) {
        for (Order o : orders) {
            if (o.getOrderId() == orderId) {
                o.setStatus(Order.OrderStatus.RESULTED);
                AuditService.getInstance().log("RESULT_ACKNOWLEDGED", o.getTokenNo(), null, "IN_PROGRESS", "RESULTED", "Acknowledged by " + staffName);
                break;
            }
        }
    }
}
