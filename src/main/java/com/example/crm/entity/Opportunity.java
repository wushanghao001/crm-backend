
package com.example.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("opportunity")
public class Opportunity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long customerId;

    private String name;

    private String stage;

    private BigDecimal amount;

    private Double probability;

    private LocalDateTime expectedCloseDate;

    private String description;

    private Long ownerId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
