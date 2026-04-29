package com.example.questionbank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.questionbank.service.CategoryService;
import com.example.questionbank.entity.Category;
import com.example.questionbank.entity.Bank;
import com.example.questionbank.mapper.CategoryMapper;
import com.example.questionbank.mapper.BankMapper;
import com.example.questionbank.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private BankMapper bankMapper;

    @Override
    public List<Category> getAllCategories() {
        // 获取所有分类
        List<Category> allCategories = categoryMapper.selectList(null);
        
        // 为每个分类添加题库数量
        for (Category category : allCategories) {
            QueryWrapper<Bank> bankQuery = new QueryWrapper<>();
            bankQuery.eq("category_id", category.getId());
            long count = bankMapper.selectCount(bankQuery);
            category.setCount(count);
        }
        
        // 构建树形结构
        return buildTree(allCategories);
    }
    
    // 将平面列表转换为树形结构
    private List<Category> buildTree(List<Category> allCategories) {
        // 找到所有根分类（parentId为null或0）
        List<Category> rootCategories = allCategories.stream()
                .filter(c -> c.getParentId() == null || c.getParentId() == 0)
                .collect(Collectors.toList());
        
        // 为每个根节点构建子节点
        for (Category root : rootCategories) {
            buildChildren(root, allCategories);
        }
        
        return rootCategories;
    }

    // 递归构建子节点
    private void buildChildren(Category parent, List<Category> allCategories) {
        List<Category> children = allCategories.stream()
                .filter(c -> c.getParentId() != null && c.getParentId().equals(parent.getId()))
                .collect(Collectors.toList());
        
        if (!children.isEmpty()) {
            parent.setChildren(children);
            // 对子节点继续构建其子节点
            for (Category child : children) {
                buildChildren(child, allCategories);
            }
        }
    }

    @Override
    public Category createCategory(Category category) {
        categoryMapper.insert(category);
        return category;
    }

    @Override
    public Category updateCategory(Integer id, Category category) {
        UpdateWrapper<Category> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", id)
               .set("name", category.getName())
               .set("sort", category.getSort())
               .set("parent_id", category.getParentId());
        categoryMapper.update(null, wrapper);
        category.setId(id);
        return category;
    }

    @Override
    public void deleteCategory(Integer id) {
        // 检查是否有子分类
        List<Category> children = categoryMapper.selectByParentId(id);
        if (children != null && !children.isEmpty()) {
            throw new BusinessException("删除失败：该分类下存在子分类");
        }
        
        categoryMapper.deleteById(id);
    }
}