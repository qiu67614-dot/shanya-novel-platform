package com.sdjz.novel.pojo.dto.reader;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ProgressUpdateDTO {

    @NotNull(message = "书籍ID不能为空")
    @Positive(message = "书籍ID必须为正数")
    private Long bookId;

    @NotNull(message = "章节ID不能为空")
    @Positive(message = "章节ID必须为正数")
    private Long chapterId;
}
