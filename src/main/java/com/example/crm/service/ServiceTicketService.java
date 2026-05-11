
package com.example.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.crm.dto.PageResponse;
import com.example.crm.entity.ServiceTicket;
import com.example.crm.entity.User;
import com.example.crm.mapper.ServiceTicketMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ServiceTicketService {

    private final ServiceTicketMapper serviceTicketMapper;

    public ServiceTicketService(ServiceTicketMapper serviceTicketMapper) {
        this.serviceTicketMapper = serviceTicketMapper;
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        throw new IllegalStateException("无法获取当前用户信息");
    }

    public PageResponse<ServiceTicket> listTickets(Integer pageNum, Integer pageSize, String keyword, String status, String priority) {
        Page<ServiceTicket> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ServiceTicket> queryWrapper = new LambdaQueryWrapper<>();

        User currentUser = getCurrentUser();
        boolean isAdmin = "admin".equals(currentUser.getRole());

        if (!isAdmin) {
            queryWrapper.eq(ServiceTicket::getAssigneeId, currentUser.getId());
        }

        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.like(ServiceTicket::getTitle, keyword);
        }
        if (status != null && !status.isEmpty()) {
            if (status.contains(",")) {
                queryWrapper.in(ServiceTicket::getStatus, status.split(","));
            } else {
                queryWrapper.eq(ServiceTicket::getStatus, status);
            }
        }
        if (priority != null && !priority.isEmpty()) {
            queryWrapper.eq(ServiceTicket::getPriority, priority);
        }

        queryWrapper.orderByDesc(ServiceTicket::getCreatedAt);
        IPage<ServiceTicket> result = serviceTicketMapper.selectPage(page, queryWrapper);

        return new PageResponse<>(result.getRecords(), result.getTotal(), pageNum, pageSize);
    }

    public ServiceTicket getTicketById(Long id) {
        ServiceTicket ticket = serviceTicketMapper.selectById(id);
        if (ticket == null) {
            throw new IllegalArgumentException("服务工单不存在");
        }

        User currentUser = getCurrentUser();
        if (!"admin".equals(currentUser.getRole()) && !ticket.getAssigneeId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("无权访问此工单");
        }

        return ticket;
    }

    public ServiceTicket createTicket(ServiceTicket ticket) {
        User currentUser = getCurrentUser();
        if (ticket.getAssigneeId() == null) {
            ticket.setAssigneeId(currentUser.getId());
        }
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());

        serviceTicketMapper.insert(ticket);
        return ticket;
    }

    public ServiceTicket updateTicket(Long id, ServiceTicket ticket) {
        ServiceTicket existing = serviceTicketMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("服务工单不存在");
        }

        User currentUser = getCurrentUser();
        if (!"admin".equals(currentUser.getRole()) && !existing.getAssigneeId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("无权修改此工单");
        }

        existing.setTitle(ticket.getTitle());
        existing.setDescription(ticket.getDescription());
        existing.setPriority(ticket.getPriority());
        existing.setStatus(ticket.getStatus());
        existing.setAssigneeId(ticket.getAssigneeId());
        existing.setUpdatedAt(LocalDateTime.now());

        serviceTicketMapper.updateById(existing);
        return existing;
    }

    public void deleteTicket(Long id) {
        ServiceTicket existing = serviceTicketMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("服务工单不存在");
        }

        User currentUser = getCurrentUser();
        if (!"admin".equals(currentUser.getRole()) && !existing.getAssigneeId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("无权删除此工单");
        }

        serviceTicketMapper.deleteById(id);
    }

    public void batchDeleteTickets(List<Long> ids) {
        User currentUser = getCurrentUser();
        boolean isAdmin = "admin".equals(currentUser.getRole());

        for (Long id : ids) {
            ServiceTicket existing = serviceTicketMapper.selectById(id);
            if (existing != null) {
                if (!isAdmin && !existing.getAssigneeId().equals(currentUser.getId())) {
                    throw new IllegalArgumentException("无权删除服务工单: " + existing.getTitle());
                }
            }
        }

        serviceTicketMapper.deleteBatchIds(ids);
    }
}
