package com.example.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.crm.dto.PageResponse;
import com.example.crm.entity.Product;
import com.example.crm.mapper.ProductMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ProductService {

    private final ProductMapper productMapper;

    public ProductService(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    public PageResponse<Product> listProducts(Integer pageNum, Integer pageSize, String keyword, String category, Integer status) {
        Page<Product> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        
        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.like(Product::getName, keyword)
                       .or()
                       .like(Product::getCode, keyword);
        }
        if (category != null && !category.isEmpty()) {
            queryWrapper.eq(Product::getCategory, category);
        }
        if (status != null) {
            queryWrapper.eq(Product::getStatus, status);
        }
        
        IPage<Product> result = productMapper.selectPage(page, queryWrapper);
        
        return new PageResponse<>(result.getRecords(), result.getTotal(), pageNum, pageSize);
    }

    public Product getProductById(Long id) {
        return productMapper.selectById(id);
    }

    public Product createProduct(Product product) {
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        
        productMapper.insert(product);
        return product;
    }

    public Product updateProduct(Long id, Product product) {
        Product existing = productMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("产品不存在");
        }
        
        existing.setName(product.getName());
        existing.setCategory(product.getCategory());
        existing.setCode(product.getCode());
        existing.setPrice(product.getPrice());
        existing.setStock(product.getStock());
        existing.setDescription(product.getDescription());
        existing.setStatus(product.getStatus());
        existing.setUpdatedAt(LocalDateTime.now());
        
        productMapper.updateById(existing);
        return existing;
    }

    public void deleteProduct(Long id) {
        if (productMapper.selectById(id) == null) {
            throw new IllegalArgumentException("产品不存在");
        }
        productMapper.deleteById(id);
    }

    public void batchDeleteProducts(Long[] ids) {
        for (Long id : ids) {
            productMapper.deleteById(id);
        }
    }

    public boolean checkCodeExists(String code, Long excludeId) {
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getCode, code);
        
        if (excludeId != null) {
            queryWrapper.ne(Product::getId, excludeId);
        }
        
        return productMapper.exists(queryWrapper);
    }
}