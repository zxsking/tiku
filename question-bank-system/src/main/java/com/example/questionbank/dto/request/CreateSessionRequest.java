package com.example.questionbank.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class CreateSessionRequest {

    /** 题库ID，null 表示跨库随机出题 */
    private Integer bankId;

    /** 指定题目ID列表（用于错题练习），与 bankId/count 二选一 */
    private List<Integer> questionIds;

    /** 出题数量（当 questionIds 为空时生效） */
    private Integer count = 20;

    /** 时间限制（秒），0=不限时 */
    private Integer timeLimit = 0;

    /** 题目类型筛选，如 ["single","multiple","judge"] */
    private List<String> types;

    /** 难度筛选，如 ["easy","medium","hard"] */
    private List<String> difficulties;

    /** 是否随机打乱题目顺序 */
    private Boolean shuffle = true;
}
