package com.sdjz.novel.controller.reader;

import com.sdjz.novel.common.ratelimit.RateLimit;
import com.sdjz.novel.common.ratelimit.RateLimitDimension;
import com.sdjz.novel.common.ratelimit.RateLimitGroup;
import com.sdjz.novel.common.response.RestResp;
import com.sdjz.novel.pojo.dto.reader.BookQueryDTO;
import com.sdjz.novel.pojo.vo.reader.BookSummaryVO;
import com.sdjz.novel.pojo.vo.reader.HomeBooksVO;
import com.sdjz.novel.pojo.vo.reader.PageVO;
import com.sdjz.novel.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/front/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @RateLimit(group = RateLimitGroup.READ, dimension = RateLimitDimension.USER)
    @GetMapping("/home")
    public RestResp<HomeBooksVO> getHomeBooks() {
        return RestResp.ok(bookService.getHomeBooks());
    }

    /** 搜索：条件查询叠加 count，代价高于普通读，单独一档。 */
    @RateLimit(group = RateLimitGroup.SEARCH, dimension = RateLimitDimension.USER)
    @GetMapping
    public RestResp<PageVO<BookSummaryVO>> getBookPage(BookQueryDTO query) {
        return RestResp.ok(bookService.getBookPage(query));
    }

    @RateLimit(group = RateLimitGroup.READ, dimension = RateLimitDimension.USER)
    @GetMapping("/{bookId}")
    public RestResp<BookSummaryVO> getBookById(@PathVariable Long bookId) {
        return RestResp.ok(bookService.getBookById(bookId));
    }

    /** 点击量：游客也会调用，维度为 USER 时会自动降级到 IP 桶。 */
    @RateLimit(group = RateLimitGroup.READ, dimension = RateLimitDimension.USER)
    @PostMapping("/{bookId}/visit")
    public RestResp<Void> increaseVisit(@PathVariable Long bookId) {
        bookService.increaseVisit(bookId);
        return RestResp.ok();
    }
}