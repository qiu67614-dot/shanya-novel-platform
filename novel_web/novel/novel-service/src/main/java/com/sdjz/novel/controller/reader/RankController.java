package com.sdjz.novel.controller.reader;

import com.sdjz.novel.common.response.RestResp;
import com.sdjz.novel.pojo.vo.reader.BookSummaryVO;
import com.sdjz.novel.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/front/ranks")
@RequiredArgsConstructor
public class RankController {

    private final BookService bookService;

    @GetMapping("/{type}")
    public RestResp<List<BookSummaryVO>> getRank(@PathVariable String type) {
        return RestResp.ok(bookService.getRankBooks(type));
    }
}
