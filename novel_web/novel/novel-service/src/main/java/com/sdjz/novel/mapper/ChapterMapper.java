package com.sdjz.novel.mapper;

import com.sdjz.novel.pojo.vo.reader.ChapterContentVO;
import com.sdjz.novel.pojo.vo.reader.ChapterSummaryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ChapterMapper {
    List<ChapterSummaryVO> selectChapterPage(@Param("bookId") Long bookId, @Param("offset") int offset, @Param("size") int size);
    Long countChapters(@Param("bookId") Long bookId);
    ChapterContentVO selectChapterContent(@Param("chapterId") Long chapterId);
    Long selectPreviousChapterId(@Param("bookId") Long bookId, @Param("chapterNum") Integer chapterNum);
    Long selectNextChapterId(@Param("bookId") Long bookId, @Param("chapterNum") Integer chapterNum);
}
