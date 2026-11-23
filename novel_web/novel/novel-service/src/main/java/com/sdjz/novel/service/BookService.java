package com.sdjz.novel.service;

import com.sdjz.novel.pojo.dto.reader.BookQueryDTO;
import com.sdjz.novel.pojo.vo.reader.BookSummaryVO;
import com.sdjz.novel.pojo.vo.reader.CategoryVO;
import com.sdjz.novel.pojo.vo.reader.HomeBooksVO;
import com.sdjz.novel.pojo.vo.reader.PageVO;
import java.util.List;

public interface BookService {
    HomeBooksVO getHomeBooks();
    List<CategoryVO> getCategories(Integer direction);
    PageVO<BookSummaryVO> getBookPage(BookQueryDTO query);
    List<BookSummaryVO> getRankBooks(String type);
    BookSummaryVO getBookById(Long bookId);
    void increaseVisit(Long bookId);
}
