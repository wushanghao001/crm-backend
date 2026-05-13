package com.example.crm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.crm.config.CacheConfig;
import com.example.crm.dto.ApiResponse;
import com.example.crm.entity.*;
import com.example.crm.mapper.*;
import com.example.crm.service.FunnelService;
import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/home")
public class HomeController {

    private final CustomerMapper customerMapper;
    private final OrderMapper orderMapper;
    private final ServiceTicketMapper serviceTicketMapper;
    private final CustomerFollowMapper customerFollowMapper;
    private final OpportunityMapper opportunityMapper;
    private final TaskMapper taskMapper;
    private final FunnelService funnelService;
    private final Cache<String, Object> personalStatsCache;

    @Autowired
    public HomeController(CustomerMapper customerMapper, OrderMapper orderMapper,
                        ServiceTicketMapper serviceTicketMapper,
                        CustomerFollowMapper customerFollowMapper,
                        OpportunityMapper opportunityMapper,
                        TaskMapper taskMapper,
                        FunnelService funnelService,
                        @Qualifier("personalStatsCache") Cache<String, Object> personalStatsCache) {
        this.customerMapper = customerMapper;
        this.orderMapper = orderMapper;
        this.serviceTicketMapper = serviceTicketMapper;
        this.customerFollowMapper = customerFollowMapper;
        this.opportunityMapper = opportunityMapper;
        this.taskMapper = taskMapper;
        this.funnelService = funnelService;
        this.personalStatsCache = personalStatsCache;
    }

