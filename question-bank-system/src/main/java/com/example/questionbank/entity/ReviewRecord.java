package com.example.questionbank.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("review_records")
public class ReviewRecord {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Integer userId;

    @TableField("question_id")
    private Integer questionId;

    @TableField("reviewed_at")
    private LocalDateTime reviewedAt;

    /** 复习结果：true=答对 false=答错 null=仅标记已复习 */
    @TableField("result")
    private Boolean result;
}
