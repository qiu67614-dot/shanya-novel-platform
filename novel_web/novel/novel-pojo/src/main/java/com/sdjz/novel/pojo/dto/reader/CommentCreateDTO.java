package com.sdjz.novel.pojo.dto.reader;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentCreateDTO {

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论内容不能超过500字")
    private String content;

    /** 不传表示主评论；传入时必须是同一作品下的主评论。 */
    @Positive(message = "父评论ID必须为正数")
    private Long parentCommentId;
}
