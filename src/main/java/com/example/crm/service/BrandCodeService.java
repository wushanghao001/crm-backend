package com.example.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.crm.dto.PageResponse;
import com.example.crm.entity.BrandCode;
import com.example.crm.mapper.BrandCodeMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class BrandCodeService {

    private final BrandCodeMapper brandCodeMapper;

    public BrandCodeService(BrandCodeMapper brandCodeMapper) {
        this.brandCodeMapper = brandCodeMapper;
    }

    public PageResponse<BrandCode> listBrandCodes(Integer pageNum, Integer pageSize, String keyword) {
        Page<BrandCode> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BrandCode> queryWrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.like(BrandCode::getName, keyword)
                       .or()
                       .like(BrandCode::getCode, keyword);
        }

        queryWrapper.orderByDesc(BrandCode::getCreatedAt);
        IPage<BrandCode> result = brandCodeMapper.selectPage(page, queryWrapper);

        return new PageResponse<>(result.getRecords(), result.getTotal(), pageNum, pageSize);
    }

    public BrandCode getBrandCodeById(Long id) {
        return brandCodeMapper.selectById(id);
    }

    public BrandCode createBrandCode(BrandCode brandCode) {
        brandCode.setCreatedAt(LocalDateTime.now());
        brandCode.setUpdatedAt(LocalDateTime.now());
        brandCodeMapper.insert(brandCode);
        return brandCode;
    }

    public BrandCode updateBrandCode(Long id, BrandCode brandCode) {
        BrandCode existing = brandCodeMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("牌号不存在");
        }

        existing.setName(brandCode.getName());
        existing.setCode(brandCode.getCode());
        existing.setStatus(brandCode.getStatus());
        existing.setUpdatedAt(LocalDateTime.now());

        brandCodeMapper.updateById(existing);
        return existing;
    }

    public void deleteBrandCode(Long id) {
        brandCodeMapper.deleteById(id);
    }

    public void batchDeleteBrandCodes(Long[] ids) {
        for (Long id : ids) {
            brandCodeMapper.deleteById(id);
        }
    }

    public boolean checkCodeExists(String code, Long excludeId) {
        LambdaQueryWrapper<BrandCode> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BrandCode::getCode, code);

        if (excludeId != null) {
            queryWrapper.ne(BrandCode::getId, excludeId);
        }

        return brandCodeMapper.exists(queryWrapper);
    }
}