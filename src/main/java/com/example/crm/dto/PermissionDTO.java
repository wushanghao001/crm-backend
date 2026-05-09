package com.example.crm.dto;

import lombok.Data;

import java.util.List;

@Data
public class PermissionDTO {
    private Long id;
    private String name;
    private String code;
    private String path;
    private Integer sort;
    private Long parentId;
    private String icon;
    private String type;
    private List<PermissionDTO> children;
}

@Data
class RolePermissionDTO {
    private Long roleId;
    private List<String> permissionCodes;
}
