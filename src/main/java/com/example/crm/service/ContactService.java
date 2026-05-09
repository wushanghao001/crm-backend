package com.example.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.crm.dto.PageResponse;
import com.example.crm.entity.Contact;
import com.example.crm.entity.Customer;
import com.example.crm.entity.User;
import com.example.crm.mapper.ContactMapper;
import com.example.crm.mapper.CustomerMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class ContactService {

    private final ContactMapper contactMapper;
    private final CustomerMapper customerMapper;

    public ContactService(ContactMapper contactMapper, CustomerMapper customerMapper) {
        this.contactMapper = contactMapper;
        this.customerMapper = customerMapper;
    }

    public PageResponse<Contact> listContacts(Integer pageNum, Integer pageSize, String keyword, Long customerId) {
        Page<Contact> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Contact> queryWrapper = new LambdaQueryWrapper<>();

        User currentUser = getCurrentUser();
        boolean isAdmin = "admin".equals(currentUser.getRole());

        if (!isAdmin) {
            queryWrapper.eq(Contact::getCreatorId, currentUser.getId());
        }

        if (customerId != null) {
            queryWrapper.eq(Contact::getCustomerId, customerId);
        }

        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.and(wrapper -> wrapper
                .like(Contact::getName, keyword)
                .or()
                .like(Contact::getPhone, keyword));
        }

        queryWrapper.orderByDesc(Contact::getCreatedAt);
        IPage<Contact> result = contactMapper.selectPage(page, queryWrapper);

        return new PageResponse<>(result.getRecords(), result.getTotal(), pageNum, pageSize);
    }

    public Contact getContactById(Long id) {
        Contact contact = contactMapper.selectById(id);
        if (contact == null) {
            throw new IllegalArgumentException("联系人不存在");
        }

        User currentUser = getCurrentUser();
        if (!"admin".equals(currentUser.getRole())) {
            Customer customer = customerMapper.selectById(contact.getCustomerId());
            if (customer == null || !customer.getCreatorId().equals(currentUser.getId())) {
                throw new IllegalArgumentException("无权访问此联系人");
            }
        }

        return contact;
    }

    public PageResponse<Contact> getByCustomerId(Long customerId, Integer pageNum, Integer pageSize) {
        User currentUser = getCurrentUser();
        boolean isAdmin = "admin".equals(currentUser.getRole());

        if (!isAdmin) {
            Customer customer = customerMapper.selectById(customerId);
            if (customer == null || !customer.getCreatorId().equals(currentUser.getId())) {
                return new PageResponse<>(java.util.Collections.emptyList(), 0L, pageNum, pageSize);
            }
        }

        Page<Contact> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Contact> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Contact::getCustomerId, customerId);
        queryWrapper.orderByDesc(Contact::getCreatedAt);

        IPage<Contact> result = contactMapper.selectPage(page, queryWrapper);

        return new PageResponse<>(result.getRecords(), result.getTotal(), pageNum, pageSize);
    }

    public Contact createContact(Contact contact) {
        User currentUser = getCurrentUser();
        boolean isAdmin = "admin".equals(currentUser.getRole());

        if (!isAdmin) {
            Customer customer = customerMapper.selectById(contact.getCustomerId());
            if (customer == null || !customer.getCreatorId().equals(currentUser.getId())) {
                throw new IllegalArgumentException("无权为此客户创建联系人");
            }
        }

        contact.setCreatorId(currentUser.getId());
        contactMapper.insert(contact);
        return contact;
    }

    public Contact updateContact(Long id, Contact contact) {
        Contact existing = contactMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("联系人不存在");
        }

        User currentUser = getCurrentUser();
        if (!"admin".equals(currentUser.getRole())) {
            Customer customer = customerMapper.selectById(existing.getCustomerId());
            if (customer == null || !customer.getCreatorId().equals(currentUser.getId())) {
                throw new IllegalArgumentException("无权修改此联系人");
            }
        }

        contact.setId(id);
        contactMapper.updateById(contact);
        return contact;
    }

    public void deleteContact(Long id) {
        Contact contact = contactMapper.selectById(id);
        if (contact == null) {
            throw new IllegalArgumentException("联系人不存在");
        }

        User currentUser = getCurrentUser();
        if (!"admin".equals(currentUser.getRole()) && !currentUser.getId().equals(contact.getCreatorId())) {
            throw new IllegalArgumentException("只能删除自己创建的联系人");
        }

        contactMapper.deleteById(id);
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        throw new IllegalArgumentException("用户未登录");
    }
}