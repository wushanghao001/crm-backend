package com.example.crm.controller;

import com.example.crm.dto.ApiResponse;
import com.example.crm.dto.OrderDetailResponse;
import com.example.crm.dto.PageResponse;
import com.example.crm.entity.Order;
import com.example.crm.entity.OrderItem;
import com.example.crm.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<Order>>> listOrders(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String payStatus) {

        PageResponse<Order> response = orderService.listOrders(pageNum, pageSize, keyword, status, payStatus);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getOrder(@PathVariable Long id) {
        OrderDetailResponse response = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Order>> createOrder(@RequestBody Map<String, Object> request) {
        Order order = new Order();

        Object customerIdObj = request.get("customerId");
        if (customerIdObj != null) {
            order.setCustomerId(((Number) customerIdObj).longValue());
        }

        order.setCustomerName((String) request.get("customerName"));
        order.setContactName((String) request.get("contactName"));
        order.setPhone((String) request.get("phone"));
        order.setEmail((String) request.get("email"));
        order.setStatus((String) request.get("status"));
        order.setPayStatus((String) request.get("payStatus"));

        Object paymentMethodObj = request.get("paymentMethod");
        if (paymentMethodObj != null) {
            order.setPaymentMethod((String) paymentMethodObj);
        }

        Object discountAmountObj = request.get("discountAmount");
        if (discountAmountObj != null) {
            order.setDiscountAmount(new java.math.BigDecimal(discountAmountObj.toString()));
        }

        Object remarkObj = request.get("remark");
        if (remarkObj != null) {
            order.setRemark((String) remarkObj);
        }

        List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");
        List<OrderItem> orderItems = null;
        if (items != null && !items.isEmpty()) {
            orderItems = items.stream().map(item -> {
                OrderItem orderItem = new OrderItem();

                Object productIdObj = item.get("productId");
                if (productIdObj != null) {
                    orderItem.setProductId(((Number) productIdObj).longValue());
                }

                orderItem.setProductName((String) item.get("productName"));
                orderItem.setProductCode((String) item.get("productCode"));

                Object unitPriceObj = item.get("unitPrice");
                if (unitPriceObj != null) {
                    orderItem.setUnitPrice(new java.math.BigDecimal(unitPriceObj.toString()));
                }

                Object quantityObj = item.get("quantity");
                if (quantityObj != null) {
                    orderItem.setQuantity(((Number) quantityObj).intValue());
                }

                Object subtotalObj = item.get("subtotal");
                if (subtotalObj != null) {
                    orderItem.setSubtotal(new java.math.BigDecimal(subtotalObj.toString()));
                }

                return orderItem;
            }).toList();
        }

        Order created = orderService.createOrder(order, orderItems);
        return ResponseEntity.ok(ApiResponse.success("创建成功", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Order>> updateOrder(@PathVariable Long id, @RequestBody Order order) {
        Order updated = orderService.updateOrder(id, order);
        return ResponseEntity.ok(ApiResponse.success("更新成功", updated));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Order>> cancelOrder(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String reason = request.get("reason");
        Order cancelled = orderService.cancelOrder(id, reason);
        return ResponseEntity.ok(ApiResponse.success("取消成功", cancelled));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<ApiResponse<Order>> requestRefund(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String reason = request.get("reason");
        Order refunded = orderService.requestRefund(id, reason);
        return ResponseEntity.ok(ApiResponse.success("退款申请已提交", refunded));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    @DeleteMapping("/batch")
    public ResponseEntity<ApiResponse<Void>> batchDeleteOrders(@RequestBody Map<String, Long[]> request) {
        Long[] ids = request.get("ids");
        orderService.batchDeleteOrders(ids);
        return ResponseEntity.ok(ApiResponse.success("批量删除成功", null));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String payStatus) {

        String csv = orderService.exportOrders(keyword, status, payStatus);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        byte[] bom = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
        byte[] result = new byte[bom.length + bytes.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(bytes, 0, result, bom.length, bytes.length);

        return ResponseEntity.ok()
                .header("Content-Type", "text/csv;charset=UTF-8")
                .header("Content-Disposition", "attachment; filename=orders.csv")
                .body(result);
    }
}