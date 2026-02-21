package com.yuz.toplinks.service;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yuz.toplinks.entity.BaseEntity;
import com.yuz.toplinks.entity.TlkCategory;
import com.yuz.toplinks.mapper.TlkCategoryMapper;

@Service
public class CategoryService {

    private final TlkCategoryMapper categoryMapper;

    public CategoryService(TlkCategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @Cacheable("categories")
    public List<TlkCategory> listActiveCategories() {
        return categoryMapper.selectList(
                new QueryWrapper<TlkCategory>()
                        .eq("status", BaseEntity.STATUS_ACTIVE)
                        .orderByAsc("sort_order", "create_time"));
    }

    public TlkCategory getById(String id) {
        return categoryMapper.selectById(id);
    }
}
