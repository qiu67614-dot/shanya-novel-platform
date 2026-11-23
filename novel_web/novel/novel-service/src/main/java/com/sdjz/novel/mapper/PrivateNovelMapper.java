package com.sdjz.novel.mapper;

import com.sdjz.novel.pojo.entity.UserNovel;
import com.sdjz.novel.pojo.entity.UserNovelChapter;
import com.sdjz.novel.pojo.vo.reader.ChapterSummaryVO;
import com.sdjz.novel.pojo.vo.reader.PrivateNovelVO;
import com.sdjz.novel.pojo.dto.reader.SubmissionUpdateDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PrivateNovelMapper {
    int updateCover(@Param("userId") Long userId, @Param("novelId") Long novelId,
                    @Param("coverUrl") String coverUrl);
    int insertNovel(UserNovel novel);
    int insertChapter(UserNovelChapter chapter);
    int setFirstChapter(@Param("novelId") Long novelId, @Param("chapterId") Long chapterId);

    List<PrivateNovelVO> selectPage(@Param("userId") Long userId,
                                    @Param("offset") long offset, @Param("size") int size);
    Long count(@Param("userId") Long userId);
    PrivateNovelVO selectNovel(@Param("userId") Long userId, @Param("novelId") Long novelId);

    List<ChapterSummaryVO> selectChapterPage(@Param("userId") Long userId,
                                              @Param("novelId") Long novelId,
                                              @Param("offset") long offset, @Param("size") int size);
    Long countChapters(@Param("userId") Long userId, @Param("novelId") Long novelId);
    ChapterSummaryVO selectChapterMeta(@Param("userId") Long userId,
                                        @Param("novelId") Long novelId,
                                        @Param("chapterId") Long chapterId);
    UserNovelChapter selectChapter(@Param("userId") Long userId,
                                   @Param("novelId") Long novelId,
                                   @Param("chapterId") Long chapterId);
    Long selectPreviousChapterId(@Param("novelId") Long novelId, @Param("chapterNum") Integer chapterNum);
    Long selectNextChapterId(@Param("novelId") Long novelId, @Param("chapterNum") Integer chapterNum);

    int upsertProgress(@Param("userId") Long userId,
                       @Param("novelId") Long novelId, @Param("chapterId") Long chapterId);
    int deleteNovel(@Param("userId") Long userId, @Param("novelId") Long novelId);
    int deleteSubmission(@Param("userId") Long userId, @Param("novelId") Long novelId);
    int updateNovelMeta(@Param("userId") Long userId, @Param("novelId") Long novelId,
                        @Param("bookName") String bookName, @Param("originalFileName") String originalFileName,
                        @Param("encoding") String encoding, @Param("fileSize") long fileSize,
                        @Param("wordCount") long wordCount, @Param("chapterCount") int chapterCount,
                        @Param("firstChapterId") Long firstChapterId);
    int deleteChapters(@Param("userId") Long userId, @Param("novelId") Long novelId);
    int deleteProgress(@Param("userId") Long userId, @Param("novelId") Long novelId);
}
