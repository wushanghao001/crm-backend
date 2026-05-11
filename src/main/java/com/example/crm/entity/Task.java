package com.example.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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

    @TableField("task_type")
    private String taskType;

    @TableField("related_customer_id")
    private Integer relatedCustomerId;

    @TableField("related_follow_id")
    private Integer relatedFollowId;

    private LocalDateTime dueDate;

    private String priority;

    private String status;

    @TableField("assignee_id")
    private Integer assigneeId;

    @TableField("creator_id")
    private Integer creatorId;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}