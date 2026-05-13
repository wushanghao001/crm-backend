package com.example.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.crm.dto.CustomerBrief;
import com.example.crm.dto.FunnelResponse;
import com.example.crm.dto.StageCustomersResponse;
import com.example.crm.dto.TrendResponse;
import com.example.crm.entity.Customer;
import com.example.crm.entity.CustomerFollow;
import com.example.crm.mapper.CustomerFollowMapper;
import com.example.crm.mapper.CustomerMapper;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FunnelService {

    private final CustomerFollowMapper customerFollowMapper;
    private final CustomerMapper customerMapper;

    public FunnelService(CustomerFollowMapper customerFollowMapper, CustomerMapper customerMapper) {
        this.customerFollowMapper = customerFollowMapper;
        this.customerMapper = customerMapper;
    }

    public FunnelResponse getFunnelData(String timeRange) {
        LocalDateTime startDateTime = getStartDateTime(timeRange);
        LocalDateTime endDateTime = LocalDateTime.now();

        LambdaQueryWrapper<CustomerFollow> queryWrapper = new LambdaQueryWrapper<>();
        if (startDateTime != null) {
            queryWrapper.ge(CustomerFollow::getCreatedAt, startDateTime);
        }
        queryWrapper.le(CustomerFollow::getCreatedAt, endDateTime);
        queryWrapper.orderByDesc(CustomerFollow::getCreatedAt);

        List<CustomerFollow> follows = customerFollowMapper.selectList(queryWrapper);

        FunnelResponse response = new FunnelResponse();

        response.setLeads(follows.size());

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
        response.setWinRate(response.getQuoted() > 0 ?
                (double) response.getWon() / response.getQuoted() * 100 : 0);
        response.setTotalWinRate(response.getLeads() > 0 ?
                (double) response.getWon() / response.getLeads() * 100 : 0);

        return response;
    }

    public TrendResponse getTrendData(String timeRange) {
        TrendResponse response = new TrendResponse();
        LocalDateTime startDateTime = getStartDateTime(timeRange);
        LocalDateTime endDateTime = LocalDateTime.now();
        LocalDate startDate = startDateTime.toLocalDate();
        LocalDate endDate = endDateTime.toLocalDate();

        LambdaQueryWrapper<CustomerFollow> queryWrapper = new LambdaQueryWrapper<>();
        if (startDateTime != null) {
            queryWrapper.ge(CustomerFollow::getCreatedAt, startDateTime);
        }
        queryWrapper.le(CustomerFollow::getCreatedAt, endDateTime);
        List<CustomerFollow> allFollows = customerFollowMapper.selectList(queryWrapper);

        switch (timeRange.toLowerCase()) {
            case "today":
                for (int hour = 0; hour <= 23; hour++) {
                    final int h = hour;
                    List<CustomerFollow> hourFollows = allFollows.stream()
                            .filter(f -> f.getCreatedAt().getHour() == h)
                            .collect(Collectors.toList());
                    response.getDates().add(hour + ":00");
                    response.getWonData().add((int) hourFollows.stream()
                            .filter(f -> "closed".equals(f.getFollowResult())).count());
                    response.getLostData().add((int) hourFollows.stream()
                            .filter(f -> "lost".equals(f.getFollowResult()) || "contact_lost".equals(f.getFollowResult())).count());
                    response.getTotalData().add(hourFollows.size());
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
                    response.getDates().add(dayName);
                    response.getWonData().add((int) dayFollows.stream()
                            .filter(f -> "closed".equals(f.getFollowResult())).count());
                    response.getLostData().add((int) dayFollows.stream()
                            .filter(f -> "lost".equals(f.getFollowResult()) || "contact_lost".equals(f.getFollowResult())).count());
                    response.getTotalData().add(dayFollows.size());
                }
                break;
            case "month":
                for (int day = 1; day <= endDate.getDayOfMonth(); day++) {
                    final int d = day;
                    List<CustomerFollow> dayFollows = allFollows.stream()
                            .filter(f -> f.getCreatedAt().getDayOfMonth() == d)
                            .collect(Collectors.toList());
                    response.getDates().add(day + "日");
                    response.getWonData().add((int) dayFollows.stream()
                            .filter(f -> "closed".equals(f.getFollowResult())).count());
                    response.getLostData().add((int) dayFollows.stream()
                            .filter(f -> "lost".equals(f.getFollowResult()) || "contact_lost".equals(f.getFollowResult())).count());
                    response.getTotalData().add(dayFollows.size());
                }
                break;
            case "quarter":
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
                    response.getDates().add("第" + (week + 1) + "周");
                    response.getWonData().add((int) weekFollows.stream()
                            .filter(f -> "closed".equals(f.getFollowResult())).count());
                    response.getLostData().add((int) weekFollows.stream()
                            .filter(f -> "lost".equals(f.getFollowResult()) || "contact_lost".equals(f.getFollowResult())).count());
                    response.getTotalData().add(weekFollows.size());
                }
                break;
            default:
                for (int day = 0; day < 7; day++) {
                    LocalDate date = startDate.plusDays(day);
                    final LocalDate finalDate = date;
                    List<CustomerFollow> dayFollows = allFollows.stream()
                            .filter(f -> f.getCreatedAt().toLocalDate().equals(finalDate))
                            .collect(Collectors.toList());
                    response.getDates().add(date.toString());
                    response.getWonData().add((int) dayFollows.stream()
                            .filter(f -> "closed".equals(f.getFollowResult())).count());
                    response.getLostData().add((int) dayFollows.stream()
                            .filter(f -> "lost".equals(f.getFollowResult()) || "contact_lost".equals(f.getFollowResult())).count());
                    response.getTotalData().add(dayFollows.size());
                }
        }

        return response;
    }

    public StageCustomersResponse getStageCustomers(String stage) {
        LambdaQueryWrapper<CustomerFollow> queryWrapper = new LambdaQueryWrapper<>();

        switch (stage) {
            case "contacted":
                queryWrapper.in(CustomerFollow::getFollowResult,
                    "initial_contact", "requirement", "quotation", "negotiation", "pending_deal");
                break;
            case "quoted":
                queryWrapper.in(CustomerFollow::getFollowResult,
                    "quotation", "negotiation", "pending_deal");
                break;
            case "won":
                queryWrapper.eq(CustomerFollow::getFollowResult, "closed");
                break;
            case "lost":
                queryWrapper.in(CustomerFollow::getFollowResult, "lost", "contact_lost");
                break;
            default:
                break;
        }

        List<CustomerFollow> follows = customerFollowMapper.selectList(queryWrapper);

        Set<Long> customerIds = new HashSet<>();
        for (CustomerFollow f : follows) {
            if (f.getCustomerId() != null) {
                customerIds.add(f.getCustomerId().longValue());
            }
        }

        StageCustomersResponse response = new StageCustomersResponse();

        if (!customerIds.isEmpty()) {
            List<Customer> customers = customerMapper.selectBatchIds(customerIds);
            response.setCustomers(customers.stream().map(c -> {
                CustomerBrief brief = new CustomerBrief();
                brief.setId(c.getId());
                brief.setName(c.getName());
                brief.setPhone(c.getPhone());
                brief.setIndustry(c.getIndustry());
                brief.setStatus(c.getStatus());
                return brief;
            }).collect(Collectors.toList()));
            response.setTotal(response.getCustomers().size());
        } else {
            response.setCustomers(new ArrayList<>());
            response.setTotal(0);
        }

        return response;
    }

    private LocalDateTime getStartDateTime(String timeRange) {
        LocalDate now = LocalDate.now();

        return switch (timeRange.toLowerCase()) {
            case "today" -> now.atStartOfDay();
            case "week" -> now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
            case "month" -> now.withDayOfMonth(1).atStartOfDay();
            case "quarter" -> {
                int currentMonth = now.getMonthValue();
                int quarterStartMonth = ((currentMonth - 1) / 3) * 3 + 1;
                yield now.withMonth(quarterStartMonth).withDayOfMonth(1).atStartOfDay();
            }
            default -> null;
        };
    }
}
