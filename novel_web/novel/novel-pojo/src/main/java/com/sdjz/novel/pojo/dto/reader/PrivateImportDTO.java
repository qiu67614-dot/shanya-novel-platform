package com.sdjz.novel.pojo.dto.reader;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PrivateImportDTO {
    private String visibility = "PRIVATE";
    @Size(max = 128)
    private String bookName;
    @Size(max = 64)
    private String authorName;
    @Positive
    private Long categoryId;
    @Size(max = 2000)
    private String bookDesc;
    private Boolean rightsConfirmed = false;
}
