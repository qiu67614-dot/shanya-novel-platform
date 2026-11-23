package com.sdjz.novel.pojo.vo.reader;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChapterSummaryVO {
    private Long id;
    private Long bookId;
    private Integer chapterNum;
    private String chapterName;
    private Integer wordCount;
    private LocalDateTime updateTime;
    private LocalDateTime createTime;
}
