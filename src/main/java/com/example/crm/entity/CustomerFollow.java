package com.example.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("customer_follow")
public class CustomerFollow {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer customerId;

    private String followType;

    private String followResult;

    private String intentLevel;

    private String content;

    private String remark;

    private LocalDateTime nextFollowTime;

    private String attachment;

    private Boolean remindFlag;

    private Integer followUserId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}