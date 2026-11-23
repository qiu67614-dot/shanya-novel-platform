package com.sdjz.novel.controller.me;

import com.sdjz.novel.common.context.UserContext;
import com.sdjz.novel.common.exception.BusinessException;
import com.sdjz.novel.common.exception.ErrorCode;
import com.sdjz.novel.common.response.RestResp;
import com.sdjz.novel.pojo.dto.reader.CommentCreateDTO;
import com.sdjz.novel.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/front/me")
@RequiredArgsConstructor
public class MeCommentController {

    private final CommentService commentService;

    @PostMapping("/books/{bookId}/comments")
    public RestResp<Void> createComment(
            @PathVariable Long bookId,
            @Valid @RequestBody CommentCreateDTO dto
    ) {
        commentService.createComment(requireUserId(), bookId, dto);
        return RestResp.ok();
    }

    @DeleteMapping("/comments/{commentId}")
    public RestResp<Void> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(requireUserId(), commentId);
        return RestResp.ok();
    }

    private Long requireUserId() {
        Long userId = UserContext.getUid();
        if (userId == null) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRE, "登录过期或未登录");
        }
        return userId;
    }
}
