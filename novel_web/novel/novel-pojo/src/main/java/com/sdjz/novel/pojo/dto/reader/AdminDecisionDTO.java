package com.sdjz.novel.pojo.dto.reader;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminDecisionDTO {
    @Size(max = 128)
    private String bookName;
    @Size(max = 64)
    private String authorName;
    private Long categoryId;
    @Size(max = 2000)
    private String bookDesc;
    @Size(max = 500)
    private String reason;
}
