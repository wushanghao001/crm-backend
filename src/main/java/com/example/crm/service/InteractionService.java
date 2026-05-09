package com.example.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.crm.dto.InteractionDTO;
import com.example.crm.dto.PageResult;
import com.example.crm.entity.Interaction;
import com.example.crm.entity.Customer;
import com.example.crm.entity.Contact;
import com.example.crm.entity.User;
import com.example.crm.mapper.InteractionMapper;
import com.example.crm.mapper.CustomerMapper;
import com.example.crm.mapper.ContactMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InteractionService {

    private final InteractionMapper interactionMapper;
    private final CustomerMapper customerMapper;
    private final ContactMapper contactMapper;

    public InteractionService(InteractionMapper interactionMapper, CustomerMapper customerMapper, ContactMapper contactMapper) {
        this.interactionMapper = interactionMapper;
        this.customerMapper = customerMapper;
        this.contactMapper = contactMapper;
    }

    public PageResult<InteractionDTO> getInteractions(Integer page, Integer size, String keyword, String type, String operator, String status, Long customerId) {
        Page<Interaction> pageParam = new Page<>(page, size);
        QueryWrapper<Interaction> queryWrapper = new QueryWrapper<>();

        User currentUser = getCurrentUser();
        boolean isAdmin = "admin".equals(currentUser.getRole());

        if (!isAdmin) {
            queryWrapper.eq("creator_id", currentUser.getId());
        }

        if (customerId != null) {
            queryWrapper.eq("customer_id", customerId);
        }

        if (type != null && !type.isEmpty()) {
            queryWrapper.eq("type", type);
        }
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq("status", status);
        }
        if (operator != null && !operator.isEmpty()) {
            queryWrapper.eq("operator", operator);
        }

        queryWrapper.orderByDesc("interaction_time");

        Page<Interaction> result = interactionMapper.selectPage(pageParam, queryWrapper);

        List<InteractionDTO> dtoList = result.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        if (keyword != null && !keyword.isEmpty()) {
            dtoList = dtoList.stream()
                    .filter(dto -> {
                        boolean match = false;
                        if (dto.getCustomerName() != null) {
                            match = dto.getCustomerName().contains(keyword);
                        }
                        if (!match && dto.getContactName() != null) {
                            match = dto.getContactName().contains(keyword);
                        }
                        if (!match && dto.getContent() != null) {
                            match = dto.getContent().contains(keyword);
                        }
                        return match;
                    })
                    .collect(Collectors.toList());
        }

        return new PageResult<>(dtoList, result.getTotal(), page, size);
    }

    public InteractionDTO getById(Long id) {
        Interaction interaction = interactionMapper.selectById(id);
        if (interaction == null) {
            return null;
        }

        User currentUser = getCurrentUser();
        if (!"admin".equals(currentUser.getRole())) {
            Customer customer = customerMapper.selectById(interaction.getCustomerId());
            if (customer == null || !customer.getCreatorId().equals(currentUser.getId())) {
                throw new IllegalArgumentException("无权访问此交互记录");
            }
        }

        return convertToDTO(interaction);
    }

    public List<Interaction> getByCustomerId(Long customerId) {
        User currentUser = getCurrentUser();
        boolean isAdmin = "admin".equals(currentUser.getRole());

        if (!isAdmin) {
            Customer customer = customerMapper.selectById(customerId);
            if (customer == null || !customer.getCreatorId().equals(currentUser.getId())) {
                throw new IllegalArgumentException("无权访问此客户");
            }
        }

        return interactionMapper.findByCustomerId(customerId);
    }

    public Interaction createInteraction(Interaction interaction) {
        User currentUser = getCurrentUser();
        boolean isAdmin = "admin".equals(currentUser.getRole());

        if (!isAdmin) {
            Customer customer = customerMapper.selectById(interaction.getCustomerId());
            if (customer == null || !customer.getCreatorId().equals(currentUser.getId())) {
                throw new IllegalArgumentException("无权为此客户创建交互记录");
            }
        }

        if (interaction.getInteractionTime() == null) {
            interaction.setInteractionTime(LocalDateTime.now());
        }
        interaction.setCreatedAt(LocalDateTime.now());

        interaction.setOperatorId(currentUser.getId());
        if (interaction.getOperator() == null || interaction.getOperator().isEmpty()) {
            interaction.setOperator(currentUser.getUsername());
        }

        interaction.setCreatorId(currentUser.getId());
        interactionMapper.insert(interaction);
        return interaction;
    }

    public Interaction updateInteraction(Long id, Interaction interaction) {
        Interaction existing = interactionMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("交互记录不存在");
        }

        User currentUser = getCurrentUser();
        if (!"admin".equals(currentUser.getRole())) {
            if (!existing.getOperatorId().equals(currentUser.getId())) {
                throw new IllegalArgumentException("无权修改此交互记录");
            }
        }

        interaction.setId(id);
        interactionMapper.updateById(interaction);
        return interactionMapper.selectById(id);
    }

    public void deleteInteraction(Long id) {
        Interaction interaction = interactionMapper.selectById(id);
        if (interaction == null) {
            throw new IllegalArgumentException("交互记录不存在");
        }

        User currentUser = getCurrentUser();
        if (!"admin".equals(currentUser.getRole()) && !currentUser.getId().equals(interaction.getCreatorId())) {
            throw new IllegalArgumentException("只能删除自己创建的交互记录");
        }

        interactionMapper.deleteById(id);
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        throw new IllegalArgumentException("用户未登录");
    }

    private InteractionDTO convertToDTO(Interaction interaction) {
        InteractionDTO dto = new InteractionDTO();
        dto.setId(interaction.getId());
        dto.setCustomerId(interaction.getCustomerId());
        dto.setContactId(interaction.getContactId());
        dto.setType(interaction.getType());
        dto.setContent(interaction.getContent());
        dto.setInteractionTime(interaction.getInteractionTime());
        dto.setOperator(interaction.getOperator());
        dto.setStatus(interaction.getStatus());
        dto.setPhone(interaction.getPhone());
        dto.setEmail(interaction.getEmail());
        dto.setCreatedAt(interaction.getCreatedAt());

        if (interaction.getCustomerId() != null) {
            Customer customer = customerMapper.selectById(interaction.getCustomerId());
            if (customer != null) {
                dto.setCustomerName(customer.getName());
            }
        }

        if (interaction.getContactId() != null) {
            Contact contact = contactMapper.selectById(interaction.getContactId());
            if (contact != null) {
                dto.setContactName(contact.getName());
            }
        }

        return dto;
    }
}