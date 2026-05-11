package com.example.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.crm.dto.PageResponse;
import com.example.crm.entity.Product;
import com.example.crm.entity.StockLog;
import com.example.crm.mapper.ProductMapper;
import com.example.crm.mapper.StockLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StockLogService {

    private final StockLogMapper stockLogMapper;
    private final ProductMapper productMapper;

    public StockLogService(StockLogMapper stockLogMapper, ProductMapper productMapper) {
        this.stockLogMapper = stockLogMapper;
        this.productMapper = productMapper;
    }

    public static final String TYPE_LOCK = "预占";
    public static final String TYPE_DEDUCT = "确认扣减";
    public static final String TYPE_RELEASE = "释放";
    public static final String TYPE_MANUAL = "手动调整";
    public static final String TYPE_REFUND = "退款回滚";

    @Transactional
    public void lockStock(Long productId, Long orderId, Integer quantity, String remark) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new IllegalArgumentException("产品不存在");
        }

        int beforeStock = product.getAvailableStock() != null ? product.getAvailableStock() : 0;
        int afterStock = beforeStock - quantity;

        if (afterStock < 0) {
            throw new IllegalArgumentException("库存不足，无法预占");
        }

        product.setAvailableStock(afterStock);
        product.setLockedStock((product.getLockedStock() != null ? product.getLockedStock() : 0) + quantity);
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);

        StockLog log = new StockLog();
        log.setProductId(productId);
        log.setOrderId(orderId);
        log.setType(TYPE_LOCK);
        log.setNum(-quantity);
        log.setBeforeStock(beforeStock);
        log.setAfterStock(afterStock);
        log.setRemark(remark);
        log.setCreateTime(LocalDateTime.now());
        stockLogMapper.insert(log);
    }

    @Transactional
    public void deductStock(Long productId, Long orderId, Integer quantity, String remark) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new IllegalArgumentException("产品不存在");
        }

        int beforeLockedStock = product.getLockedStock() != null ? product.getLockedStock() : 0;
        int afterLockedStock = beforeLockedStock - quantity;

        if (afterLockedStock < 0) {
            throw new IllegalArgumentException("预占库存异常");
        }

        int beforeTotalStock = product.getStock() != null ? product.getStock() : 0;
        int afterTotalStock = beforeTotalStock - quantity;

        if (afterTotalStock < 0) {
            throw new IllegalArgumentException("总库存不足");
        }

        product.setStock(afterTotalStock);
        product.setLockedStock(afterLockedStock);
        product.setAvailableStock((product.getAvailableStock() != null ? product.getAvailableStock() : 0));
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);

        StockLog log = new StockLog();
        log.setProductId(productId);
        log.setOrderId(orderId);
        log.setType(TYPE_DEDUCT);
        log.setNum(-quantity);
        log.setBeforeStock(beforeTotalStock);
        log.setAfterStock(afterTotalStock);
        log.setRemark(remark);
        log.setCreateTime(LocalDateTime.now());
        stockLogMapper.insert(log);
    }

    @Transactional
    public void releaseStock(Long productId, Long orderId, Integer quantity, String remark) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new IllegalArgumentException("产品不存在");
        }

        int beforeAvailableStock = product.getAvailableStock() != null ? product.getAvailableStock() : 0;
        int afterAvailableStock = beforeAvailableStock + quantity;

        int beforeLockedStock = product.getLockedStock() != null ? product.getLockedStock() : 0;
        int afterLockedStock = beforeLockedStock - quantity;

        if (afterLockedStock < 0) {
            throw new IllegalArgumentException("预占库存异常，无法释放");
        }

        product.setAvailableStock(afterAvailableStock);
        product.setLockedStock(afterLockedStock);
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);

        StockLog log = new StockLog();
        log.setProductId(productId);
        log.setOrderId(orderId);
        log.setType(TYPE_RELEASE);
        log.setNum(quantity);
        log.setBeforeStock(beforeAvailableStock);
        log.setAfterStock(afterAvailableStock);
        log.setRemark(remark);
        log.setCreateTime(LocalDateTime.now());
        stockLogMapper.insert(log);
    }

    @Transactional
    public void adjustStock(Long productId, Integer newStock, String remark) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new IllegalArgumentException("产品不存在");
        }

        int beforeStock = product.getStock() != null ? product.getStock() : 0;
        int beforeAvailableStock = product.getAvailableStock() != null ? product.getAvailableStock() : 0;
        int lockedStock = product.getLockedStock() != null ? product.getLockedStock() : 0;

        int newAvailableStock = newStock - lockedStock;
        if (newAvailableStock < 0) {
            throw new IllegalArgumentException("调整后可售库存不能为负数，预占库存为" + lockedStock);
        }

        product.setStock(newStock);
        product.setAvailableStock(newAvailableStock);
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);

        StockLog log = new StockLog();
        log.setProductId(productId);
        log.setOrderId(null);
        log.setType(TYPE_MANUAL);
        log.setNum(newStock - beforeStock);
        log.setBeforeStock(beforeStock);
        log.setAfterStock(newStock);
        log.setRemark(remark);
        log.setCreateTime(LocalDateTime.now());
        stockLogMapper.insert(log);
    }

    @Transactional
    public void refundStock(Long productId, Long orderId, Integer quantity, String remark) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new IllegalArgumentException("产品不存在");
        }

        int beforeStock = product.getStock() != null ? product.getStock() : 0;
        int afterStock = beforeStock + quantity;

        int beforeAvailableStock = product.getAvailableStock() != null ? product.getAvailableStock() : 0;
        int afterAvailableStock = beforeAvailableStock + quantity;

        product.setStock(afterStock);
        product.setAvailableStock(afterAvailableStock);
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);

        StockLog log = new StockLog();
        log.setProductId(productId);
        log.setOrderId(orderId);
        log.setType(TYPE_REFUND);
        log.setNum(quantity);
        log.setBeforeStock(beforeStock);
        log.setAfterStock(afterStock);
        log.setRemark(remark);
        log.setCreateTime(LocalDateTime.now());
        stockLogMapper.insert(log);
    }

    public PageResponse<StockLog> getStockLogs(Long productId, Integer pageNum, Integer pageSize) {
        Page<StockLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<StockLog> queryWrapper = new LambdaQueryWrapper<>();
        if (productId != null) {
            queryWrapper.eq(StockLog::getProductId, productId);
        }
        queryWrapper.orderByDesc(StockLog::getCreateTime);
        IPage<StockLog> result = stockLogMapper.selectPage(page, queryWrapper);
        return new PageResponse<>(result.getRecords(), result.getTotal(), pageNum, pageSize);
    }

    public List<StockLog> getStockLogsByOrderId(Long orderId) {
        LambdaQueryWrapper<StockLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StockLog::getOrderId, orderId);
        queryWrapper.orderByDesc(StockLog::getCreateTime);
        return stockLogMapper.selectList(queryWrapper);
    }

    public List<Product> getLowStockProducts() {
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getStatus, 1);
        queryWrapper.apply("available_stock IS NOT NULL AND safe_stock IS NOT NULL AND available_stock <= safe_stock");
        return productMapper.selectList(queryWrapper);
    }

    public boolean checkStockAvailable(Long productId, Integer quantity) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            return false;
        }
        int availableStock = product.getAvailableStock() != null ? product.getAvailableStock() : 0;
        return availableStock >= quantity;
    }
}
