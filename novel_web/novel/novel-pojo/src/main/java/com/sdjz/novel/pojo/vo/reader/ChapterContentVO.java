package com.sdjz.novel.pojo.vo.reader;

import lombok.Data;

@Data
public class ChapterContentVO {
    private BookBriefVO book;
    private ChapterSummaryVO chapter;
    private String content;
    private Long previousChapterId;
    private Long nextChapterId;
}
