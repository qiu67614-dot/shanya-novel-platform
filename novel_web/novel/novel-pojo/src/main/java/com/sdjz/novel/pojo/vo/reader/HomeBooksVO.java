package com.sdjz.novel.pojo.vo.reader;

import lombok.Data;
import java.util.List;

@Data
public class HomeBooksVO {
    private List<BookSummaryVO> latest;
    private List<BookSummaryVO> hot;
    private List<BookSummaryVO> updated;
}
