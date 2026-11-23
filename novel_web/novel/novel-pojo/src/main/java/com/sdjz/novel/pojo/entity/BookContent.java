package com.sdjz.novel.pojo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookContent {
    private Long id;
    private Long chapterId;
    private String content;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}