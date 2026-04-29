package com.example.questionbank.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.questionbank.entity.Bank;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankMapper extends BaseMapper<Bank> {

    /** 原子自增题目数，避免并发更新死锁 */
    @Update("UPDATE banks SET question_count = question_count + 1 WHERE id = #{bankId}")
    int incrementQuestionCount(@Param("bankId") Integer bankId);

    /** 原子自减题目数（删题时使用） */
    @Update("UPDATE banks SET question_count = GREATEST(question_count - 1, 0) WHERE id = #{bankId}")
    int decrementQuestionCount(@Param("bankId") Integer bankId);

    /** 公开题库总数（与 selectPublicBanksPage 条件一致） */
    long countPublicBanksPage(
            @Param("categoryIds") List<Integer> categoryIds,
            @Param("keyword") String keyword);

    /**
     * 公开题库分页，SQL 见 mapper/BankMapper.xml；sortBy 仅 latest | popular | favorites（Service 白名单）。
     */
    IPage<Bank> selectPublicBanksPage(
            Page<Bank> page,
            @Param("categoryIds") List<Integer> categoryIds,
            @Param("keyword") String keyword,
            @Param("sortBy") String sortBy);

    @Select("SELECT * FROM banks WHERE visibility = 'public' AND status = 'published' ORDER BY view_count DESC LIMIT #{limit}")
    List<Bank> selectHotPublicBanks(@Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM banks WHERE category_id = #{categoryId} AND visibility = 'public' AND status = 'published'")
    long countPublicPublishedByCategoryId(@Param("categoryId") Integer categoryId);

    @Select("SELECT COUNT(*) FROM banks WHERE visibility = 'public' AND status = 'published'")
    long countPublicPublishedBanks();

    @Select("SELECT id FROM banks WHERE visibility = 'public' AND status = 'published'")
    List<Integer> selectPublicPublishedBankIds();

    @Select("SELECT COUNT(*) FROM banks WHERE author_id = #{authorId} AND visibility = 'public' AND status = 'published'")
    long countAuthorPublicPublishedBanks(@Param("authorId") Integer authorId);

    @Select("SELECT id FROM banks WHERE author_id = #{authorId} AND visibility = 'public' AND status = 'published'")
    List<Integer> selectAuthorPublicPublishedBankIds(@Param("authorId") Integer authorId);

    /** 用户主页展示的公开题库（与列表页条件一致） */
    @Select("SELECT * FROM banks WHERE author_id = #{authorId} AND visibility = 'public' AND status = 'published' ORDER BY id DESC")
    IPage<Bank> selectAuthorPublicBanksPage(Page<Bank> page, @Param("authorId") Integer authorId);
}
