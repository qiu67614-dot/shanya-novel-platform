package com.sdjz.novel.pojo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookInfo {
    private Long id;
    private String bookName;
    private String authorName;
    private Long categoryId;
    private String picUrl;
    private String bookDesc;
    private Integer bookStatus;
    private Long wordCount;
    private Long visitCount;
    private Integer chapterCount;
    private Long firstChapterId;
    private Long lastChapterId;
    private String lastChapterName;
    private LocalDateTime lastChapterUpdateTime;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
