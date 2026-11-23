package com.sdjz.novel.service;

import com.sdjz.novel.pojo.vo.reader.ChapterContentVO;
import com.sdjz.novel.pojo.vo.reader.ChapterSummaryVO;
import com.sdjz.novel.pojo.vo.reader.PageVO;

public interface ChapterService {
    PageVO<ChapterSummaryVO> getChapters(Long bookId, int page, int size);
    ChapterContentVO getChapterContent(Long chapterId);
}
