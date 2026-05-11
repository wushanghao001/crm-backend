package com.example.crm.service;

import com.example.crm.dto.MenuResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MenuService {

    public List<MenuResponse> getMenus(String role) {
        List<MenuResponse> menus = new ArrayList<>();

        MenuResponse dashboard = new MenuResponse();
        dashboard.setName("首页");
        dashboard.setCode("dashboard");
        dashboard.setPath("/dashboard");
        dashboard.setIcon("Home");
        menus.add(dashboard);

        MenuResponse customerMenu = new MenuResponse();
        customerMenu.setName("客户管理");
        customerMenu.setCode("customer");
        customerMenu.setIcon("People");
        customerMenu.setType("menu");
        customerMenu.setSort(1);
        List<MenuResponse> customerChildren = new ArrayList<>();

        MenuResponse c1 = new MenuResponse();
        c1.setName("客户列表");
        c1.setCode("customer:view");
        c1.setPath("/dashboard/customers");
        c1.setParentId(1L);
        c1.setType("page");
        c1.setSort(1);
        customerChildren.add(c1);

        MenuResponse c2 = new MenuResponse();
        c2.setName("联系人");
        c2.setCode("contact:view");
        c2.setPath("/dashboard/contacts");
        c2.setParentId(1L);
        c2.setType("page");
        c2.setSort(2);
        customerChildren.add(c2);

        MenuResponse c3 = new MenuResponse();
        c3.setName("交互记录");
        c3.setCode("interaction:view");
        c3.setPath("/dashboard/interactions");
        c3.setParentId(1L);
        c3.setType("page");
        c3.setSort(3);
        customerChildren.add(c3);

        MenuResponse c4 = new MenuResponse();
        c4.setName("历史订单");
        c4.setCode("order:view");
        c4.setPath("/dashboard/orders");
        c4.setParentId(1L);
        c4.setType("page");
        c4.setSort(4);
        customerChildren.add(c4);

        if ("admin".equals(role)) {
            MenuResponse c5 = new MenuResponse();
            c5.setName("客户流失");
            c5.setCode("churn:view");
            c5.setPath("/dashboard/churn");
            c5.setParentId(1L);
            c5.setType("page");
            c5.setSort(5);
            customerChildren.add(c5);
        }

        customerMenu.setChildren(customerChildren);
        menus.add(customerMenu);

        MenuResponse performance = new MenuResponse();
        performance.setName("个人业绩");
        performance.setCode("performance:view");
        performance.setPath("/dashboard/my-performance");
        performance.setIcon("StatsChart");
        performance.setType("page");
        performance.setSort(1);
        menus.add(performance);

        MenuResponse opportunity = new MenuResponse();
        opportunity.setName("销售机会");
        opportunity.setCode("opportunity:view");
        opportunity.setPath("/dashboard/opportunities");
        opportunity.setIcon("Briefcase");
        opportunity.setType("page");
        opportunity.setSort(2);
        menus.add(opportunity);

        MenuResponse service = new MenuResponse();
        service.setName("服务管理");
        service.setCode("service:view");
        service.setPath("/dashboard/services");
        service.setIcon("Headset");
        service.setType("page");
        service.setSort(3);
        menus.add(service);

        MenuResponse product = new MenuResponse();
        product.setName("产品管理");
        product.setCode("product:view");
        product.setPath("/dashboard/products");
        product.setIcon("FileTray");
        product.setType("page");
        service.setSort(4);
        menus.add(product);

        if ("admin".equals(role)) {
            MenuResponse statistics = new MenuResponse();
            statistics.setName("统计分析");
            statistics.setCode("statistics:view");
            statistics.setPath("/dashboard/statistics");
            statistics.setIcon("BarChart");
            statistics.setType("page");
            statistics.setSort(5);
            menus.add(statistics);

            MenuResponse system = new MenuResponse();
            system.setName("系统管理");
            system.setCode("system");
            system.setIcon("Settings");
            system.setType("menu");
            system.setSort(6);
            List<MenuResponse> systemChildren = new ArrayList<>();

            MenuResponse s1 = new MenuResponse();
            s1.setName("用户管理");
            s1.setCode("user:view");
            s1.setPath("/dashboard/users");
            s1.setParentId(6L);
            s1.setType("page");
            s1.setSort(1);
            systemChildren.add(s1);

            MenuResponse s2 = new MenuResponse();
            s2.setName("权限管理");
            s2.setCode("role:view");
            s2.setPath("/dashboard/roles");
            s2.setParentId(6L);
            s2.setType("page");
            s2.setSort(2);
            systemChildren.add(s2);

            MenuResponse s3 = new MenuResponse();
            s3.setName("日志管理");
            s3.setCode("log:view");
            s3.setPath("/dashboard/logs");
            s3.setParentId(6L);
            s3.setType("page");
            s3.setSort(3);
            systemChildren.add(s3);

            system.setChildren(systemChildren);
            menus.add(system);
        }

        return menus;
    }

    public List<MenuResponse> getPermissionTree() {
        List<MenuResponse> tree = new ArrayList<>();

        MenuResponse dashboardMenu = new MenuResponse();
        dashboardMenu.setId(0L);
        dashboardMenu.setName("首页");
        dashboardMenu.setCode("dashboard");
        dashboardMenu.setType("menu");
        dashboardMenu.setSort(0);
        dashboardMenu.setParentId(0L);

        List<MenuResponse> dashboardChildren = new ArrayList<>();
        dashboardChildren.add(createPermission(1L, "首页", "dashboard:view", "page", 0L));
        dashboardChildren.add(createPermission(2L, "管理首页", "admin:dashboard", "page", 0L));
        dashboardMenu.setChildren(dashboardChildren);
        tree.add(dashboardMenu);

        MenuResponse customerMenu = new MenuResponse();
        customerMenu.setId(1L);
        customerMenu.setName("客户管理");
        customerMenu.setCode("customer");
        customerMenu.setType("menu");
        customerMenu.setSort(1);
        customerMenu.setParentId(0L);

        List<MenuResponse> customerChildren = new ArrayList<>();
        customerChildren.add(createPermission(101L, "客户列表", "customer:view", "page", 1L));
        customerChildren.add(createPermission(102L, "客户新增", "customer:add", "button", 1L));
        customerChildren.add(createPermission(103L, "客户编辑", "customer:edit", "button", 1L));
        customerChildren.add(createPermission(104L, "客户删除", "customer:delete", "button", 1L));
        customerChildren.add(createPermission(105L, "联系人列表", "contact:view", "page", 1L));
        customerChildren.add(createPermission(106L, "联系人新增", "contact:add", "button", 1L));
        customerChildren.add(createPermission(107L, "联系人编辑", "contact:edit", "button", 1L));
        customerChildren.add(createPermission(108L, "联系人删除", "contact:delete", "button", 1L));
        customerChildren.add(createPermission(109L, "交互记录列表", "interaction:view", "page", 1L));
        customerChildren.add(createPermission(110L, "交互记录新增", "interaction:add", "button", 1L));
        customerChildren.add(createPermission(111L, "交互记录编辑", "interaction:edit", "button", 1L));
        customerChildren.add(createPermission(112L, "交互记录删除", "interaction:delete", "button", 1L));
        customerChildren.add(createPermission(113L, "订单列表", "order:view", "page", 1L));
        customerChildren.add(createPermission(114L, "订单新增", "order:add", "button", 1L));
        customerChildren.add(createPermission(115L, "订单编辑", "order:edit", "button", 1L));
        customerChildren.add(createPermission(116L, "订单删除", "order:delete", "button", 1L));
        customerChildren.add(createPermission(117L, "客户跟进查询", "follow:view", "page", 1L));
        customerChildren.add(createPermission(118L, "客户流失", "churn:view", "page", 1L));
        customerMenu.setChildren(customerChildren);
        tree.add(customerMenu);

        MenuResponse performanceMenu = new MenuResponse();
        performanceMenu.setId(10L);
        performanceMenu.setName("个人业绩");
        performanceMenu.setCode("performance");
        performanceMenu.setType("menu");
        performanceMenu.setSort(10);
        performanceMenu.setParentId(0L);

        List<MenuResponse> performanceChildren = new ArrayList<>();
        performanceChildren.add(createPermission(1001L, "查看业绩", "performance:view", "page", 10L));
        performanceChildren.add(createPermission(1002L, "设置目标", "performance:setTarget", "button", 10L));
        performanceMenu.setChildren(performanceChildren);
        tree.add(performanceMenu);

        MenuResponse opportunityMenu = new MenuResponse();
        opportunityMenu.setId(2L);
        opportunityMenu.setName("销售机会");
        opportunityMenu.setCode("opportunity");
        opportunityMenu.setType("menu");
        opportunityMenu.setSort(2);
        opportunityMenu.setParentId(0L);

        List<MenuResponse> opportunityChildren = new ArrayList<>();
        opportunityChildren.add(createPermission(201L, "机会列表", "opportunity:view", "page", 2L));
        opportunityChildren.add(createPermission(202L, "机会新增", "opportunity:add", "button", 2L));
        opportunityChildren.add(createPermission(203L, "机会编辑", "opportunity:edit", "button", 2L));
        opportunityChildren.add(createPermission(204L, "机会删除", "opportunity:delete", "button", 2L));
        opportunityMenu.setChildren(opportunityChildren);
        tree.add(opportunityMenu);

        MenuResponse serviceMenu = new MenuResponse();
        serviceMenu.setId(3L);
        serviceMenu.setName("服务管理");
        serviceMenu.setCode("service");
        serviceMenu.setType("menu");
        serviceMenu.setSort(3);
        serviceMenu.setParentId(0L);

        List<MenuResponse> serviceChildren = new ArrayList<>();
        serviceChildren.add(createPermission(301L, "服务列表", "service:view", "page", 3L));
        serviceChildren.add(createPermission(302L, "服务新增", "service:add", "button", 3L));
        serviceChildren.add(createPermission(303L, "服务编辑", "service:edit", "button", 3L));
        serviceChildren.add(createPermission(304L, "服务删除", "service:delete", "button", 3L));
        serviceChildren.add(createPermission(305L, "服务处理", "service:handle", "button", 3L));
        serviceMenu.setChildren(serviceChildren);
        tree.add(serviceMenu);

        MenuResponse productMenu = new MenuResponse();
        productMenu.setId(4L);
        productMenu.setName("产品管理");
        productMenu.setCode("product");
        productMenu.setType("menu");
        productMenu.setSort(4);
        productMenu.setParentId(0L);

        List<MenuResponse> productChildren = new ArrayList<>();
        productChildren.add(createPermission(401L, "产品列表", "product:view", "page", 4L));
        productChildren.add(createPermission(402L, "产品新增", "product:add", "button", 4L));
        productChildren.add(createPermission(403L, "产品编辑", "product:edit", "button", 4L));
        productChildren.add(createPermission(404L, "产品删除", "product:delete", "button", 4L));
        productChildren.add(createPermission(405L, "项目号档案维护", "projectcode:view", "page", 4L));
        productChildren.add(createPermission(406L, "项目号新增", "projectcode:add", "button", 4L));
        productChildren.add(createPermission(407L, "项目号编辑", "projectcode:edit", "button", 4L));
        productChildren.add(createPermission(408L, "项目号删除", "projectcode:delete", "button", 4L));
        productChildren.add(createPermission(409L, "料号档案维护", "materialcode:view", "page", 4L));
        productChildren.add(createPermission(410L, "料号新增", "materialcode:add", "button", 4L));
        productChildren.add(createPermission(411L, "料号编辑", "materialcode:edit", "button", 4L));
        productChildren.add(createPermission(412L, "料号删除", "materialcode:delete", "button", 4L));
        productChildren.add(createPermission(413L, "牌号档案维护", "brandcode:view", "page", 4L));
        productChildren.add(createPermission(414L, "牌号新增", "brandcode:add", "button", 4L));
        productChildren.add(createPermission(415L, "牌号编辑", "brandcode:edit", "button", 4L));
        productChildren.add(createPermission(416L, "牌号删除", "brandcode:delete", "button", 4L));
        productMenu.setChildren(productChildren);
        tree.add(productMenu);

        MenuResponse statisticsMenu = new MenuResponse();
        statisticsMenu.setId(5L);
        statisticsMenu.setName("统计分析");
        statisticsMenu.setCode("statistics");
        statisticsMenu.setType("menu");
        statisticsMenu.setSort(5);
        statisticsMenu.setParentId(0L);

        List<MenuResponse> statisticsChildren = new ArrayList<>();
        statisticsChildren.add(createPermission(501L, "数据统计", "statistics:view", "page", 5L));
        statisticsMenu.setChildren(statisticsChildren);
        tree.add(statisticsMenu);

        MenuResponse systemMenu = new MenuResponse();
        systemMenu.setId(6L);
        systemMenu.setName("系统管理");
        systemMenu.setCode("system");
        systemMenu.setType("menu");
        systemMenu.setSort(6);
        systemMenu.setParentId(0L);

        List<MenuResponse> systemChildren = new ArrayList<>();
        systemChildren.add(createPermission(601L, "用户管理", "user:view", "page", 6L));
        systemChildren.add(createPermission(602L, "用户新增", "user:add", "button", 6L));
        systemChildren.add(createPermission(603L, "用户编辑", "user:edit", "button", 6L));
        systemChildren.add(createPermission(604L, "用户删除", "user:delete", "button", 6L));
        systemChildren.add(createPermission(605L, "角色管理", "role:view", "page", 6L));
        systemChildren.add(createPermission(606L, "角色新增", "role:add", "button", 6L));
        systemChildren.add(createPermission(607L, "角色编辑", "role:edit", "button", 6L));
        systemChildren.add(createPermission(608L, "角色删除", "role:delete", "button", 6L));
        systemChildren.add(createPermission(609L, "日志管理", "log:view", "page", 6L));
        systemMenu.setChildren(systemChildren);
        tree.add(systemMenu);

        return tree;
    }

    private MenuResponse createPermission(Long id, String name, String code, String type, Long parentId) {
        MenuResponse permission = new MenuResponse();
        permission.setId(id);
        permission.setName(name);
        permission.setCode(code);
        permission.setType(type);
        permission.setParentId(parentId);
        return permission;
    }
}