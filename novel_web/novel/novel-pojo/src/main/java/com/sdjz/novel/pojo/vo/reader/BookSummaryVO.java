package com.sdjz.novel.pojo.vo.reader;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BookSummaryVO {
    private Long id;
    private String bookName;
    private String authorName;
    private Long categoryId;
    private String categoryName;
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
