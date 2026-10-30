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
     * Orders start clean and empty, populated dynamically as real clinical orders are placed.
     */
    private void seedInitialOrders() {
        orders.clear();
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
