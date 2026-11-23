package com.sdjz.novel.pojo.vo.reader;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageVO<T> {
    private List<T> list;
    private Integer page;
    private Integer size;
    private Long total;
}

