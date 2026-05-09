package com.example.crm.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class UserPerformanceTrendResponse {
    private List<String> months;
    private List<BigDecimal> salesData;
    private List<Integer> orderCountData;
    private List<String> customerSources;
    private List<Integer> sourceCounts;
}