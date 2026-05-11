package com.example.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.crm.dto.PageResponse;
import com.example.crm.entity.ProjectCode;
import com.example.crm.mapper.ProjectCodeMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ProjectCodeService {

    private final ProjectCodeMapper projectCodeMapper;

    public ProjectCodeService(ProjectCodeMapper projectCodeMapper) {
        this.projectCodeMapper = projectCodeMapper;
    }

    public PageResponse<ProjectCode> listProjectCodes(Integer pageNum, Integer pageSize, String keyword) {
        Page<ProjectCode> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ProjectCode> queryWrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.like(ProjectCode::getName, keyword)
                       .or()
                       .like(ProjectCode::getCode, keyword);
        }

        queryWrapper.orderByDesc(ProjectCode::getCreatedAt);
        IPage<ProjectCode> result = projectCodeMapper.selectPage(page, queryWrapper);

        return new PageResponse<>(result.getRecords(), result.getTotal(), pageNum, pageSize);
    }

    public ProjectCode getProjectCodeById(Long id) {
        return projectCodeMapper.selectById(id);
    }

    public ProjectCode createProjectCode(ProjectCode projectCode) {
        projectCode.setCreatedAt(LocalDateTime.now());
        projectCode.setUpdatedAt(LocalDateTime.now());
        projectCodeMapper.insert(projectCode);
        return projectCode;
    }

    public ProjectCode updateProjectCode(Long id, ProjectCode projectCode) {
        ProjectCode existing = projectCodeMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("项目号不存在");
        }

        existing.setName(projectCode.getName());
        existing.setCode(projectCode.getCode());
        existing.setStatus(projectCode.getStatus());
        existing.setUpdatedAt(LocalDateTime.now());

        projectCodeMapper.updateById(existing);
        return existing;
    }

    public void deleteProjectCode(Long id) {
        projectCodeMapper.deleteById(id);
    }

    public void batchDeleteProjectCodes(Long[] ids) {
        for (Long id : ids) {
            projectCodeMapper.deleteById(id);
        }
    }

    public boolean checkCodeExists(String code, Long excludeId) {
        LambdaQueryWrapper<ProjectCode> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProjectCode::getCode, code);

        if (excludeId != null) {
            queryWrapper.ne(ProjectCode::getId, excludeId);
        }

        return projectCodeMapper.exists(queryWrapper);
    }
}