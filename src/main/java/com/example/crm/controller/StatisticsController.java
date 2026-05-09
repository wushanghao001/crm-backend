
package com.example.crm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.crm.dto.ApiResponse;
import com.example.crm.entity.Customer;
import com.example.crm.entity.CustomerFollow;
import com.example.crm.entity.Order;
import com.example.crm.mapper.CustomerFollowMapper;
import com.example.crm.mapper.CustomerMapper;
import com.example.crm.mapper.OrderMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final CustomerMapper customerMapper;
    private final OrderMapper orderMapper;
    private final CustomerFollowMapper customerFollowMapper;

    public StatisticsController(CustomerMapper customerMapper, OrderMapper orderMapper, CustomerFollowMapper customerFollowMapper) {
        this.customerMapper = customerMapper;
        this.orderMapper = orderMapper;
        this.customerFollowMapper = customerFollowMapper;
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOverview() {
        Map<String, Object> overview = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        int currentMonth = now.getMonthValue();
        int lastMonth = currentMonth == 1 ? 12 : currentMonth - 1;
        int currentYear = now.getYear();
        int lastYear = currentMonth == 1 ? currentYear - 1 : currentYear;

        long totalCustomers = customerMapper.selectCount(null);
        long activeCustomers = customerMapper.selectList(null).stream()
                .filter(c -> "active".equals(c.getStatus()))
                .count();
        long churnedCustomers = customerMapper.selectList(null).stream()
                .filter(c -> "churned".equals(c.getStatus()))
                .count();

        long newCustomers = customerMapper.selectList(null).stream()
                .filter(c -> c.getCreatedAt() != null &&
                        c.getCreatedAt().getMonthValue() == currentMonth &&
                        c.getCreatedAt().getYear() == currentYear)
                .count();

        long lastMonthNewCustomers = customerMapper.selectList(null).stream()
                .filter(c -> c.getCreatedAt() != null &&
                        c.getCreatedAt().getMonthValue() == lastMonth &&
                        c.getCreatedAt().getYear() == lastYear)
                .count();

        double customerGrowthRate = lastMonthNewCustomers > 0
                ? ((double) (newCustomers - lastMonthNewCustomers) / lastMonthNewCustomers) * 100
                : (newCustomers > 0 ? 100 : 0);

        BigDecimal thisMonthSales = BigDecimal.ZERO;
        List<Order> allOrders = orderMapper.selectList(null);
        for (Order order : allOrders) {
            if ("paid".equals(order.getPayStatus()) &&
                    order.getUpdatedAt() != null &&
                    order.getUpdatedAt().getMonthValue() == currentMonth &&
                    order.getUpdatedAt().getYear() == currentYear) {
                if (order.getPaidAmount() != null) {
                    thisMonthSales = thisMonthSales.add(order.getPaidAmount());
                }
            }
        }

        BigDecimal lastMonthSales = BigDecimal.ZERO;
        for (Order order : allOrders) {
            if ("paid".equals(order.getPayStatus()) &&
                    order.getUpdatedAt() != null &&
                    order.getUpdatedAt().getMonthValue() == lastMonth &&
                    order.getUpdatedAt().getYear() == lastYear) {
                if (order.getPaidAmount() != null) {
                    lastMonthSales = lastMonthSales.add(order.getPaidAmount());
                }
            }
        }

        double salesGrowthRate = lastMonthSales.compareTo(BigDecimal.ZERO) > 0
                ? thisMonthSales.subtract(lastMonthSales).divide(lastMonthSales, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100
                : (thisMonthSales.compareTo(BigDecimal.ZERO) > 0 ? 100 : 0);

        double activeRate = totalCustomers > 0
                ? ((double) activeCustomers / totalCustomers) * 100
                : 0;

        double churnRate = totalCustomers > 0
                ? ((double) churnedCustomers / totalCustomers) * 100
                : 0;

        overview.put("totalCustomers", totalCustomers);
        overview.put("newCustomers", newCustomers);
        overview.put("customerGrowthRate", String.format("%.1f", customerGrowthRate));
        overview.put("thisMonthSales", thisMonthSales);
        overview.put("salesGrowthRate", String.format("%.1f", salesGrowthRate));
        overview.put("activeCustomers", activeCustomers);
        overview.put("activeRate", String.format("%.1f", activeRate));
        overview.put("churnRate", String.format("%.1f", churnRate));

        return ResponseEntity.ok(ApiResponse.success(overview));
    }

    @GetMapping("/customer-trend")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCustomerTrend() {
        List<Map<String, Object>> trend = new java.util.ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();

        for (int month = 1; month <= currentMonth; month++) {
            final int m = month;
            BigDecimal monthSales = BigDecimal.ZERO;
            List<Order> monthOrders = orderMapper.selectList(null);
            for (Order order : monthOrders) {
                if ("paid".equals(order.getPayStatus()) &&
                        order.getUpdatedAt() != null &&
                        order.getUpdatedAt().getMonthValue() == m &&
                        order.getUpdatedAt().getYear() == currentYear) {
                    if (order.getPaidAmount() != null) {
                        monthSales = monthSales.add(order.getPaidAmount());
                    }
                }
            }
            Map<String, Object> item = new HashMap<>();
            item.put("month", m + "月");
            item.put("sales", monthSales);
            trend.add(item);
        }

        return ResponseEntity.ok(ApiResponse.success(trend));
    }

    @GetMapping("/sales-top")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSalesTop() {
        List<Map<String, Object>> salesTop = List.of(
            Map.of("name", "北京科技有限公司", "product", "企业版套餐", "amount", "¥120,000"),
            Map.of("name", "上海数据科技", "product", "高级版套餐", "amount", "¥85,000"),
            Map.of("name", "深圳创新集团", "product", "标准版套餐", "amount", "¥68,000"),
            Map.of("name", "广州智能制造", "product", "企业版套餐", "amount", "¥110,000"),
            Map.of("name", "杭州电商科技", "product", "专业版套餐", "amount", "¥45,000")
        );
        return ResponseEntity.ok(ApiResponse.success(salesTop));
    }

    @GetMapping("/funnel")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFunnelData() {
        LambdaQueryWrapper<CustomerFollow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(CustomerFollow::getCreatedAt);
        List<CustomerFollow> follows = customerFollowMapper.selectList(queryWrapper);

        int leads = follows.size();

        long contacted = follows.stream()
                .filter(f -> f.getFollowResult() != null &&
                        (f.getFollowResult().equals("initial_contact") ||
                                f.getFollowResult().equals("requirement") ||
                                f.getFollowResult().equals("quotation") ||
                                f.getFollowResult().equals("negotiation") ||
                                f.getFollowResult().equals("pending_deal")))
                .count();

        long quoted = follows.stream()
                .filter(f -> f.getFollowResult() != null &&
                        (f.getFollowResult().equals("quotation") ||
                                f.getFollowResult().equals("negotiation") ||
                                f.getFollowResult().equals("pending_deal")))
                .count();

        long won = follows.stream()
                .filter(f -> f.getFollowResult() != null && f.getFollowResult().equals("closed"))
                .count();

        long lost = follows.stream()
                .filter(f -> f.getFollowResult() != null &&
                        (f.getFollowResult().equals("lost") || f.getFollowResult().equals("contact_lost")))
                .count();

        Map<String, Object> funnel = new HashMap<>();
        funnel.put("leads", leads);
        funnel.put("contacted", contacted);
        funnel.put("quoted", quoted);
        funnel.put("won", won);
        funnel.put("lost", lost);

        return ResponseEntity.ok(ApiResponse.success(funnel));
    }

    @GetMapping("/industry-distribution")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getIndustryDistribution() {
        List<Customer> customers = customerMapper.selectList(null);
        Map<String, Long> industryCount = new HashMap<>();
        for (Customer customer : customers) {
            String industry = customer.getIndustry() != null ? customer.getIndustry() : "其他";
            industryCount.put(industry, industryCount.getOrDefault(industry, 0L) + 1);
        }
        List<Map<String, Object>> distribution = industryCount.entrySet().stream()
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", e.getKey());
                    map.put("value", e.getValue());
                    return map;
                })
                .toList();
        return ResponseEntity.ok(ApiResponse.success(distribution));
    }

    @GetMapping("/churn-reason")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getChurnReasonDistribution() {
        LambdaQueryWrapper<Customer> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Customer::getStatus, "churned");
        List<Customer> churnedCustomers = customerMapper.selectList(queryWrapper);

        Map<String, Long> churnReasonCount = new HashMap<>();
        for (Customer customer : churnedCustomers) {
            String reason = customer.getChurnReason() != null ? customer.getChurnReason() : "未知";
            churnReasonCount.put(reason, churnReasonCount.getOrDefault(reason, 0L) + 1);
        }

        List<Map<String, Object>> distribution = churnReasonCount.entrySet().stream()
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", e.getKey());
                    map.put("value", e.getValue());
                    return map;
                })
                .toList();

        return ResponseEntity.ok(ApiResponse.success(distribution));
    }
}
