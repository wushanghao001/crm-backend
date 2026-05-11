package com.example.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.crm.dto.PageResponse;
import com.example.crm.mapper.BrandCodeMapper;
import com.example.crm.mapper.MaterialCodeMapper;
import com.example.crm.mapper.ProjectCodeMapper;
import org.springframework.stereotype.Service;

@Service
public class ArchiveCodeService {

    private final ProjectCodeMapper projectCodeMapper;
    private final MaterialCodeMapper materialCodeMapper;
    private final BrandCodeMapper brandCodeMapper;

    public ArchiveCodeService(ProjectCodeMapper projectCodeMapper,
                              MaterialCodeMapper materialCodeMapper,
                              BrandCodeMapper brandCodeMapper) {
        this.projectCodeMapper = projectCodeMapper;
        this.materialCodeMapper = materialCodeMapper;
        this.brandCodeMapper = brandCodeMapper;
    }

    public <T> PageResponse<T> listArchiveCodes(Integer pageNum, Integer pageSize, String keyword,
                                                 Class<T> entityClass,
                                                 java.util.function.Function<Long, ? extends com.baomidou.mybatisplus.core.mapper.BaseMapper<T>> mapperProvider) {
        Page<T> page = new Page<>(pageNum, pageSize);
        QueryWrapper<T> queryWrapper = new QueryWrapper<>();

        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.like("name", keyword)
                       .or()
                       .like("code", keyword);
        }

        com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper = mapperProvider.apply(0L);
        IPage<T> result = mapper.selectPage(page, queryWrapper);

        return new PageResponse<>(result.getRecords(), result.getTotal(), pageNum, pageSize);
    }

    public <T> T getArchiveCodeById(Long id, java.util.function.Function<Long, ? extends com.baomidou.mybatisplus.core.mapper.BaseMapper<T>> mapperProvider) {
        com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper = mapperProvider.apply(0L);
        return mapper.selectById(id);
    }

    public <T> T createArchiveCode(T entity,
                                    java.util.function.Consumer<T> insertConsumer) {
        insertConsumer.accept(entity);
        return entity;
    }

    public <T> void updateArchiveCode(Long id, T entity,
                                      java.util.function.BiFunction<Long, T, T> updateFunction) {
        updateFunction.apply(id, entity);
    }

    public <T> void deleteArchiveCode(Long id,
                                       java.util.function.Function<Long, ? extends com.baomidou.mybatisplus.core.mapper.BaseMapper<T>> mapperProvider) {
        com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper = mapperProvider.apply(0L);
        mapper.deleteById(id);
    }

    public boolean checkCodeExists(String code, Long excludeId, String tableName) {
        String sql = "SELECT COUNT(*) > 0 FROM " + tableName + " WHERE code = #{code}";
        if (excludeId != null) {
            sql += " AND id != #{excludeId}";
        }
        return false;
    }

    public void batchDeleteArchiveCodes(Long[] ids,
                                         java.util.function.Function<Long[], Integer> batchDeleteConsumer) {
        batchDeleteConsumer.apply(ids);
    }
}