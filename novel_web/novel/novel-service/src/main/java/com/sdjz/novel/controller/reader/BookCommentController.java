package com.sdjz.novel.controller.reader;

import com.sdjz.novel.common.response.RestResp;
import com.sdjz.novel.pojo.vo.reader.CommentVO;
import com.sdjz.novel.pojo.vo.reader.PageVO;
import com.sdjz.novel.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/front/books")
@RequiredArgsConstructor
public class BookCommentController {

    private final CommentService commentService;

    @GetMapping("/{bookId}/comments")
    public RestResp<PageVO<CommentVO>> getComments(
            @PathVariable Long bookId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        return RestResp.ok(commentService.getRootComments(bookId, page, size));
    }

    @GetMapping("/{bookId}/comments/{commentId}/replies")
    public RestResp<PageVO<CommentVO>> getReplies(
            @PathVariable Long bookId,
            @PathVariable Long commentId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        return RestResp.ok(commentService.getReplies(bookId, commentId, page, size));
    }
}
