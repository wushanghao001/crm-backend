package com.example.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task")
public class Task {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String title;

    private String content;

    private String taskType;

    private Integer relatedCustomerId;

    private Integer relatedFollowId;

    private LocalDateTime dueDate;

    private String priority;

    private String status;

    private Integer assigneeId;

    private Integer creatorId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}