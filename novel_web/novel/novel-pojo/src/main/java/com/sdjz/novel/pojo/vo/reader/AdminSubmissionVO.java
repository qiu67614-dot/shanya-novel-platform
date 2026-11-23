package com.sdjz.novel.pojo.vo.reader;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AdminSubmissionVO {
    private Long id;
    private Long userNovelId;
    private Long userId;
    private String username;
    private String originalFileName;
    private String bookName;
    private String picUrl;
    private String authorName;
    private Long categoryId;
    private String categoryName;
    private String bookDesc;
    private Boolean rightsConfirmed;
    private String status;
    private String publicStatus;
    private String rejectionReason;
    private Long publicBookId;
    private Integer chapterCount;
    private Long wordCount;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
