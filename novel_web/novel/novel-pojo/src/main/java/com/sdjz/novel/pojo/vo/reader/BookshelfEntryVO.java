package com.sdjz.novel.pojo.vo.reader;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BookshelfEntryVO {

    private BookSummaryVO book;

    private Long lastChapterId;
    private String lastChapterName;


    private LocalDateTime lastReadAt;


    private LocalDateTime createTime;


    private LocalDateTime updateTime;
}