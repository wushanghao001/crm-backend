package com.example.crm.dto;

import lombok.Data;

@Data
public class FunnelResponse {
    private Integer leads = 0;
    private Integer contacted = 0;
    private Integer quoted = 0;
    private Integer won = 0;
    private Integer lost = 0;
    private Double contactRate = 0.0;
    private Double quoteRate = 0.0;
    private Double winRate = 0.0;
    private Double totalWinRate = 0.0;
}