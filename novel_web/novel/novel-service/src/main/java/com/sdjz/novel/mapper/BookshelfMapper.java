package com.sdjz.novel.mapper;

import com.sdjz.novel.pojo.vo.reader.BookshelfEntryVO;
import com.sdjz.novel.pojo.vo.reader.ChapterSummaryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BookshelfMapper {

    List<BookshelfEntryVO> selectByUserId(@Param("userId") Long userId);

    BookshelfEntryVO selectOne(
            @Param("userId") Long userId,
            @Param("bookId") Long bookId
    );

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

    int deleteByUserAndBook(
            @Param("userId") Long userId,
            @Param("bookId") Long bookId
    );
}