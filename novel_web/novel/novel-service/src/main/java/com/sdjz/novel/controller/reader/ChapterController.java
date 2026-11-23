package com.sdjz.novel.controller.reader;

import com.sdjz.novel.common.response.RestResp;
import com.sdjz.novel.pojo.vo.reader.ChapterContentVO;
import com.sdjz.novel.pojo.vo.reader.ChapterSummaryVO;
import com.sdjz.novel.pojo.vo.reader.PageVO;
import com.sdjz.novel.service.ChapterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/front")
@RequiredArgsConstructor
public class ChapterController {

    private final ChapterService chapterService;

    @GetMapping("/books/{bookId}/chapters")
    public RestResp<PageVO<ChapterSummaryVO>> getChapters(
            @PathVariable Long bookId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return RestResp.ok(chapterService.getChapters(bookId, page, size));
    }

    @GetMapping("/chapters/{chapterId}")
    public RestResp<ChapterContentVO> getChapterContent(@PathVariable Long chapterId) {
        return RestResp.ok(chapterService.getChapterContent(chapterId));
    }
}
