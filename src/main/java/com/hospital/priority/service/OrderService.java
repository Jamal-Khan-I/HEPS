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
     * Seeds initial orders matching Pages 10 and 11 for immediate realism and testing.
     */
    private void seedInitialOrders() {
        orders.clear();

        // 1. Ramesh Kumar (Bay 1 Resus, TK-0142)
        Order o1 = new Order(1, "TK-0142", "Ramesh Kumar", "Bay 1 · Resus",
                Order.OrderType.CARDIAC_ECG, "12-lead ECG", "Dr. Mehta");
        o1.setOrderId(1);
        o1.setStatus(Order.OrderStatus.RESULTED);
        o1.setOrderedAt(LocalDateTime.now().minusMinutes(27));
        o1.setResultedAt(LocalDateTime.now().minusMinutes(20));
        o1.addResultItem(new OrderResultItem("Rhythm", "Sinus Tachycardia", "Normal Sinus", "Normal"));
        o1.addResultItem(new OrderResultItem("ST Segment", "ST Elevation 2mm V2-V4", "Isoelectric", "High"));
        orders.add(o1);

        Order o2 = new Order(1, "TK-0142", "Ramesh Kumar", "Bay 1 · Resus",
                Order.OrderType.LAB, "Troponin I", "Dr. Mehta");
        o2.setOrderId(2);
        o2.setStatus(Order.OrderStatus.IN_PROGRESS);
        o2.setAbnormal(true);
        o2.setOrderedAt(LocalDateTime.now().minusMinutes(26));
        o2.setResultedAt(LocalDateTime.now().minusMinutes(1));
        o2.addResultItem(new OrderResultItem("Troponin I", "0.42 ng/mL", "under 0.04", "High"));
        o2.addResultItem(new OrderResultItem("CK-MB", "8 ng/mL", "0 to 5", "High"));
        o2.addResultItem(new OrderResultItem("Potassium", "4.1 mmol/L", "3.5 to 5.0", "Normal"));
        orders.add(o2);

        Order o3 = new Order(1, "TK-0142", "Ramesh Kumar", "Bay 1 · Resus",
                Order.OrderType.MEDICATION, "Aspirin 300 mg, oral", "Dr. Mehta");
        o3.setOrderId(3);
        o3.setStatus(Order.OrderStatus.ORDERED);
        o3.setOrderedAt(LocalDateTime.now().minusMinutes(25));
        orders.add(o3);

        // 2. Anita Joshi (Bay 3 Acute, TK-0143)
        Order o4 = new Order(2, "TK-0143", "Anita Joshi", "Bay 3 · Acute",
                Order.OrderType.IMAGING, "X-ray left leg", "Dr. Rao");
        o4.setOrderId(4);
        o4.setStatus(Order.OrderStatus.IN_PROGRESS);
        o4.setOrderedAt(LocalDateTime.now().minusMinutes(20));
        orders.add(o4);

        // 3. Kavita Sundaram (Bay 4 Obs, TK-0140)
        Order o5 = new Order(3, "TK-0140", "Kavita Sundaram", "Bay 4 · Obs",
                Order.OrderType.LAB, "CBC", "Dr. Miller");
        o5.setOrderId(5);
        o5.setStatus(Order.OrderStatus.ORDERED);
        o5.setOrderedAt(LocalDateTime.now().minusMinutes(12));
        orders.add(o5);
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
