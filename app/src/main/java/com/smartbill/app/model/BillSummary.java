
package com.smartbill.app.model;

public class BillSummary {

    private final long billId;
    private final String totalAmount;
    private final String paymentMethod;
    private final String paymentStatus;
    private final String createdAt;

    public BillSummary(
            long billId,
            String totalAmount,
            String paymentMethod,
            String paymentStatus,
            String createdAt) {

        this.billId = billId;
        this.totalAmount = totalAmount;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.createdAt = createdAt;
    }

    public long getBillId() {
        return billId;
    }

    public String getTotalAmount() {
        return totalAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}

