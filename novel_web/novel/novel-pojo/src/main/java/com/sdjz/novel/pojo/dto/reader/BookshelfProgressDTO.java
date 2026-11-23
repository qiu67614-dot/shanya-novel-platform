package com.sdjz.novel.pojo.dto.reader;

import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class BookshelfProgressDTO {

    /**
     * 最近阅读章节。
     * 为空时表示只加入书架，不更新阅读进度。
     */
    @Positive(message = "章节ID必须为正数")
    private Long chapterId;
}