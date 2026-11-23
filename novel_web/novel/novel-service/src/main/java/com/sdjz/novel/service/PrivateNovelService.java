package com.sdjz.novel.service;

import com.sdjz.novel.pojo.dto.reader.PrivateProgressDTO;
import com.sdjz.novel.pojo.dto.reader.PrivateImportDTO;
import com.sdjz.novel.pojo.vo.reader.ChapterContentVO;
import com.sdjz.novel.pojo.vo.reader.ChapterSummaryVO;
import com.sdjz.novel.pojo.vo.reader.PageVO;
import com.sdjz.novel.pojo.vo.reader.PrivateNovelVO;
import org.springframework.web.multipart.MultipartFile;

public interface PrivateNovelService {
    PrivateNovelVO importTxt(Long userId, MultipartFile file);
    PrivateNovelVO importTxt(Long userId, MultipartFile file, PrivateImportDTO options);
    PrivateNovelVO importTxt(Long userId, MultipartFile file, PrivateImportDTO options, MultipartFile cover);
    PageVO<PrivateNovelVO> getPage(Long userId, int page, int size);
    PrivateNovelVO getNovel(Long userId, Long novelId);
    PageVO<ChapterSummaryVO> getChapters(Long userId, Long novelId, int page, int size);
    ChapterContentVO getChapter(Long userId, Long novelId, Long chapterId);
    void updateProgress(Long userId, Long novelId, PrivateProgressDTO dto);
    void delete(Long userId, Long novelId);
}
