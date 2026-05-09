
package com.example.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("service_ticket")
public class ServiceTicket {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long customerId;

    private String customerName;

    private String type;

    private String title;

    private String description;

    private String priority;

    private String status;

    private Long assigneeId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
