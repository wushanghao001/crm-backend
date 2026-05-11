package com.example.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.crm.dto.OrderDetailResponse;
import com.example.crm.dto.PageResponse;
import com.example.crm.entity.Customer;
import com.example.crm.entity.Order;
import com.example.crm.entity.OrderItem;
import com.example.crm.entity.User;
import com.example.crm.mapper.CustomerMapper;
import com.example.crm.mapper.OrderItemMapper;
import com.example.crm.mapper.OrderMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CustomerMapper customerMapper;
    private final StockLogService stockLogService;

    public OrderService(OrderMapper orderMapper, OrderItemMapper orderItemMapper, CustomerMapper customerMapper, StockLogService stockLogService) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.customerMapper = customerMapper;
        this.stockLogService = stockLogService;
    }

    public PageResponse<Order> listOrders(Integer pageNum, Integer pageSize, String keyword, String status, String payStatus) {
        Page<Order> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();

        User currentUser = getCurrentUser();
        boolean isAdmin = "admin".equals(currentUser.getRole());

        if (!isAdmin) {
            queryWrapper.eq(Order::getCreatorId, currentUser.getId());
        }

        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.and(wrapper -> wrapper
                .like(Order::getOrderNo, keyword)
                .or()
                .like(Order::getCustomerName, keyword));
        }
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq(Order::getStatus, status);
        }
        if (payStatus != null && !payStatus.isEmpty()) {
            queryWrapper.eq(Order::getPayStatus, payStatus);
        }

        queryWrapper.orderByDesc(Order::getCreatedAt);

        IPage<Order> result = orderMapper.selectPage(page, queryWrapper);

        return new PageResponse<>(result.getRecords(), result.getTotal(), pageNum, pageSize);
    }

    public OrderDetailResponse getOrderById(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }

        User currentUser = getCurrentUser();
        if (!"admin".equals(currentUser.getRole())) {
            Customer customer = customerMapper.selectById(order.getCustomerId());
            if (customer == null || !customer.getCreatorId().equals(currentUser.getId())) {
                throw new IllegalArgumentException("无权访问此订单");
            }
        }

        List<OrderItem> items = orderItemMapper.findByOrderId(id);

        OrderDetailResponse response = new OrderDetailResponse();
        response.setId(order.getId());
        response.setOrderNo(order.getOrderNo());
        response.setCustomerId(order.getCustomerId());
        response.setCustomerName(order.getCustomerName());
        response.setContactName(order.getContactName());
        response.setPhone(order.getPhone());
        response.setEmail(order.getEmail());
        response.setTotalAmount(order.getTotalAmount());
        response.setDiscountAmount(order.getDiscountAmount());
        response.setPaidAmount(order.getPaidAmount());
        response.setStatus(order.getStatus());
        response.setPayStatus(order.getPayStatus());
        response.setPaymentMethod(order.getPaymentMethod());
        response.setPaidAt(order.getPaidAt());
        response.setTransactionNo(order.getTransactionNo());
        response.setRemark(order.getRemark());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

        response.setItems(items.stream().map(item -> {
            OrderDetailResponse.OrderItemDTO dto = new OrderDetailResponse.OrderItemDTO();
            dto.setId(item.getId());
            dto.setOrderId(item.getOrderId());
            dto.setProductName(item.getProductName());
            dto.setProductCode(item.getProductCode());
            dto.setProjectCodeId(item.getProjectCodeId());
            dto.setProjectCodeName(item.getProjectCodeName());
            dto.setMaterialCodeId(item.getMaterialCodeId());
            dto.setMaterialCodeName(item.getMaterialCodeName());
            dto.setBrandCodeId(item.getBrandCodeId());
            dto.setBrandCodeName(item.getBrandCodeName());
            dto.setUnitPrice(item.getUnitPrice());
            dto.setQuantity(item.getQuantity());
            dto.setSubtotal(item.getSubtotal());
            return dto;
        }).collect(Collectors.toList()));

        return response;
    }

    @Transactional
    public Order createOrder(Order order, List<OrderItem> items) {
        User currentUser = getCurrentUser();
        boolean isAdmin = "admin".equals(currentUser.getRole());

        Customer customer = customerMapper.selectById(order.getCustomerId());
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在");
        }

        if (!isAdmin && !customer.getCreatorId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("只能为自己负责的客户创建订单");
        }

        if (items != null && !items.isEmpty()) {
            for (OrderItem item : items) {
                if (!stockLogService.checkStockAvailable(item.getProductId(), item.getQuantity())) {
                    throw new IllegalArgumentException("产品【" + item.getProductName() + "】库存不足，无法下单");
                }
            }
        }

        order.setOrderNo(generateOrderNo());
        order.setCreatorId(currentUser.getId());
        if (order.getStatus() == null || order.getStatus().isEmpty()) {
            order.setStatus("pending");
        }
        if (order.getPayStatus() == null || order.getPayStatus().isEmpty()) {
            order.setPayStatus("unpaid");
        }
        if ("paid".equals(order.getPayStatus())) {
            order.setPaidAt(LocalDateTime.now());
        }
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        if (items != null && !items.isEmpty()) {
            java.math.BigDecimal totalAmount = java.math.BigDecimal.ZERO;
            for (OrderItem item : items) {
                item.setCreatedAt(LocalDateTime.now());
                if (item.getUnitPrice() != null && item.getQuantity() != null) {
                    item.setSubtotal(item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())));
                    totalAmount = totalAmount.add(item.getSubtotal());
                }
            }
            if (order.getDiscountAmount() != null) {
                totalAmount = totalAmount.subtract(order.getDiscountAmount());
            }
            order.setTotalAmount(totalAmount);
            order.setPaidAmount(totalAmount);
        }

        orderMapper.insert(order);

        if (items != null && !items.isEmpty()) {
            for (OrderItem item : items) {
                item.setOrderId(order.getId());
                orderItemMapper.insert(item);
                if ("paid".equals(order.getPayStatus())) {
                    stockLogService.deductStock(item.getProductId(), order.getId(), item.getQuantity(), "订单创建时直接扣减：" + order.getOrderNo());
                } else {
                    stockLogService.lockStock(item.getProductId(), order.getId(), item.getQuantity(), "订单预占：" + order.getOrderNo());
                }
            }
        }

        return order;
    }

    @Transactional
    public Order updateOrder(Long id, Order order) {
        Order existing = orderMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("订单不存在");
        }

        User currentUser = getCurrentUser();
        boolean isAdmin = "admin".equals(currentUser.getRole());

        if (!isAdmin) {
            Customer customer = customerMapper.selectById(existing.getCustomerId());
            if (customer == null || !customer.getCreatorId().equals(currentUser.getId())) {
                throw new IllegalArgumentException("无权修改此订单");
            }
            if ("paid".equals(existing.getPayStatus()) || "completed".equals(existing.getStatus())) {
                if ("refunded".equals(order.getPayStatus())) {
                    throw new IllegalArgumentException("已支付订单退款需联系管理员处理");
                }
                throw new IllegalArgumentException("已支付或已完成的订单不可修改");
            }
        }

        existing.setContactName(order.getContactName());
        existing.setPhone(order.getPhone());
        existing.setEmail(order.getEmail());
        existing.setTotalAmount(order.getTotalAmount());
        existing.setDiscountAmount(order.getDiscountAmount());
        existing.setPaidAmount(order.getPaidAmount());
        existing.setStatus(order.getStatus());

        String oldPayStatus = existing.getPayStatus();
        existing.setPayStatus(order.getPayStatus());

        if ("paid".equals(order.getPayStatus()) && !"paid".equals(oldPayStatus)) {
            existing.setPaidAt(LocalDateTime.now());
            List<OrderItem> orderItems = orderItemMapper.findByOrderId(id);
            for (OrderItem item : orderItems) {
                stockLogService.deductStock(item.getProductId(), id, item.getQuantity(), "订单支付确认扣减：" + existing.getOrderNo());
            }
        }

        if (isAdmin && "refunded".equals(order.getPayStatus()) && "paid".equals(oldPayStatus)) {
            existing.setPayStatus("refunded");
            List<OrderItem> orderItems = orderItemMapper.findByOrderId(id);
            for (OrderItem item : orderItems) {
                stockLogService.refundStock(item.getProductId(), id, item.getQuantity(), "管理员退款：" + existing.getOrderNo());
            }
        }

        existing.setPaymentMethod(order.getPaymentMethod());
        existing.setRemark(order.getRemark());
        existing.setUpdatedAt(LocalDateTime.now());

        orderMapper.updateById(existing);

        if (isAdmin && "refunded".equals(order.getPayStatus()) && "paid".equals(oldPayStatus)) {
            LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Order::getId, id);
            updateWrapper.set(Order::getPaidAt, null);
            orderMapper.update(null, updateWrapper);
        }

        return existing;
    }

    @Transactional
    public Order cancelOrder(Long id, String reason) {
        Order existing = orderMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("订单不存在");
        }

        User currentUser = getCurrentUser();
        boolean isAdmin = "admin".equals(currentUser.getRole());

        if (!isAdmin) {
            Customer customer = customerMapper.selectById(existing.getCustomerId());
            if (customer == null || !customer.getCreatorId().equals(currentUser.getId())) {
                throw new IllegalArgumentException("无权取消此订单");
            }
        }

        if (!"unpaid".equals(existing.getPayStatus()) && !"pending".equals(existing.getStatus())) {
            if ("paid".equals(existing.getPayStatus())) {
                throw new IllegalArgumentException("已支付订单无法直接取消，请申请退款");
            }
        }

        List<OrderItem> orderItems = orderItemMapper.findByOrderId(id);
        for (OrderItem item : orderItems) {
            stockLogService.releaseStock(item.getProductId(), id, item.getQuantity(), "订单取消释放：" + existing.getOrderNo());
        }

        existing.setStatus("cancelled");
        existing.setRemark((existing.getRemark() != null ? existing.getRemark() : "") + "\n取消原因: " + reason);
        existing.setUpdatedAt(LocalDateTime.now());

        orderMapper.updateById(existing);

        return existing;
    }

    @Transactional
    public Order requestRefund(Long id, String reason) {
        Order existing = orderMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("订单不存在");
        }

        User currentUser = getCurrentUser();
        boolean isAdmin = "admin".equals(currentUser.getRole());

        if (!isAdmin) {
            Customer customer = customerMapper.selectById(existing.getCustomerId());
            if (customer == null || !customer.getCreatorId().equals(currentUser.getId())) {
                throw new IllegalArgumentException("无权申请退款");
            }
        }

        if (!"paid".equals(existing.getPayStatus())) {
            throw new IllegalArgumentException("只能对已支付订单申请退款");
        }

        List<OrderItem> orderItems = orderItemMapper.findByOrderId(id);
        for (OrderItem item : orderItems) {
            stockLogService.refundStock(item.getProductId(), id, item.getQuantity(), "退款完成库存回滚：" + existing.getOrderNo());
        }

        existing.setPayStatus("refunded");
        existing.setRemark((existing.getRemark() != null ? existing.getRemark() : "") + "\n退款原因: " + reason);
        existing.setUpdatedAt(LocalDateTime.now());

        orderMapper.updateById(existing);

        LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Order::getId, id);
        updateWrapper.set(Order::getPaidAt, null);
        orderMapper.update(null, updateWrapper);

        return existing;
    }

    @Transactional
    public void deleteOrder(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }

        User currentUser = getCurrentUser();
        boolean isAdmin = "admin".equals(currentUser.getRole());

        if (!isAdmin) {
            if (!currentUser.getId().equals(order.getCreatorId())) {
                throw new IllegalArgumentException("只能删除自己创建的订单");
            }
            if ("paid".equals(order.getPayStatus()) || "completed".equals(order.getStatus())) {
                throw new IllegalArgumentException("已支付或已完成的订单无法删除");
            }
        }

        if (!"paid".equals(order.getPayStatus()) && !"completed".equals(order.getStatus())) {
            List<OrderItem> orderItems = orderItemMapper.findByOrderId(id);
            for (OrderItem item : orderItems) {
                stockLogService.releaseStock(item.getProductId(), id, item.getQuantity(), "订单删除释放：" + order.getOrderNo());
            }
        }

        orderMapper.deleteById(id);
    }

    @Transactional
    public void batchDeleteOrders(Long[] ids) {
        User currentUser = getCurrentUser();
        boolean isAdmin = "admin".equals(currentUser.getRole());

        if (!isAdmin) {
            for (Long id : ids) {
                Order order = orderMapper.selectById(id);
                if (order == null) {
                    throw new IllegalArgumentException("订单不存在，ID: " + id);
                }
                if (!order.getCreatorId().equals(currentUser.getId())) {
                    throw new IllegalArgumentException("只能删除自己创建的订单");
                }
                if ("paid".equals(order.getPayStatus()) || "completed".equals(order.getStatus())) {
                    throw new IllegalArgumentException("已支付或已完成的订单无法删除");
                }
            }
        }

        for (Long id : ids) {
            Order order = orderMapper.selectById(id);
            if (order != null && !"paid".equals(order.getPayStatus()) && !"completed".equals(order.getStatus())) {
                List<OrderItem> orderItems = orderItemMapper.findByOrderId(id);
                for (OrderItem item : orderItems) {
                    stockLogService.releaseStock(item.getProductId(), id, item.getQuantity(), "订单删除释放：" + order.getOrderNo());
                }
            }
            orderMapper.deleteById(id);
        }
    }

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%03d", (int) (Math.random() * 1000));
        return "ORD" + timestamp + random;
    }

    public String exportOrders(String keyword, String status, String payStatus) {
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();

        User currentUser = getCurrentUser();
        boolean isAdmin = "admin".equals(currentUser.getRole());

        if (!isAdmin) {
            queryWrapper.eq(Order::getCreatorId, currentUser.getId());
        }

        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.and(wrapper -> wrapper
                .like(Order::getOrderNo, keyword)
                .or()
                .like(Order::getCustomerName, keyword));
        }
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq(Order::getStatus, status);
        }
        if (payStatus != null && !payStatus.isEmpty()) {
            queryWrapper.eq(Order::getPayStatus, payStatus);
        }

        queryWrapper.orderByDesc(Order::getCreatedAt);

        List<Order> orders = orderMapper.selectList(queryWrapper);

        StringBuilder csv = new StringBuilder();
        csv.append("订单编号,客户名称,联系人,订单金额,订单状态,支付状态,下单时间\n");

        for (Order order : orders) {
            csv.append(order.getOrderNo()).append(",");
            csv.append(order.getCustomerName()).append(",");
            csv.append(order.getContactName() != null ? order.getContactName() : "").append(",");
            csv.append(order.getTotalAmount()).append(",");
            csv.append(getStatusText(order.getStatus())).append(",");
            csv.append(getPayStatusText(order.getPayStatus())).append(",");
            csv.append(order.getCreatedAt() != null ? order.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "").append("\n");
        }

        return csv.toString();
    }

    public Long countMyOrders() {
        User currentUser = getCurrentUser();
        List<Long> customerIds = customerMapper.selectList(
            new LambdaQueryWrapper<Customer>()
                .eq(Customer::getCreatorId, currentUser.getId())
        ).stream().map(Customer::getId).collect(Collectors.toList());

        if (customerIds.isEmpty()) {
            return 0L;
        }

        return orderMapper.selectCount(
            new LambdaQueryWrapper<Order>()
                .in(Order::getCustomerId, customerIds)
        );
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        throw new IllegalArgumentException("用户未登录");
    }

    private String getStatusText(String status) {
        return switch (status) {
            case "pending" -> "待付款";
            case "paid" -> "已付款";
            case "completed" -> "已完成";
            case "cancelled" -> "已取消";
            default -> status;
        };
    }

    private String getPayStatusText(String payStatus) {
        return switch (payStatus) {
            case "unpaid" -> "未支付";
            case "paid" -> "已支付";
            case "refunding" -> "退款中";
            case "refunded" -> "已退款";
            default -> payStatus;
        };
    }
}