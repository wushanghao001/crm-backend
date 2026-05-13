package com.example.crm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.crm.config.CacheConfig;
import com.example.crm.dto.ApiResponse;
import com.example.crm.entity.*;
import com.example.crm.mapper.*;
import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final UserMapper userMapper;
    private final CustomerMapper customerMapper;
    private final OpportunityMapper opportunityMapper;
    private final OrderMapper orderMapper;
    private final OperationLogMapper operationLogMapper;
    private final CustomerFollowMapper customerFollowMapper;
    private final Cache<String, Object> dashboardCache;

    public DashboardController(UserMapper userMapper, CustomerMapper customerMapper,
                               OpportunityMapper opportunityMapper, OrderMapper orderMapper,
                               OperationLogMapper operationLogMapper, CustomerFollowMapper customerFollowMapper,
                               Cache<String, Object> dashboardCache) {
        this.userMapper = userMapper;
        this.customerMapper = customerMapper;
        this.opportunityMapper = opportunityMapper;
        this.orderMapper = orderMapper;
        this.operationLogMapper = operationLogMapper;
        this.customerFollowMapper = customerFollowMapper;
        this.dashboardCache = dashboardCache;
    }

    @GetMapping("/refresh")
    public ResponseEntity<ApiResponse<String>> refreshCache() {
        dashboardCache.invalidateAll();
        return ResponseEntity.ok(ApiResponse.success("刷新成功"));
    }

    @GetMapping("/data")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardData(
            @RequestParam(defaultValue = "month") String timeRange) {
        String cacheKey = CacheConfig.DASHBOARD_CACHE + "_data_" + timeRange;

        @SuppressWarnings("unchecked")
        Map<String, Object> cached = (Map<String, Object>) dashboardCache.getIfPresent(cacheKey);
        if (cached != null) {
            System.out.println("[DashboardController] Cache HIT for key: " + cacheKey);
            return ResponseEntity.ok(ApiResponse.success(cached));
        }

        System.out.println("[DashboardController] Cache MISS for key: " + cacheKey + ", loading from database");

        Map<String, Object> result = new HashMap<>();

        // 1. 概览数据
        result.put("overview", loadOverview(timeRange));

        // 2. 漏斗数据
        result.put("funnel", loadFunnelData());

        // 3. 趋势数据
        result.put("trend", loadTrendData(timeRange));

        // 4. 行业分布
        result.put("industryDistribution", loadIndustryDistribution());

        // 5. 最新日志
        result.put("recentLogs", loadRecentLogs());

        // 6. 待办事项
        result.put("todos", loadTodos());

        dashboardCache.put(cacheKey, result);
        System.out.println("[DashboardController] Cache PUT for key: " + cacheKey);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private Map<String, Object> loadOverview(String timeRange) {
        LocalDateTime[] dateRange = getDateRange(timeRange);
        Map<String, Object> overview = new HashMap<>();

        long totalUsers = userMapper.selectCount(null);
        long monthNewUsers = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .ge(User::getCreatedAt, dateRange[0])
                        .le(User::getCreatedAt, dateRange[1])
        );
        overview.put("totalUsers", totalUsers);
        overview.put("monthNewUsers", monthNewUsers);

        long totalCustomers = customerMapper.selectCount(null);
        long monthNewCustomers = customerMapper.selectCount(
                new LambdaQueryWrapper<Customer>()
                        .ge(Customer::getCreatedAt, dateRange[0])
                        .le(Customer::getCreatedAt, dateRange[1])
        );
        overview.put("totalCustomers", totalCustomers);
        overview.put("monthNewCustomers", monthNewCustomers);

        long totalOpportunities = opportunityMapper.selectCount(null);
        long monthNewOpportunities = opportunityMapper.selectCount(
                new LambdaQueryWrapper<Opportunity>()
                        .ge(Opportunity::getCreatedAt, dateRange[0])
                        .le(Opportunity::getCreatedAt, dateRange[1])
        );
        overview.put("totalOpportunities", totalOpportunities);
        overview.put("monthNewOpportunities", monthNewOpportunities);

        long totalOrders = orderMapper.selectCount(
                new LambdaQueryWrapper<Order>().eq(Order::getPayStatus, "paid")
        );
        long monthOrders = orderMapper.selectCount(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getPayStatus, "paid")
                        .ge(Order::getUpdatedAt, dateRange[0])
                        .le(Order::getUpdatedAt, dateRange[1])
        );
        overview.put("totalOrders", totalOrders);
        overview.put("monthOrders", monthOrders);

        return overview;
    }

    private Map<String, Object> loadFunnelData() {
        List<CustomerFollow> follows = customerFollowMapper.selectList(
                new LambdaQueryWrapper<CustomerFollow>().orderByDesc(CustomerFollow::getCreatedAt)
        );

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

        Map<String, Object> funnel = new LinkedHashMap<>();
        funnel.put("leads", leads);
        funnel.put("contacted", contacted);
        funnel.put("quoted", quoted);
        funnel.put("won", won);
        funnel.put("lost", lost);

        return funnel;
    }

    private List<Map<String, Object>> loadTrendData(String timeRange) {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate startDate;

        if ("today".equals(timeRange)) {
            startDate = today;
        } else if ("week".equals(timeRange)) {
            startDate = today.minusDays(6);
        } else if ("month".equals(timeRange)) {
            startDate = today.withDayOfMonth(1);
        } else {
            startDate = today.minusDays(89);
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = today.atTime(LocalTime.MAX);

        List<Customer> customersInRange = customerMapper.selectList(
                new LambdaQueryWrapper<Customer>()
                        .ge(Customer::getCreatedAt, startDateTime)
                        .le(Customer::getCreatedAt, endDateTime)
        );

        List<Opportunity> opportunitiesInRange = opportunityMapper.selectList(
                new LambdaQueryWrapper<Opportunity>()
                        .ge(Opportunity::getCreatedAt, startDateTime)
                        .le(Opportunity::getCreatedAt, endDateTime)
        );

        List<Order> ordersInRange = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getPayStatus, "paid")
                        .ge(Order::getUpdatedAt, startDateTime)
                        .le(Order::getUpdatedAt, endDateTime)
        );

        Map<LocalDate, Long> customersByDate = customersInRange.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getCreatedAt().toLocalDate(),
                        Collectors.counting()
                ));

        Map<LocalDate, Long> opportunitiesByDate = opportunitiesInRange.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getCreatedAt().toLocalDate(),
                        Collectors.counting()
                ));

        Map<LocalDate, Long> ordersByDate = ordersInRange.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getUpdatedAt().toLocalDate(),
                        Collectors.counting()
                ));

        LocalDate current = startDate;
        while (!current.isAfter(today)) {
            Map<String, Object> dayData = new LinkedHashMap<>();
            dayData.put("date", current.toString());
            dayData.put("customers", customersByDate.getOrDefault(current, 0L));
            dayData.put("opportunities", opportunitiesByDate.getOrDefault(current, 0L));
            dayData.put("orders", ordersByDate.getOrDefault(current, 0L));
            trend.add(dayData);
            current = current.plusDays(1);
        }

        return trend;
    }

    private List<Map<String, Object>> loadIndustryDistribution() {
        List<Customer> customers = customerMapper.selectList(null);
        Map<String, Long> industryCount = new HashMap<>();
        for (Customer customer : customers) {
            String industry = customer.getIndustry() != null ? customer.getIndustry() : "其他";
            industryCount.put(industry, industryCount.getOrDefault(industry, 0L) + 1);
        }
        return industryCount.entrySet().stream()
                .map(e -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("name", e.getKey());
                    map.put("value", e.getValue());
                    return map;
                })
                .toList();
    }

    private List<Map<String, Object>> loadRecentLogs() {
        List<OperationLog> logs = operationLogMapper.selectList(
                new LambdaQueryWrapper<OperationLog>()
                        .orderByDesc(OperationLog::getCreatedAt)
                        .last("LIMIT 10")
        );

        return logs.stream()
                .map(log -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", log.getId());
                    map.put("operator", log.getOperator());
                    map.put("type", log.getType());
                    map.put("content", log.getContent());
                    map.put("createdAt", log.getCreatedAt());
                    return map;
                })
                .toList();
    }

    private Map<String, Object> loadTodos() {
        List<Map<String, Object>> pendingFollows = new ArrayList<>();
        List<CustomerFollow> follows = customerFollowMapper.selectList(
                new LambdaQueryWrapper<CustomerFollow>()
                        .eq(CustomerFollow::getFollowResult, "pending_deal")
                        .orderByDesc(CustomerFollow::getCreatedAt)
                        .last("LIMIT 10")
        );
        for (CustomerFollow follow : follows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", follow.getId());
            item.put("customerId", follow.getCustomerId());
            Customer customer = customerMapper.selectById(follow.getCustomerId());
            if (customer != null) {
                item.put("customerName", customer.getName());
            } else {
                item.put("customerName", "未知客户");
            }
            item.put("content", follow.getContent());
            item.put("followResult", getFollowResultText(follow.getFollowResult()));
            item.put("createdAt", follow.getCreatedAt());
            item.put("type", "pending_follow");
            pendingFollows.add(item);
        }

        List<Map<String, Object>> pendingCustomers = new ArrayList<>();
        List<Customer> customers = customerMapper.selectList(
                new LambdaQueryWrapper<Customer>()
                        .eq(Customer::getStatus, "warning")
                        .orderByDesc(Customer::getUpdatedAt)
                        .last("LIMIT 10")
        );
        for (Customer customer : customers) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", customer.getId());
            item.put("name", customer.getName());
            item.put("status", customer.getStatus());
            item.put("lastFollowTime", customer.getLastFollowTime());
            item.put("type", "pending_customer");
            pendingCustomers.add(item);
        }

        Map<String, Object> todos = new LinkedHashMap<>();
        todos.put("pendingFollows", pendingFollows);
        todos.put("pendingCustomers", pendingCustomers);

        return todos;
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOverview(
            @RequestParam(defaultValue = "today") String timeRange) {
        String cacheKey = CacheConfig.DASHBOARD_CACHE + "_overview_" + timeRange;
        @SuppressWarnings("unchecked")
        Map<String, Object> cached = (Map<String, Object>) dashboardCache.getIfPresent(cacheKey);
        if (cached != null) {
            return ResponseEntity.ok(ApiResponse.success(cached));
        }

        LocalDateTime[] dateRange = getDateRange(timeRange);

        Map<String, Object> overview = new HashMap<>();

        long totalUsers = userMapper.selectCount(null);
        long todayNewUsers = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .ge(User::getCreatedAt, dateRange[0])
                        .le(User::getCreatedAt, dateRange[1])
        );
        overview.put("totalUsers", totalUsers);
        overview.put("todayNewUsers", todayNewUsers);

        long totalCustomers = customerMapper.selectCount(null);
        long todayNewCustomers = customerMapper.selectCount(
                new LambdaQueryWrapper<Customer>()
                        .ge(Customer::getCreatedAt, dateRange[0])
                        .le(Customer::getCreatedAt, dateRange[1])
        );
        overview.put("totalCustomers", totalCustomers);
        overview.put("todayNewCustomers", todayNewCustomers);

        long totalOpportunities = opportunityMapper.selectCount(null);
        long todayNewOpportunities = opportunityMapper.selectCount(
                new LambdaQueryWrapper<Opportunity>()
                        .ge(Opportunity::getCreatedAt, dateRange[0])
                        .le(Opportunity::getCreatedAt, dateRange[1])
        );
        overview.put("totalOpportunities", totalOpportunities);
        overview.put("todayNewOpportunities", todayNewOpportunities);

        long totalOrders = orderMapper.selectCount(
                new LambdaQueryWrapper<Order>().eq(Order::getPayStatus, "paid")
        );
        long monthOrders = orderMapper.selectCount(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getPayStatus, "paid")
                        .ge(Order::getUpdatedAt, dateRange[0])
                        .le(Order::getUpdatedAt, dateRange[1])
        );
        overview.put("totalOrders", totalOrders);
        overview.put("monthOrders", monthOrders);

        dashboardCache.put(cacheKey, overview);
        return ResponseEntity.ok(ApiResponse.success(overview));
    }

    @GetMapping("/funnel")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFunnel() {
        String cacheKey = CacheConfig.DASHBOARD_CACHE + "_funnel";
        @SuppressWarnings("unchecked")
        Map<String, Object> cached = (Map<String, Object>) dashboardCache.getIfPresent(cacheKey);
        if (cached != null) {
            return ResponseEntity.ok(ApiResponse.success(cached));
        }

        List<CustomerFollow> follows = customerFollowMapper.selectList(
                new LambdaQueryWrapper<CustomerFollow>().orderByDesc(CustomerFollow::getCreatedAt)
        );

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

        Map<String, Object> funnel = new LinkedHashMap<>();
        funnel.put("leads", leads);
        funnel.put("contacted", contacted);
        funnel.put("quoted", quoted);
        funnel.put("won", won);
        funnel.put("lost", lost);

        dashboardCache.put(cacheKey, funnel);
        return ResponseEntity.ok(ApiResponse.success(funnel));
    }

    @GetMapping("/trend")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTrend(
            @RequestParam(defaultValue = "month") String timeRange) {
        String cacheKey = CacheConfig.DASHBOARD_CACHE + "_trend_" + timeRange;
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cached = (List<Map<String, Object>>) dashboardCache.getIfPresent(cacheKey);
        if (cached != null) {
            return ResponseEntity.ok(ApiResponse.success(cached));
        }

        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate startDate;

        if ("today".equals(timeRange)) {
            startDate = today;
        } else if ("week".equals(timeRange)) {
            startDate = today.minusDays(6);
        } else if ("month".equals(timeRange)) {
            startDate = today.withDayOfMonth(1);
        } else {
            startDate = today.minusDays(89);
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = today.atTime(LocalTime.MAX);

        List<Customer> customersInRange = customerMapper.selectList(
                new LambdaQueryWrapper<Customer>()
                        .ge(Customer::getCreatedAt, startDateTime)
                        .le(Customer::getCreatedAt, endDateTime)
        );

        List<Opportunity> opportunitiesInRange = opportunityMapper.selectList(
                new LambdaQueryWrapper<Opportunity>()
                        .ge(Opportunity::getCreatedAt, startDateTime)
                        .le(Opportunity::getCreatedAt, endDateTime)
        );

        List<Order> ordersInRange = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getPayStatus, "paid")
                        .ge(Order::getUpdatedAt, startDateTime)
                        .le(Order::getUpdatedAt, endDateTime)
        );

        Map<LocalDate, Long> customersByDate = customersInRange.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getCreatedAt().toLocalDate(),
                        Collectors.counting()
                ));

        Map<LocalDate, Long> opportunitiesByDate = opportunitiesInRange.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getCreatedAt().toLocalDate(),
                        Collectors.counting()
                ));

        Map<LocalDate, Long> ordersByDate = ordersInRange.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getUpdatedAt().toLocalDate(),
                        Collectors.counting()
                ));

        LocalDate current = startDate;
        while (!current.isAfter(today)) {
            Map<String, Object> dayData = new LinkedHashMap<>();
            dayData.put("date", current.toString());
            dayData.put("customers", customersByDate.getOrDefault(current, 0L));
            dayData.put("opportunities", opportunitiesByDate.getOrDefault(current, 0L));
            dayData.put("orders", ordersByDate.getOrDefault(current, 0L));
            trend.add(dayData);
            current = current.plusDays(1);
        }

        dashboardCache.put(cacheKey, trend);
        return ResponseEntity.ok(ApiResponse.success(trend));
    }

    @GetMapping("/industry-distribution")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getIndustryDistribution() {
        String cacheKey = CacheConfig.DASHBOARD_CACHE + "_industry_distribution";
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cached = (List<Map<String, Object>>) dashboardCache.getIfPresent(cacheKey);
        if (cached != null) {
            return ResponseEntity.ok(ApiResponse.success(cached));
        }

        List<Customer> customers = customerMapper.selectList(null);
        Map<String, Long> industryCount = new HashMap<>();
        for (Customer customer : customers) {
            String industry = customer.getIndustry() != null ? customer.getIndustry() : "其他";
            industryCount.put(industry, industryCount.getOrDefault(industry, 0L) + 1);
        }
        List<Map<String, Object>> distribution = industryCount.entrySet().stream()
                .map(e -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("name", e.getKey());
                    map.put("value", e.getValue());
                    return map;
                })
                .toList();

        dashboardCache.put(cacheKey, distribution);
        return ResponseEntity.ok(ApiResponse.success(distribution));
    }

    @GetMapping("/recent-logs")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRecentLogs() {
        String cacheKey = CacheConfig.DASHBOARD_CACHE + "_recent_logs";
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cached = (List<Map<String, Object>>) dashboardCache.getIfPresent(cacheKey);
        if (cached != null) {
            return ResponseEntity.ok(ApiResponse.success(cached));
        }

        List<OperationLog> logs = operationLogMapper.selectList(
                new LambdaQueryWrapper<OperationLog>()
                        .orderByDesc(OperationLog::getCreatedAt)
                        .last("LIMIT 10")
        );

        List<Map<String, Object>> result = logs.stream()
                .map(log -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", log.getId());
                    map.put("operator", log.getOperator());
                    map.put("type", log.getType());
                    map.put("content", log.getContent());
                    map.put("createdAt", log.getCreatedAt());
                    return map;
                })
                .toList();

        dashboardCache.put(cacheKey, result);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/todos")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTodos() {
        List<Map<String, Object>> pendingFollows = new ArrayList<>();
        List<CustomerFollow> follows = customerFollowMapper.selectList(
                new LambdaQueryWrapper<CustomerFollow>()
                        .eq(CustomerFollow::getFollowResult, "pending_deal")
                        .orderByDesc(CustomerFollow::getCreatedAt)
                        .last("LIMIT 10")
        );
        for (CustomerFollow follow : follows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", follow.getId());
            item.put("customerId", follow.getCustomerId());
            Customer customer = customerMapper.selectById(follow.getCustomerId());
            if (customer != null) {
                item.put("customerName", customer.getName());
            } else {
                item.put("customerName", "未知客户");
            }
            item.put("content", follow.getContent());
            item.put("followResult", getFollowResultText(follow.getFollowResult()));
            item.put("createdAt", follow.getCreatedAt());
            item.put("type", "pending_follow");
            pendingFollows.add(item);
        }

        List<Map<String, Object>> pendingCustomers = new ArrayList<>();
        List<Customer> customers = customerMapper.selectList(
                new LambdaQueryWrapper<Customer>()
                        .eq(Customer::getStatus, "warning")
                        .orderByDesc(Customer::getUpdatedAt)
                        .last("LIMIT 10")
        );
        for (Customer customer : customers) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", customer.getId());
            item.put("name", customer.getName());
            item.put("status", customer.getStatus());
            item.put("lastFollowTime", customer.getLastFollowTime());
            item.put("type", "pending_customer");
            pendingCustomers.add(item);
        }

        Map<String, Object> todos = new LinkedHashMap<>();
        todos.put("pendingFollows", pendingFollows);
        todos.put("pendingCustomers", pendingCustomers);

        return ResponseEntity.ok(ApiResponse.success(todos));
    }

    private String getFollowResultText(String followResult) {
        if (followResult == null) return "未知";
        switch (followResult) {
            case "initial_contact": return "初步接触";
            case "requirement": return "需求确认";
            case "quotation": return "报价";
            case "negotiation": return "谈判";
            case "pending_deal": return "待成交";
            case "closed": return "成交";
            case "lost": return "流失";
            case "contact_lost": return "联系失败";
            default: return followResult;
        }
    }

    private LocalDateTime[] getDateRange(String timeRange) {
        LocalDate today = LocalDate.now();
        LocalDateTime start;
        LocalDateTime end = today.atTime(LocalTime.MAX);

        switch (timeRange) {
            case "today":
                start = today.atStartOfDay();
                break;
            case "week":
                start = today.minusDays(today.getDayOfWeek().getValue() - 1).atStartOfDay();
                break;
            case "month":
                start = today.withDayOfMonth(1).atStartOfDay();
                break;
            case "quarter":
                start = today.minusMonths(2).withDayOfMonth(1).atStartOfDay();
                break;
            default:
                start = today.atStartOfDay();
        }

        return new LocalDateTime[]{start, end};
    }
}