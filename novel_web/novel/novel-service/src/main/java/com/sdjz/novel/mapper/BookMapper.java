package com.sdjz.novel.mapper;

import com.sdjz.novel.pojo.dto.reader.BookQueryDTO;
import com.sdjz.novel.pojo.vo.reader.BookSummaryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
@Mapper
public interface BookMapper {
    List<BookSummaryVO> selectHomeLatest();
    List<BookSummaryVO> selectHomeHot();
    List<BookSummaryVO> selectHomeUpdated();
    List<BookSummaryVO> selectBookPage(@Param("query") BookQueryDTO query, @Param("orderBy") String orderBy, @Param("offset") int offset);
    Long countBooks(@Param("query") BookQueryDTO query);
    BookSummaryVO selectBookById(@Param("bookId") Long bookId);
    List<BookSummaryVO> selectRankBooks(@Param("orderBy") String orderBy);
    int increaseVisit(@Param("bookId") Long bookId);

    /** 批量回写用：一次累加 delta，把 N 条 UPDATE 合并成一条 */
    int increaseVisitBy(@Param("bookId") Long bookId, @Param("delta") long delta);
}
