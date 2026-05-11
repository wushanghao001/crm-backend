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
    private final StockLogService stockLogService;

    public ProductService(ProductMapper productMapper, StockLogService stockLogService) {
        this.productMapper = productMapper;
        this.stockLogService = stockLogService;
    }

    public PageResponse<Product> listProducts(Integer pageNum, Integer pageSize, String keyword, String category, Integer status,
                                              Long projectCodeId, Long materialCodeId, Long brandCodeId) {
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
        if (projectCodeId != null) {
            queryWrapper.eq(Product::getProjectCodeId, projectCodeId);
        }
        if (materialCodeId != null) {
            queryWrapper.eq(Product::getMaterialCodeId, materialCodeId);
        }
        if (brandCodeId != null) {
            queryWrapper.eq(Product::getBrandCodeId, brandCodeId);
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

        if (product.getSafeStock() == null) {
            product.setSafeStock(5);
        }
        if (product.getStock() != null) {
            product.setAvailableStock(product.getStock());
            product.setLockedStock(0);
        } else {
            product.setStock(0);
            product.setAvailableStock(0);
            product.setLockedStock(0);
        }

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
        existing.setDescription(product.getDescription());
        existing.setStatus(product.getStatus());
        existing.setSafeStock(product.getSafeStock());
        existing.setProjectCodeId(product.getProjectCodeId());
        existing.setProjectCodeName(product.getProjectCodeName());
        existing.setMaterialCodeId(product.getMaterialCodeId());
        existing.setMaterialCodeName(product.getMaterialCodeName());
        existing.setBrandCodeId(product.getBrandCodeId());
        existing.setBrandCodeName(product.getBrandCodeName());
        existing.setUpdatedAt(LocalDateTime.now());
        
        if (product.getStock() != null && !product.getStock().equals(existing.getStock())) {
            int oldStock = existing.getStock() != null ? existing.getStock() : 0;
            int lockedStock = existing.getLockedStock() != null ? existing.getLockedStock() : 0;
            int newAvailableStock = product.getStock() - lockedStock;
            if (newAvailableStock < 0) {
                throw new IllegalArgumentException("调整后可售库存不能为负数，当前预占库存为" + lockedStock);
            }
            existing.setStock(product.getStock());
            existing.setAvailableStock(newAvailableStock);
            stockLogService.adjustStock(id, product.getStock(), "更新产品时调整库存");
        }
        
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

    public void adjustStock(Long productId, Integer newStock, String remark) {
        stockLogService.adjustStock(productId, newStock, remark);
    }
}