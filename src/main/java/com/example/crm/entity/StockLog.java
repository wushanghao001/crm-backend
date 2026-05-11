package com.example.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("stock_log")
public class StockLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    private Long orderId;

    private String type;

    private Integer num;

    private Integer beforeStock;

    private Integer afterStock;

    private String remark;

    private LocalDateTime createTime;
}
