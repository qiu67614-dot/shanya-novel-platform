package com.sdjz.novel.mapper;

import com.sdjz.novel.pojo.vo.reader.ChapterSummaryVO;
import com.sdjz.novel.pojo.vo.reader.HistoryEntryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface HistoryMapper {

    List<HistoryEntryVO> selectPage(
            @Param("userId") Long userId,
            @Param("offset") int offset,
            @Param("size") int size
    );

    Long count(@Param("userId") Long userId);

    ChapterSummaryVO selectChapterOfBook(
            @Param("bookId") Long bookId,
            @Param("chapterId") Long chapterId
    );

    int upsert(
            @Param("userId") Long userId,
            @Param("bookId") Long bookId,
            @Param("chapterId") Long chapterId,
            @Param("chapterName") String chapterName
    );

    int updateBookshelfProgress(
            @Param("userId") Long userId,
            @Param("bookId") Long bookId,
            @Param("chapterId") Long chapterId,
            @Param("chapterName") String chapterName
    );
}
