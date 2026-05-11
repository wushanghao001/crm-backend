package com.example.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("project_code")
public class ProjectCode {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String code;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}