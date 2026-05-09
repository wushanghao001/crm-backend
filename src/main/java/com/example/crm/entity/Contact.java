
package com.example.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("contact")
public class Contact {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long customerId;

    private Long creatorId;

    private String name;

    private String phone;

    private String position;

    private String email;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
