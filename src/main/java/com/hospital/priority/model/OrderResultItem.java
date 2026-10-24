package com.hospital.priority.model;

import java.io.Serializable;

/**
 * Diagnostic test result item (Page 11).
 * Displays Test Name, Value, Reference Range, and Flag (High, Low, Normal).
 */
public class OrderResultItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private String testName;
    private String value;
    private String referenceRange;
    private String flag; // "High", "Low", "Normal"

    public OrderResultItem() {
    }

    public OrderResultItem(String testName, String value, String referenceRange, String flag) {
        this.testName = testName;
        this.value = value;
        this.referenceRange = referenceRange;
        this.flag = flag;
    }

    public String getTestName() {
        return testName;
    }

    public String getParameterName() {
        return getTestName();
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getReferenceRange() {
        return referenceRange;
    }

    public void setReferenceRange(String referenceRange) {
        this.referenceRange = referenceRange;
    }

    public String getFlag() {
        return flag != null ? flag : "Normal";
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

    public boolean isAbnormal() {
        return "High".equalsIgnoreCase(flag) || "Low".equalsIgnoreCase(flag) || "Abnormal".equalsIgnoreCase(flag);
    }
}
