package com.sdjz.novel.pojo.vo.reader;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PrivateNovelVO {
    private Long id;
    private String bookName;
    private String picUrl;
    private String originalFileName;
    private String encoding;
    private Long fileSize;
    private Long wordCount;
    private Integer chapterCount;
    private Long firstChapterId;
    private Long lastReadChapterId;
    private String lastReadChapterName;
    private LocalDateTime lastReadAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String visibility;
    private String authorName;
    private Long categoryId;
    private String bookDesc;
    private Boolean rightsConfirmed;
    private Long submissionId;
    private String submissionStatus;
    private String rejectionReason;
    private Long publicBookId;
    private String publicStatus;
}
