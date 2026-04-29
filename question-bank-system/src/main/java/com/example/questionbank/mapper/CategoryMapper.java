package com.example.questionbank.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.questionbank.entity.Category;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryMapper extends BaseMapper<Category> {
    
    @Select("SELECT * FROM categories WHERE parent_id = #{parentId}")
    List<Category> selectByParentId(Integer parentId);
}