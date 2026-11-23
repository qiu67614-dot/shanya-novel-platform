package com.sdjz.novel.controller.reader;

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

    @GetMapping("/home")
    public RestResp<HomeBooksVO> getHomeBooks() {
        return RestResp.ok(bookService.getHomeBooks());
    }

    @GetMapping
    public RestResp<PageVO<BookSummaryVO>> getBookPage(BookQueryDTO query) {
        return RestResp.ok(bookService.getBookPage(query));
    }

    @GetMapping("/{bookId}")
    public RestResp<BookSummaryVO> getBookById(@PathVariable Long bookId) {
        return RestResp.ok(bookService.getBookById(bookId));
    }

    @PostMapping("/{bookId}/visit")
    public RestResp<Void> increaseVisit(@PathVariable Long bookId) {
        bookService.increaseVisit(bookId);
        return RestResp.ok();
    }
}
