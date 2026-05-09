package com.example.crm.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TrendResponse {
    private List<String> dates = new ArrayList<>();
    private List<Integer> wonData = new ArrayList<>();
    private List<Integer> lostData = new ArrayList<>();
    private List<Integer> totalData = new ArrayList<>();
}