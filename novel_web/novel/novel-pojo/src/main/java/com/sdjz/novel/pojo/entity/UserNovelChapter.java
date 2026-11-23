package com.sdjz.novel.pojo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserNovelChapter {
    private Long id;
    private Long novelId;
    private Integer chapterNum;
    private String chapterName;
    private String content;
    private Integer wordCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
