package com.sdjz.novel.pojo.vo.reader;


import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CategoryVO {
    private Long id;
    private String categoryName;
    private Integer direction;
    private Integer sort;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
