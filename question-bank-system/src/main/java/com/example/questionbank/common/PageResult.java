package com.example.questionbank.common;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PageResult<T> {
    private long total;     // 总记录数
    private int pages;    // 总页数
    private int current;    // 当前页
    private int size;   // 每页大小
    private java.util.List<T> records; // 当前页数据

    public PageResult(long total, int pages, int current, int size, java.util.List<T> records) {
        this.total = total;
        this.pages = pages;
        this.current = current;
        this.size = size;
        this.records = records;
    }

    public static <T> PageResult<T> of(long total, int current, int size, java.util.List<T> records) {
        int pages = (int) Math.ceil((double) total / size);
        return new PageResult<>(total, pages, current, size, records);
    }
}