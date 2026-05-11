package com.example.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.crm.dto.PageResponse;
import com.example.crm.entity.MaterialCode;
import com.example.crm.mapper.MaterialCodeMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MaterialCodeService {

    private final MaterialCodeMapper materialCodeMapper;

    public MaterialCodeService(MaterialCodeMapper materialCodeMapper) {
        this.materialCodeMapper = materialCodeMapper;
    }

    public PageResponse<MaterialCode> listMaterialCodes(Integer pageNum, Integer pageSize, String keyword) {
        Page<MaterialCode> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MaterialCode> queryWrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.like(MaterialCode::getName, keyword)
                       .or()
                       .like(MaterialCode::getCode, keyword);
        }

        queryWrapper.orderByDesc(MaterialCode::getCreatedAt);
        IPage<MaterialCode> result = materialCodeMapper.selectPage(page, queryWrapper);

        return new PageResponse<>(result.getRecords(), result.getTotal(), pageNum, pageSize);
    }

    public MaterialCode getMaterialCodeById(Long id) {
        return materialCodeMapper.selectById(id);
    }

    public MaterialCode createMaterialCode(MaterialCode materialCode) {
        materialCode.setCreatedAt(LocalDateTime.now());
        materialCode.setUpdatedAt(LocalDateTime.now());
        materialCodeMapper.insert(materialCode);
        return materialCode;
    }

    public MaterialCode updateMaterialCode(Long id, MaterialCode materialCode) {
        MaterialCode existing = materialCodeMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("料号不存在");
        }

        existing.setName(materialCode.getName());
        existing.setCode(materialCode.getCode());
        existing.setStatus(materialCode.getStatus());
        existing.setUpdatedAt(LocalDateTime.now());

        materialCodeMapper.updateById(existing);
        return existing;
    }

    public void deleteMaterialCode(Long id) {
        materialCodeMapper.deleteById(id);
    }

    public void batchDeleteMaterialCodes(Long[] ids) {
        for (Long id : ids) {
            materialCodeMapper.deleteById(id);
        }
    }

    public boolean checkCodeExists(String code, Long excludeId) {
        LambdaQueryWrapper<MaterialCode> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MaterialCode::getCode, code);

        if (excludeId != null) {
            queryWrapper.ne(MaterialCode::getId, excludeId);
        }

        return materialCodeMapper.exists(queryWrapper);
    }
}