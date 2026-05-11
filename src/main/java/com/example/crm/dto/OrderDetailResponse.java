package com.example.crm.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDetailResponse {

    private Long id;

    private String orderNo;

    private Long customerId;

    private String customerName;

    private String contactName;

    private String phone;

    private String email;

    private BigDecimal totalAmount;

    private BigDecimal discountAmount;

    private BigDecimal paidAmount;

    private String status;

    private String payStatus;

    private String paymentMethod;

    private LocalDateTime paidAt;

    private String transactionNo;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<OrderItemDTO> items;

    @Data
    public static class OrderItemDTO {
        private Long id;
        private Long orderId;
        private String productName;
        private String productCode;
        private Long projectCodeId;
        private String projectCodeName;
        private Long materialCodeId;
        private String materialCodeName;
        private Long brandCodeId;
        private String brandCodeName;
        private BigDecimal unitPrice;
        private Integer quantity;
        private BigDecimal subtotal;
    }
}