package com.example.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.crm.dto.PageResponse;
import com.example.crm.dto.UserPerformanceResponse;
import com.example.crm.dto.UserPerformanceTrendResponse;
import com.example.crm.entity.Customer;
import com.example.crm.entity.CustomerFollow;
import com.example.crm.entity.Order;
import com.example.crm.entity.User;
import com.example.crm.mapper.CustomerFollowMapper;
import com.example.crm.mapper.CustomerMapper;
import com.example.crm.mapper.OrderMapper;
import com.example.crm.mapper.UserMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserPerformanceService {

    private final CustomerFollowMapper customerFollowMapper;
    private final CustomerMapper customerMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;

    private static final BigDecimal DEFAULT_MONTH_TARGET = new BigDecimal("100000");

    public UserPerformanceService(CustomerFollowMapper customerFollowMapper, CustomerMapper customerMapper, OrderMapper orderMapper, UserMapper userMapper) {
        this.customerFollowMapper = customerFollowMapper;
        this.customerMapper = customerMapper;
        this.orderMapper = orderMapper;
        this.userMapper = userMapper;
    }

    public void updateTarget(BigDecimal target) {
        User currentUser = getCurrentUser();
        currentUser.setMonthTarget(target);
        userMapper.updateById(currentUser);
    }

    public BigDecimal getUserTarget() {
        User currentUser = getCurrentUser();
        return currentUser.getMonthTarget() != null ? currentUser.getMonthTarget() : DEFAULT_MONTH_TARGET;
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        throw new IllegalStateException("无法获取当前用户信息");
    }

    public UserPerformanceResponse getPerformance(String timeRange) {
        User currentUser = getCurrentUser();
        UserPerformanceResponse response = new UserPerformanceResponse();

        LocalDateTime startDateTime = getStartDateTime(timeRange);
        LocalDateTime endDateTime = LocalDateTime.now();

        LambdaQueryWrapper<CustomerFollow> followQueryWrapper = new LambdaQueryWrapper<>();
        followQueryWrapper.eq(CustomerFollow::getFollowUserId, currentUser.getId().intValue());
        if (startDateTime != null) {
            followQueryWrapper.ge(CustomerFollow::getCreatedAt, startDateTime);
        }
        followQueryWrapper.le(CustomerFollow::getCreatedAt, endDateTime);
        List<CustomerFollow> follows = customerFollowMapper.selectList(followQueryWrapper);

        Set<Integer> followedCustomerIds = follows.stream()
                .map(CustomerFollow::getCustomerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        response.setLeads(follows.size());
        response.setFollowCustomerCount(followedCustomerIds.size());

        long contacted = follows.stream()
                .filter(f -> f.getFollowResult() != null &&
                        (f.getFollowResult().equals("initial_contact") ||
                                f.getFollowResult().equals("requirement") ||
                                f.getFollowResult().equals("quotation") ||
                                f.getFollowResult().equals("negotiation") ||
                                f.getFollowResult().equals("pending_deal")))
                .count();
        response.setContacted((int) contacted);

        long quoted = follows.stream()
                .filter(f -> f.getFollowResult() != null &&
                        (f.getFollowResult().equals("quotation") ||
                                f.getFollowResult().equals("negotiation") ||
                                f.getFollowResult().equals("pending_deal")))
                .count();
        response.setQuoted((int) quoted);

        long won = follows.stream()
                .filter(f -> f.getFollowResult() != null && f.getFollowResult().equals("closed"))
                .count();
        response.setWon((int) won);

        long lost = follows.stream()
                .filter(f -> f.getFollowResult() != null &&
                        (f.getFollowResult().equals("lost") || f.getFollowResult().equals("contact_lost")))
                .count();
        response.setLost((int) lost);

        response.setContactRate(response.getLeads() > 0 ?
                (double) response.getContacted() / response.getLeads() * 100 : 0);
        response.setQuoteRate(response.getContacted() > 0 ?
                (double) response.getQuoted() / response.getContacted() * 100 : 0);
        response.setWinRateStage(response.getQuoted() > 0 ?
                (double) response.getWon() / response.getQuoted() * 100 : 0);
        response.setTotalWinRate(response.getLeads() > 0 ?
                (double) response.getWon() / response.getLeads() * 100 : 0);

        LambdaQueryWrapper<Order> orderQueryWrapper = new LambdaQueryWrapper<>();
        orderQueryWrapper.eq(Order::getCreatorId, currentUser.getId());
        orderQueryWrapper.eq(Order::getStatus, "completed");
        if (startDateTime != null) {
            orderQueryWrapper.ge(Order::getCreatedAt, startDateTime);
        }
        orderQueryWrapper.le(Order::getCreatedAt, endDateTime);
        List<Order> orders = orderMapper.selectList(orderQueryWrapper);

        BigDecimal totalSales = orders.stream()
                .map(Order::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        response.setTotalSales(totalSales);

        Set<Long> wonCustomerIds = orders.stream()
                .map(Order::getCustomerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        response.setWonCustomerCount(wonCustomerIds.size());

        if (response.getFollowCustomerCount() > 0) {
            response.setWinRate((double) response.getWonCustomerCount() / response.getFollowCustomerCount() * 100);
        } else {
            response.setWinRate(0);
        }

        LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDateTime monthStart = firstDayOfMonth.atStartOfDay();
        LocalDateTime monthEnd = LocalDateTime.now();

        LambdaQueryWrapper<Order> monthOrderWrapper = new LambdaQueryWrapper<>();
        monthOrderWrapper.eq(Order::getCreatorId, currentUser.getId());
        monthOrderWrapper.eq(Order::getStatus, "completed");
        monthOrderWrapper.ge(Order::getCreatedAt, monthStart);
        monthOrderWrapper.le(Order::getCreatedAt, monthEnd);
        List<Order> monthOrders = orderMapper.selectList(monthOrderWrapper);

        BigDecimal currentMonthSales = monthOrders.stream()
                .map(Order::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        response.setCurrentMonthSales(currentMonthSales);

        BigDecimal userTarget = getUserTarget();
        response.setMonthTarget(userTarget);
        double completion = userTarget.compareTo(BigDecimal.ZERO) > 0 ?
                currentMonthSales.divide(userTarget, 4, RoundingMode.HALF_UP).doubleValue() * 100 : 0;
        response.setMonthTargetCompletion(completion);

        return response;
    }

    public UserPerformanceTrendResponse getTrend(Integer months) {
        User currentUser = getCurrentUser();
        UserPerformanceTrendResponse response = new UserPerformanceTrendResponse();

        LocalDate now = LocalDate.now();
        List<String> monthLabels = new ArrayList<>();
        List<BigDecimal> salesData = new ArrayList<>();
        List<Integer> orderCountData = new ArrayList<>();

        for (int i = months - 1; i >= 0; i--) {
            LocalDate monthDate = now.minusMonths(i);
            String monthLabel = monthDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            monthLabels.add(monthLabel);

            LocalDate firstDay = monthDate.withDayOfMonth(1);
            LocalDate lastDay = monthDate.with(TemporalAdjusters.lastDayOfMonth());

            LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
            orderWrapper.eq(Order::getCreatorId, currentUser.getId());
            orderWrapper.eq(Order::getStatus, "completed");
            orderWrapper.ge(Order::getCreatedAt, firstDay.atStartOfDay());
            orderWrapper.le(Order::getCreatedAt, lastDay.atTime(LocalTime.MAX));
            List<Order> orders = orderMapper.selectList(orderWrapper);

            BigDecimal monthSales = orders.stream()
                    .map(Order::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            salesData.add(monthSales);
            orderCountData.add(orders.size());
        }

        response.setMonths(monthLabels);
        response.setSalesData(salesData);
        response.setOrderCountData(orderCountData);

        LambdaQueryWrapper<Customer> customerWrapper = new LambdaQueryWrapper<>();
        customerWrapper.eq(Customer::getCreatorId, currentUser.getId());
        List<Customer> customers = customerMapper.selectList(customerWrapper);

        Map<String, Long> sourceCountMap = customers.stream()
                .filter(c -> c.getSource() != null && !c.getSource().isEmpty())
                .collect(Collectors.groupingBy(Customer::getSource, Collectors.counting()));

        List<String> sources = new ArrayList<>(sourceCountMap.keySet());
        List<Integer> counts = sourceCountMap.values().stream().map(Long::intValue).collect(Collectors.toList());

        response.setCustomerSources(sources);
        response.setSourceCounts(counts);

        return response;
    }

    public PageResponse<Order> getOrders(Integer pageNum, Integer pageSize, String keyword, String startDate, String endDate) {
        User currentUser = getCurrentUser();
        Page<Order> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getCreatorId, currentUser.getId());
        queryWrapper.eq(Order::getStatus, "completed");

        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.and(w -> w.like(Order::getCustomerName, keyword)
                    .or().like(Order::getOrderNo, keyword));
        }

        if (startDate != null && !startDate.isEmpty()) {
            queryWrapper.ge(Order::getCreatedAt, LocalDateTime.parse(startDate + "T00:00:00"));
        }
        if (endDate != null && !endDate.isEmpty()) {
            queryWrapper.le(Order::getCreatedAt, LocalDateTime.parse(endDate + "T23:59:59"));
        }

        queryWrapper.orderByDesc(Order::getCreatedAt);
        Page<Order> result = orderMapper.selectPage(page, queryWrapper);

        PageResponse<Order> response = new PageResponse<Order>();
        response.setList(result.getRecords());
        response.setTotal(result.getTotal());
        response.setPageNum(pageNum);
        response.setPageSize(pageSize);

        return response;
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
                int currentMonth = today.getMonthValue();
                int quarterStartMonth = ((currentMonth - 1) / 3) * 3 + 1;
                return today.withMonth(quarterStartMonth).withDayOfMonth(1).atStartOfDay();
            case "year":
                return today.withDayOfYear(1).atStartOfDay();
            default:
                return today.withDayOfMonth(1).atStartOfDay();
        }
    }
}