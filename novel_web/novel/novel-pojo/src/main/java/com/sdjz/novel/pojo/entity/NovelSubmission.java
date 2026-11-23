package com.sdjz.novel.pojo.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NovelSubmission {
    private Long id;
    private Long userNovelId;
    private Long userId;
    private String visibility;
    private String bookName;
    private String authorName;
    private Long categoryId;
    private String bookDesc;
    private String coverUrl;
    private Boolean rightsConfirmed;
    private String status;
    private String publicStatus;
    private String rejectionReason;
    private Long publicBookId;
    private Long reviewedBy;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
