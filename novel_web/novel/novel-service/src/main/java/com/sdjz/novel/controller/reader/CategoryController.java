package com.sdjz.novel.controller.reader;

import com.sdjz.novel.common.response.RestResp;
import com.sdjz.novel.pojo.vo.reader.CategoryVO;
import com.sdjz.novel.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/front/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final BookService bookService;

    @GetMapping
    public RestResp<List<CategoryVO>> getCategories(
            @RequestParam(value = "direction",required = false) Integer direction) {
        return RestResp.ok(bookService.getCategories(direction));
    }
}
