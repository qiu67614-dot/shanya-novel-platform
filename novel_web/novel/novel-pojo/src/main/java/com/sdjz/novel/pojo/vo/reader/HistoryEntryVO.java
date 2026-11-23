package com.sdjz.novel.pojo.vo.reader;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HistoryEntryVO {

    private BookSummaryVO book;

    private Long chapterId;
    private String chapterName;

    private LocalDateTime readAt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
