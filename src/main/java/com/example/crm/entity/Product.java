
package com.example.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String category;

    private String code;

    private BigDecimal price;

    private Integer stock;

    private Integer availableStock;

    private Integer lockedStock;

    private Integer safeStock;

    private String description;

    private Integer status;

    private Long projectCodeId;

    private String projectCodeName;

    private Long materialCodeId;

    private String materialCodeName;

    private Long brandCodeId;

    private String brandCodeName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
