package com.example.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.crm.dto.CustomerRequest;
import com.example.crm.dto.PageResponse;
import com.example.crm.entity.Customer;
import com.example.crm.entity.User;
import com.example.crm.mapper.CustomerMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CustomerService {

    private final CustomerMapper customerMapper;

    public CustomerService(CustomerMapper customerMapper) {
        this.customerMapper = customerMapper;
    }

    public PageResponse<Customer> listCustomers(Integer pageNum, Integer pageSize, String keyword, String status, String industry, String listType) {
        Page<Customer> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Customer> queryWrapper = new LambdaQueryWrapper<>();

        User currentUser = getCurrentUser();
        boolean isAdmin = "admin".equals(currentUser.getRole());

        if (!isAdmin) {
            if ("public".equals(listType)) {
                queryWrapper.isNull(Customer::getCreatorId);
            } else {
                queryWrapper.eq(Customer::getCreatorId, currentUser.getId());
            }
        }

        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.and(wrapper -> wrapper
                .like(Customer::getName, keyword)
                .or()
                .like(Customer::getPhone, keyword)
                .or()
                .like(Customer::getEmail, keyword));
        }
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq(Customer::getStatus, status);
        }
        if (industry != null && !industry.isEmpty()) {
            queryWrapper.eq(Customer::getIndustry, industry);
        }

        queryWrapper.orderByDesc(Customer::getCreatedAt);

        IPage<Customer> result = customerMapper.selectPage(page, queryWrapper);

        return new PageResponse<>(result.getRecords(), result.getTotal(), pageNum, pageSize);
    }

    public Customer getCustomerById(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在");
        }

        User currentUser = getCurrentUser();
        if (!"admin".equals(currentUser.getRole())) {
            if (!customer.getCreatorId().equals(currentUser.getId()) && customer.getCreatorId() != null) {
                throw new IllegalArgumentException("无权访问此客户");
            }
        }

        return customer;
    }

    public Customer createCustomer(CustomerRequest request) {
        Customer customer = new Customer();
        customer.setName(request.getName());
        customer.setPhone(request.getPhone());
        customer.setEmail(request.getEmail());
        customer.setAddress(request.getAddress());
        customer.setIndustry(request.getIndustry());
        customer.setScale(request.getScale());
        customer.setSource(request.getSource());
        customer.setStatus(request.getStatus() != null ? request.getStatus() : "active");
        if ("churned".equals(request.getStatus()) && request.getChurnReason() != null) {
            customer.setChurnReason(request.getChurnReason());
        }
        customer.setCustomerLevel(request.getCustomerLevel() != null ? request.getCustomerLevel() : "C");
        customer.setTotalAmount(request.getTotalAmount());
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());

        User currentUser = getCurrentUser();
        boolean isAdmin = "admin".equals(currentUser.getRole());
        if (isAdmin) {
            customer.setCreatorId(null);
        } else {
            customer.setCreatorId(currentUser.getId());
        }

        customerMapper.insert(customer);
        return customer;
    }

    public Customer updateCustomer(Long id, CustomerRequest request) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在");
        }

        User currentUser = getCurrentUser();
        if (!"admin".equals(currentUser.getRole())) {
            if (!customer.getCreatorId().equals(currentUser.getId())) {
                throw new IllegalArgumentException("无权修改此客户");
            }
        }

        if (request.getName() != null) {
            customer.setName(request.getName());
        }
        if (request.getPhone() != null) {
            customer.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            customer.setEmail(request.getEmail());
        }
        if (request.getAddress() != null) {
            customer.setAddress(request.getAddress());
        }
        if (request.getIndustry() != null) {
            customer.setIndustry(request.getIndustry());
        }
        if (request.getScale() != null) {
            customer.setScale(request.getScale());
        }
        if (request.getSource() != null) {
            customer.setSource(request.getSource());
        }
        if (request.getStatus() != null) {
            customer.setStatus(request.getStatus());
            if ("churned".equals(request.getStatus()) && request.getChurnReason() != null) {
                customer.setChurnReason(request.getChurnReason());
            }
        }
        if (request.getCustomerLevel() != null) {
            customer.setCustomerLevel(request.getCustomerLevel());
        }
        if (request.getTotalAmount() != null) {
            customer.setTotalAmount(request.getTotalAmount());
        }
        customer.setUpdatedAt(LocalDateTime.now());

        customerMapper.updateById(customer);
        return customer;
    }

    public void deleteCustomer(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在");
        }

        User currentUser = getCurrentUser();
        if (!"admin".equals(currentUser.getRole())) {
            throw new IllegalArgumentException("无权删除客户");
        }

        customerMapper.deleteById(id);
    }

    public Customer claimCustomer(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在");
        }

        if (customer.getCreatorId() != null) {
            throw new IllegalArgumentException("该客户已被认领");
        }

        User currentUser = getCurrentUser();
        customer.setCreatorId(currentUser.getId());
        customer.setUpdatedAt(LocalDateTime.now());

        customerMapper.updateById(customer);
        return customer;
    }

    public List<Customer> getPublicCustomers() {
        LambdaQueryWrapper<Customer> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.isNull(Customer::getCreatorId);
        queryWrapper.orderByDesc(Customer::getCreatedAt);
        return customerMapper.selectList(queryWrapper);
    }

    public Long countMyCustomers() {
        User currentUser = getCurrentUser();
        LambdaQueryWrapper<Customer> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Customer::getCreatorId, currentUser.getId());
        return customerMapper.selectCount(queryWrapper);
    }

    public Long countPublicCustomers() {
        LambdaQueryWrapper<Customer> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.isNull(Customer::getCreatorId);
        return customerMapper.selectCount(queryWrapper);
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        throw new IllegalArgumentException("用户未登录");
    }
}