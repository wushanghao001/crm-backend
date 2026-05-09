package com.example.crm.dto;

import lombok.Data;

import java.util.List;

@Data
public class MenuResponse {
    private Long id;
    private String name;
    private String code;
    private String path;
    private String icon;
    private Integer sort;
    private Long parentId;
    private String type;
    private List<MenuResponse> children;
}
