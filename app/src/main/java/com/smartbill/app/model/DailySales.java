package com.smartbill.app.model;

import java.math.BigDecimal;

public class DailySales {

    private final String date;
    private final BigDecimal totalSales;

    public DailySales(String date, BigDecimal totalSales) {
        this.date = date;
        this.totalSales = totalSales;
    }

    public String getDate() {
        return date;
    }

    public BigDecimal getTotalSales() {
        return totalSales;
    }
}