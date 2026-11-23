package com.sdjz.novel.pojo.dto.reader;

import lombok.Data;

@Data
public class BookQueryDTO {

    private String q;
    private Long categoryId;
    private Integer status;
    private String sort = "update";
    private Integer page = 1;
    private Integer size = 20;
}
