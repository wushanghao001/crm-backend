
package com.example.crm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.crm.dto.ApiResponse;
import com.example.crm.entity.Customer;
import com.example.crm.entity.CustomerFollow;
import com.example.crm.entity.Order;
import com.example.crm.entity.OrderItem;
import com.example.crm.entity.Product;
import com.example.crm.entity.User;
import com.example.crm.mapper.CustomerFollowMapper;
import com.example.crm.mapper.CustomerMapper;
import com.example.crm.mapper.OrderItemMapper;
import com.example.crm.mapper.OrderMapper;
import com.example.crm.mapper.ProductMapper;
import com.example.crm.mapper.UserMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final CustomerMapper customerMapper;
    private final OrderMapper orderMapper;
    private final CustomerFollowMapper customerFollowMapper;
    private final UserMapper userMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;

    public StatisticsController(CustomerMapper customerMapper, OrderMapper orderMapper,
                               CustomerFollowMapper customerFollowMapper, UserMapper userMapper,
                               OrderItemMapper orderItemMapper, ProductMapper productMapper) {
        this.customerMapper = customerMapper;
        this.orderMapper = orderMapper;
        this.customerFollowMapper = customerFollowMapper;
        this.userMapper = userMapper;
        this.orderItemMapper = orderItemMapper;
        this.productMapper = productMapper;
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

    @GetMapping("/user-sales")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUserSalesByTime(@RequestParam String timeType) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime;
        LocalDateTime endTime = now;

        switch (timeType) {
            case "day":
                startTime = now.toLocalDate().atStartOfDay();
                break;
            case "week":
                startTime = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toLocalDate().atStartOfDay();
                break;
            case "month":
                startTime = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
                break;
            case "quarter":
                int quarter = (now.getMonthValue() - 1) / 3;
                int quarterMonth = quarter * 3 + 1;
                startTime = now.withMonth(quarterMonth).withDayOfMonth(1).toLocalDate().atStartOfDay();
                break;
            default:
                startTime = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
        }

        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getStatus, "completed")
                    .eq(Order::getPayStatus, "paid")
                    .between(Order::getPaidAt, startTime, endTime);
        List<Order> orders = orderMapper.selectList(queryWrapper);

        Map<Long, BigDecimal> userSalesMap = new HashMap<>();
        for (Order order : orders) {
            Long userId = order.getCreatorId();
            if (userId != null) {
                BigDecimal amount = order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO;
                userSalesMap.merge(userId, amount, BigDecimal::add);
            }
        }

        List<User> users = userMapper.selectList(null);
        List<Map<String, Object>> result = new ArrayList<>();
        for (User user : users) {
            if ("admin".equals(user.getRole())) {
                continue;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("userId", user.getId());
            item.put("userName", user.getUsername());
            item.put("amount", userSalesMap.getOrDefault(user.getId(), BigDecimal.ZERO));
            result.add(item);
        }

        result.sort((a, b) -> {
            BigDecimal amountA = (BigDecimal) a.get("amount");
            BigDecimal amountB = (BigDecimal) b.get("amount");
            return amountB.compareTo(amountA);
        });

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/product-sales")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getProductSales(@RequestParam(required = false) String timeType) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime;
        LocalDateTime endTime = now;

        if (timeType == null) {
            timeType = "month";
        }

        switch (timeType) {
            case "day":
                startTime = now.toLocalDate().atStartOfDay();
                break;
            case "week":
                startTime = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toLocalDate().atStartOfDay();
                break;
            case "month":
                startTime = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
                break;
            case "quarter":
                int quarter = (now.getMonthValue() - 1) / 3;
                int quarterMonth = quarter * 3 + 1;
                startTime = now.withMonth(quarterMonth).withDayOfMonth(1).toLocalDate().atStartOfDay();
                break;
            default:
                startTime = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
        }

        LambdaQueryWrapper<Order> orderQueryWrapper = new LambdaQueryWrapper<>();
        orderQueryWrapper.eq(Order::getStatus, "completed")
                        .eq(Order::getPayStatus, "paid")
                        .between(Order::getPaidAt, startTime, endTime);
        List<Order> paidOrders = orderMapper.selectList(orderQueryWrapper);

        Map<Long, Long> productQuantityMap = new HashMap<>();
        for (Order order : paidOrders) {
            LambdaQueryWrapper<OrderItem> itemQueryWrapper = new LambdaQueryWrapper<>();
            itemQueryWrapper.eq(OrderItem::getOrderId, order.getId());
            List<OrderItem> items = orderItemMapper.selectList(itemQueryWrapper);
            for (OrderItem item : items) {
                if (item.getProductId() != null && item.getQuantity() != null) {
                    productQuantityMap.merge(item.getProductId(), item.getQuantity().longValue(), Long::sum);
                }
            }
        }

        List<Product> products = productMapper.selectList(null);
        Map<Long, String> productNameMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p.getName() != null ? p.getName() : "未知产品"));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : productQuantityMap.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("productId", entry.getKey());
            item.put("productName", productNameMap.getOrDefault(entry.getKey(), "未知产品"));
            item.put("quantity", entry.getValue());
            result.add(item);
        }

        result.sort((a, b) -> {
            Long qtyA = (Long) a.get("quantity");
            Long qtyB = (Long) b.get("quantity");
            return qtyB.compareTo(qtyA);
        });

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportReport() {
        System.out.println("========== 开始导出报表 ==========");
        try {
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("统计分析报表");

            LocalDateTime now = LocalDateTime.now();
            int currentMonth = now.getMonthValue();
            int currentYear = now.getYear();

            System.out.println("当前年月: " + currentYear + "年" + currentMonth + "月");

            Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue(currentYear + "年" + currentMonth + "月统计分析报表");

            Row headerRow = sheet.createRow(2);
            String[] headers = {"指标", "数值", "说明"};
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            List<Customer> allCustomers = customerMapper.selectList(null);
            if (allCustomers == null) {
                allCustomers = new ArrayList<>();
            }

            long totalCustomers = allCustomers.size();
            long activeCustomers = allCustomers.stream()
                    .filter(c -> "active".equals(c.getStatus())).count();
            long newCustomers = allCustomers.stream()
                    .filter(c -> c.getCreatedAt() != null &&
                            c.getCreatedAt().getMonthValue() == currentMonth &&
                            c.getCreatedAt().getYear() == currentYear).count();
            long churnedCustomers = allCustomers.stream()
                    .filter(c -> "churned".equals(c.getStatus())).count();

            System.out.println("客户统计 - 总数:" + totalCustomers + ", 活跃:" + activeCustomers + ", 新增:" + newCustomers + ", 流失:" + churnedCustomers);

            BigDecimal thisMonthSales = BigDecimal.ZERO;
            List<Order> allOrders = orderMapper.selectList(null);
            if (allOrders == null) {
                allOrders = new ArrayList<>();
            }
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

            System.out.println("本月销售额: " + thisMonthSales);

            double churnRate = totalCustomers > 0 ? ((double) churnedCustomers / totalCustomers) * 100 : 0;

            String[][] data = {
                {"本月新增客户", String.valueOf(newCustomers), "本月新增客户数量"},
                {"活跃客户", String.valueOf(activeCustomers), "当前状态为活跃的客户数量"},
                {"客户流失率", String.format("%.1f%%", churnRate), "已流失客户占总客户比例"},
                {"本月销售额", "¥" + thisMonthSales.toString(), "本月已完成支付订单金额"},
                {"总客户数", String.valueOf(totalCustomers), "系统中所有客户数量"}
            };

            for (int i = 0; i < data.length; i++) {
                Row row = sheet.createRow(i + 3);
                for (int j = 0; j < data[i].length; j++) {
                    row.createCell(j).setCellValue(data[i][j]);
                }
            }

            Sheet userSalesSheet = workbook.createSheet("用户销售统计");
            userSalesSheet.createRow(0).createCell(0).setCellValue("用户名称");
            userSalesSheet.getRow(0).createCell(1).setCellValue("销售额");
            userSalesSheet.getRow(0).createCell(2).setCellValue("排名");

            LocalDateTime startTime = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
            LambdaQueryWrapper<Order> orderQueryWrapper = new LambdaQueryWrapper<>();
            orderQueryWrapper.eq(Order::getStatus, "completed")
                    .eq(Order::getPayStatus, "paid")
                    .between(Order::getPaidAt, startTime, now);
            List<Order> paidOrders = orderMapper.selectList(orderQueryWrapper);
            if (paidOrders == null) {
                paidOrders = new ArrayList<>();
            }
            System.out.println("本月支付订单数: " + paidOrders.size());

            Map<Long, BigDecimal> userSalesMap = new HashMap<>();
            for (Order order : paidOrders) {
                Long userId = order.getCreatorId();
                if (userId != null) {
                    BigDecimal amount = order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO;
                    userSalesMap.merge(userId, amount, BigDecimal::add);
                }
            }

            List<User> users = userMapper.selectList(null);
            if (users == null) {
                users = new ArrayList<>();
            }
            List<Map<String, Object>> userSalesList = new ArrayList<>();
            for (User user : users) {
                if ("admin".equals(user.getRole())) continue;
                Map<String, Object> item = new HashMap<>();
                item.put("userName", user.getUsername());
                item.put("amount", userSalesMap.getOrDefault(user.getId(), BigDecimal.ZERO));
                userSalesList.add(item);
            }
            userSalesList.sort((a, b) -> {
                BigDecimal amountA = (BigDecimal) a.get("amount");
                BigDecimal amountB = (BigDecimal) b.get("amount");
                if (amountA == null) amountA = BigDecimal.ZERO;
                if (amountB == null) amountB = BigDecimal.ZERO;
                return amountB.compareTo(amountA);
            });

            for (int i = 0; i < userSalesList.size(); i++) {
                Map<String, Object> userData = userSalesList.get(i);
                Row row = userSalesSheet.createRow(i + 1);
                row.createCell(0).setCellValue(userData.get("userName") != null ? (String) userData.get("userName") : "");
                row.createCell(1).setCellValue(userData.get("amount") != null ? userData.get("amount").toString() : "0");
                row.createCell(2).setCellValue(String.valueOf(i + 1));
            }

            Sheet productSalesSheet = workbook.createSheet("产品销售排行");
            productSalesSheet.createRow(0).createCell(0).setCellValue("产品名称");
            productSalesSheet.getRow(0).createCell(1).setCellValue("销售数量");
            productSalesSheet.getRow(0).createCell(2).setCellValue("排名");

            Map<Long, Long> productQuantityMap = new HashMap<>();
            for (Order order : paidOrders) {
                LambdaQueryWrapper<OrderItem> itemQueryWrapper = new LambdaQueryWrapper<>();
                itemQueryWrapper.eq(OrderItem::getOrderId, order.getId());
                List<OrderItem> items = orderItemMapper.selectList(itemQueryWrapper);
                if (items == null) continue;
                for (OrderItem item : items) {
                    if (item.getProductId() != null && item.getQuantity() != null) {
                        Long qty = item.getQuantity() instanceof Integer
                            ? ((Integer) item.getQuantity()).longValue()
                            : item.getQuantity().longValue();
                        productQuantityMap.merge(item.getProductId(), qty, Long::sum);
                    }
                }
            }

            List<Product> products = productMapper.selectList(null);
            if (products == null) {
                products = new ArrayList<>();
            }
            Map<Long, String> productNameMap = products.stream()
                    .collect(Collectors.toMap(Product::getId, p -> p.getName() != null ? p.getName() : "未知产品"));

            List<Map<String, Object>> productSalesList = new ArrayList<>();
            for (Map.Entry<Long, Long> entry : productQuantityMap.entrySet()) {
                Map<String, Object> item = new HashMap<>();
                item.put("productName", productNameMap.getOrDefault(entry.getKey(), "未知产品"));
                item.put("quantity", entry.getValue());
                productSalesList.add(item);
            }
            productSalesList.sort((a, b) -> {
                Long qtyA = (Long) a.get("quantity");
                Long qtyB = (Long) b.get("quantity");
                if (qtyA == null) qtyA = 0L;
                if (qtyB == null) qtyB = 0L;
                return qtyB.compareTo(qtyA);
            });

            for (int i = 0; i < productSalesList.size(); i++) {
                Map<String, Object> productData = productSalesList.get(i);
                Row row = productSalesSheet.createRow(i + 1);
                row.createCell(0).setCellValue(productData.get("productName") != null ? (String) productData.get("productName") : "");
                row.createCell(1).setCellValue(productData.get("quantity") != null ? productData.get("quantity").toString() : "0");
                row.createCell(2).setCellValue(String.valueOf(i + 1));
            }

            System.out.println("开始生成Excel文件...");

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            workbook.write(baos);
            workbook.close();

            byte[] excelData = baos.toByteArray();
            System.out.println("Excel文件生成成功，大小: " + excelData.length + " bytes");

            String filename = "统计分析报表_" + currentYear + "年" + currentMonth + "月.xlsx";

            HttpHeaders respHeaders = new HttpHeaders();
            respHeaders.set("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            respHeaders.setContentDispositionFormData("attachment", new String(filename.getBytes("UTF-8"), "ISO-8859-1"));
            respHeaders.setContentLength(excelData.length);

            System.out.println("========== 导出报表成功 ==========");
            return ResponseEntity.ok()
                    .headers(respHeaders)
                    .body(excelData);
        } catch (Exception e) {
            System.out.println("========== 导出报表失败 ==========");
            e.printStackTrace();
            System.err.println("异常信息: " + e.getMessage());
            System.err.println("异常类型: " + e.getClass().getName());
            return ResponseEntity.internalServerError().build();
        }
    }
}
