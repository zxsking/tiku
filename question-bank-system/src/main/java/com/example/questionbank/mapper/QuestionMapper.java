package com.example.questionbank.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.questionbank.entity.Question;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionMapper extends BaseMapper<Question> {
}