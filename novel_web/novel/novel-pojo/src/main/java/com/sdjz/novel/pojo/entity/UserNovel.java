package com.sdjz.novel.pojo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserNovel {
    private Long id;
    private Long userId;
    private String bookName;
    private String coverUrl;
    private String originalFileName;
    private String encoding;
    private Long fileSize;
    private Long wordCount;
    private Integer chapterCount;
    private Long firstChapterId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
