package com.sdjz.novel.pojo.vo.reader;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentVO {
    private Long id;
    private Long userId;
    private Long parentCommentId;
    private String nickName;
    private String userPhoto;
    private String replyToNickName;
    private String content;
    private Integer replyCount;
    private Boolean deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
