package com.example.crm.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class UserPerformanceResponse {
    private BigDecimal totalSales;
    private int wonCustomerCount;
    private int followCustomerCount;
    private double winRate;
    private double monthTargetCompletion;
    private BigDecimal monthTarget;
    private BigDecimal currentMonthSales;
    private int leads;
    private int contacted;
    private int quoted;
    private int won;
    private int lost;
    private double contactRate;
    private double quoteRate;
    private double winRateStage;
    private double totalWinRate;
}