    @GetMapping("/refresh")
    public ResponseEntity<ApiResponse<String>> refreshCache() {
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            String cacheKey = CacheConfig.PERSONAL_STATS_CACHE + "_home_" + currentUser.getId();
            personalStatsCache.invalidate(cacheKey);
        }
        return ResponseEntity.ok(ApiResponse.success("刷新成功"));
    }

    @GetMapping("/data")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHomeData(
            @RequestParam(defaultValue = "week") String timeRange) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.ok(ApiResponse.error(401, "未登录"));
        }

        String cacheKey = CacheConfig.PERSONAL_STATS_CACHE + "_home_" + currentUser.getId();

        @SuppressWarnings("unchecked")
        Map<String, Object> cached = (Map<String, Object>) personalStatsCache.getIfPresent(cacheKey);
        if (cached != null) {
            System.out.println("[HomeController] Cache HIT for key: " + cacheKey);
            return ResponseEntity.ok(ApiResponse.success(cached));
        }

        System.out.println("[HomeController] Cache MISS for key: " + cacheKey + ", loading from database");

        Map<String, Object> result = new HashMap<>();

        // 1. 个人统计数据
        Map<String, Object> personalStats = loadPersonalStats(currentUser.getId());
        result.put("personalStats", personalStats);

        // 2. 最近跟进记录
        List<Map<String, Object>> recentFollows = loadRecentFollows(currentUser.getId());
        result.put("recentFollows", recentFollows);

        // 3. 我的待办任务
        List<Map<String, Object>> myTasks = loadMyTasks(currentUser.getId());
        result.put("myTasks", myTasks);

        // 4. 销售漏斗数据（使用 FunnelService 逻辑，但加上用户过滤）
        Map<String, Object> funnelData = loadFunnelData(currentUser.getId(), timeRange);
        result.put("funnelData", funnelData);

        // 5. 趋势数据
        Map<String, Object> trendData = loadTrendData(currentUser.getId(), timeRange);
        result.put("trendData", trendData);

        personalStatsCache.put(cacheKey, result);
        System.out.println("[HomeController] Cache PUT for key: " + cacheKey);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private Map<String, Object> loadPersonalStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();

        Long myCustomers = customerMapper.selectCount(
            new LambdaQueryWrapper<Customer>()
                .eq(Customer::getCreatorId, userId)
        );

        Long publicCustomers = customerMapper.selectCount(
            new LambdaQueryWrapper<Customer>()
                .isNull(Customer::getCreatorId)
        );

        Long myOrders = orderMapper.selectCount(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getCreatorId, userId)
        );

        Long pendingServices = serviceTicketMapper.selectCount(
            new LambdaQueryWrapper<ServiceTicket>()
                .eq(ServiceTicket::getAssigneeId, userId)
                .eq(ServiceTicket::getStatus, "pending")
        );

        stats.put("myCustomers", myCustomers);
        stats.put("publicCustomers", publicCustomers);
        stats.put("myOrders", myOrders);
        stats.put("pendingServices", pendingServices);

        return stats;
    }

    private List<Map<String, Object>> loadRecentFollows(Long userId) {
        List<CustomerFollow> follows = customerFollowMapper.selectList(
            new LambdaQueryWrapper<CustomerFollow>()
                .eq(CustomerFollow::getFollowUserId, userId)
                .orderByDesc(CustomerFollow::getCreatedAt)
                .last("LIMIT 3")
        );

        return follows.stream().map(follow -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", follow.getId());
            item.put("customerId", follow.getCustomerId());
            if (follow.getCustomerId() != null) {
                Customer customer = customerMapper.selectById(follow.getCustomerId());
                item.put("customerName", customer != null ? customer.getName() : "未知客户");
            } else {
                item.put("customerName", "未知客户");
            }
            item.put("content", follow.getContent());
            item.put("createdAt", follow.getCreatedAt());
            return item;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> loadMyTasks(Long userId) {
        List<Task> tasks = taskMapper.selectList(
            new LambdaQueryWrapper<Task>()
                .eq(Task::getAssigneeId, userId)
                .eq(Task::getStatus, "pending")
                .orderByAsc(Task::getDueDate)
                .last("LIMIT 10")
        );

        return tasks.stream().map(task -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", task.getId());
            item.put("title", task.getTitle());
            item.put("content", task.getContent());
            item.put("dueDate", task.getDueDate());
            item.put("priority", task.getPriority());
            item.put("status", task.getStatus());
            item.put("taskType", task.getTaskType());
            return item;
        }).collect(Collectors.toList());
    }

    private Map<String, Object> loadFunnelData(Long userId, String timeRange) {
        LocalDateTime startDateTime = getStartDateTime(timeRange);
        LocalDateTime endDateTime = LocalDateTime.now();

        LambdaQueryWrapper<CustomerFollow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CustomerFollow::getFollowUserId, userId);
        if (startDateTime != null) {
            queryWrapper.ge(CustomerFollow::getCreatedAt, startDateTime);
        }
        queryWrapper.le(CustomerFollow::getCreatedAt, endDateTime);
        queryWrapper.orderByDesc(CustomerFollow::getCreatedAt);

        List<CustomerFollow> follows = customerFollowMapper.selectList(queryWrapper);

        Map<String, Object> funnel = new HashMap<>();

        funnel.put("leads", follows.size());

        long contacted = follows.stream()
                .filter(f -> f.getFollowResult() != null &&
                        (f.getFollowResult().equals("initial_contact") ||
                                f.getFollowResult().equals("requirement") ||
                                f.getFollowResult().equals("quotation") ||
                                f.getFollowResult().equals("negotiation") ||
                                f.getFollowResult().equals("pending_deal")))
                .count();
        funnel.put("contacted", contacted);

        long quoted = follows.stream()
                .filter(f -> f.getFollowResult() != null &&
                        (f.getFollowResult().equals("quotation") ||
                                f.getFollowResult().equals("negotiation") ||
                                f.getFollowResult().equals("pending_deal")))
                .count();
        funnel.put("quoted", quoted);

        long won = follows.stream()
                .filter(f -> f.getFollowResult() != null && f.getFollowResult().equals("closed"))
                .count();
        funnel.put("won", won);

        long lost = follows.stream()
                .filter(f -> f.getFollowResult() != null &&
                        (f.getFollowResult().equals("lost") || f.getFollowResult().equals("contact_lost")))
                .count();
        funnel.put("lost", lost);

        int leads = follows.size();
        funnel.put("contactRate", leads > 0 ? (double) contacted / leads * 100 : 0);
        funnel.put("quoteRate", contacted > 0 ? (double) quoted / contacted * 100 : 0);
        funnel.put("winRate", quoted > 0 ? (double) won / quoted * 100 : 0);
        funnel.put("totalWinRate", leads > 0 ? (double) won / leads * 100 : 0);

        return funnel;
    }

    private Map<String, Object> loadTrendData(Long userId, String timeRange) {
        Map<String, Object> trend = new HashMap<>();
        LocalDateTime startDateTime = getStartDateTime(timeRange);
        LocalDateTime endDateTime = LocalDateTime.now();
        LocalDate startDate = startDateTime.toLocalDate();
        LocalDate endDate = endDateTime.toLocalDate();

        LambdaQueryWrapper<CustomerFollow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CustomerFollow::getFollowUserId, userId);
        if (startDateTime != null) {
            queryWrapper.ge(CustomerFollow::getCreatedAt, startDateTime);
        }
        queryWrapper.le(CustomerFollow::getCreatedAt, endDateTime);
        List<CustomerFollow> allFollows = customerFollowMapper.selectList(queryWrapper);

        List<String> dates = new ArrayList<>();
        List<Integer> wonData = new ArrayList<>();
        List<Integer> lostData = new ArrayList<>();
        List<Integer> totalData = new ArrayList<>();

        switch (timeRange.toLowerCase()) {
            case "today":
                for (int hour = 0; hour <= 23; hour++) {
                    final int h = hour;
                    List<CustomerFollow> hourFollows = allFollows.stream()
                            .filter(f -> f.getCreatedAt().getHour() == h)
                            .collect(Collectors.toList());
                    dates.add(hour + ":00");
                    wonData.add((int) hourFollows.stream()
                            .filter(f -> "closed".equals(f.getFollowResult())).count());
                    lostData.add((int) hourFollows.stream()
                            .filter(f -> "lost".equals(f.getFollowResult()) || "contact_lost".equals(f.getFollowResult())).count());
                    totalData.add(hourFollows.size());
                }
                break;
            case "week":
                LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                for (int i = 0; i < 7; i++) {
                    LocalDate date = weekStart.plusDays(i);
                    final LocalDate finalDate = date;
                    List<CustomerFollow> dayFollows = allFollows.stream()
                            .filter(f -> f.getCreatedAt().toLocalDate().equals(finalDate))
                            .collect(Collectors.toList());
                    String dayName = date.getDayOfWeek().toString().substring(0, 3);
                    dates.add(dayName);
                    wonData.add((int) dayFollows.stream()
                            .filter(f -> "closed".equals(f.getFollowResult())).count());
                    lostData.add((int) dayFollows.stream()
                            .filter(f -> "lost".equals(f.getFollowResult()) || "contact_lost".equals(f.getFollowResult())).count());
                    totalData.add(dayFollows.size());
                }
                break;
            case "month":
                for (int day = 1; day <= endDate.getDayOfMonth(); day++) {
                    final int d = day;
                    List<CustomerFollow> dayFollows = allFollows.stream()
                            .filter(f -> f.getCreatedAt().getDayOfMonth() == d)
                            .collect(Collectors.toList());
                    dates.add(day + "日");
                    wonData.add((int) dayFollows.stream()
                            .filter(f -> "closed".equals(f.getFollowResult())).count());
                    lostData.add((int) dayFollows.stream()
                            .filter(f -> "lost".equals(f.getFollowResult()) || "contact_lost".equals(f.getFollowResult())).count());
                    totalData.add(dayFollows.size());
                }
                break;
            default:
                LocalDate quarterStart = startDate;
                int totalDays = (int) (endDate.toEpochDay() - quarterStart.toEpochDay()) + 1;
                int weeks = (totalDays + 6) / 7;
                for (int week = 0; week < weeks; week++) {
                    LocalDate weekStartDate = quarterStart.plusDays(week * 7L);
                    LocalDate weekEndDate = weekStartDate.plusDays(6);
                    if (weekEndDate.isAfter(endDate)) {
                        weekEndDate = endDate;
                    }
                    final LocalDate ws = weekStartDate;
                    final LocalDate we = weekEndDate;
                    List<CustomerFollow> weekFollows = allFollows.stream()
                            .filter(f -> {
                                LocalDate fd = f.getCreatedAt().toLocalDate();
                                return !fd.isBefore(ws) && !fd.isAfter(we);
                            })
                            .collect(Collectors.toList());
                    dates.add("第" + (week + 1) + "周");
                    wonData.add((int) weekFollows.stream()
                            .filter(f -> "closed".equals(f.getFollowResult())).count());
                    lostData.add((int) weekFollows.stream()
                            .filter(f -> "lost".equals(f.getFollowResult()) || "contact_lost".equals(f.getFollowResult())).count());
                    totalData.add(weekFollows.size());
                }
                break;
        }

        trend.put("dates", dates);
        trend.put("wonData", wonData);
        trend.put("lostData", lostData);
        trend.put("totalData", totalData);

        return trend;
    }

    private LocalDateTime getStartDateTime(String timeRange) {
        LocalDate today = LocalDate.now();
        switch (timeRange.toLowerCase()) {
            case "today":
                return today.atStartOfDay();
            case "week":
                return today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
            case "month":
                return today.withDayOfMonth(1).atStartOfDay();
            case "quarter":
            case "90days":
                return today.minusDays(89).atStartOfDay();
            default:
                return today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        }
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof com.example.crm.security.JwtAuthenticationToken) {
            return (User) auth.getPrincipal();
        }
        return null;
    }
}