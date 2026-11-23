package com.sdjz.novel.controller.reader;

import com.sdjz.novel.common.ratelimit.RateLimit;
import com.sdjz.novel.common.ratelimit.RateLimitDimension;
import com.sdjz.novel.common.ratelimit.RateLimitGroup;
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

    /** 目录：书章数多时前端会按 200 一页连续拉取，所以归到 READ 而不是 CHAPTER。 */
    @RateLimit(group = RateLimitGroup.READ, dimension = RateLimitDimension.USER)
    @GetMapping("/books/{bookId}/chapters")
    public RestResp<PageVO<ChapterSummaryVO>> getChapters(
            @PathVariable Long bookId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return RestResp.ok(chapterService.getChapters(bookId, page, size));
    }

    /** 正文：单请求响应体最大，单独一档，阈值高于目录以支持连续翻章。 */
    @RateLimit(group = RateLimitGroup.CHAPTER, dimension = RateLimitDimension.USER)
    @GetMapping("/chapters/{chapterId}")
    public RestResp<ChapterContentVO> getChapterContent(@PathVariable Long chapterId) {
        return RestResp.ok(chapterService.getChapterContent(chapterId));
    }
}