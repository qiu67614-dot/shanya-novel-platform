package com.sdjz.novel.pojo.dto.reader;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PrivateProgressDTO {
    @NotNull
    private Long chapterId;
}
