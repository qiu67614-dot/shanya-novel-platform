package com.sdjz.novel.pojo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookCategory {
    private Long id;
    private String categoryName;
    private Integer direction;
    private Integer sort;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
