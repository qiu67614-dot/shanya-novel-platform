package com.sdjz.novel.pojo.vo.reader;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FollowingBookVO {

    private BookSummaryVO book;

    private LocalDateTime followedAt;

    private Long lastNotifiedChapterId;

    private LocalDateTime lastNotifiedAt;

    private Boolean hasUpdate;
}
