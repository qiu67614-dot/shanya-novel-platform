package com.sdjz.novel.controller.me;

import com.sdjz.novel.common.context.UserContext;
import com.sdjz.novel.common.exception.BusinessException;
import com.sdjz.novel.common.exception.ErrorCode;
import com.sdjz.novel.common.response.RestResp;
import com.sdjz.novel.pojo.dto.reader.BookshelfProgressDTO;
import com.sdjz.novel.pojo.vo.reader.BookshelfEntryVO;
import com.sdjz.novel.service.BookshelfService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/front/me/bookshelf")
@RequiredArgsConstructor
public class BookshelfController {

    @Autowired
    private final BookshelfService bookshelfService;



    /**
     * 获取书架
     */
    @GetMapping
    public RestResp<List<BookshelfEntryVO>> list() {
        return RestResp.ok(
                bookshelfService.list(requireUserId())
        );
    }
    /**
     * 加入书架或更新书架进度
     */
    @PutMapping("/{bookId}")
    public RestResp<BookshelfEntryVO> save(
            @PathVariable Long bookId,
            @Valid @RequestBody(required = false)
            BookshelfProgressDTO dto
    ) {
        return RestResp.ok(
                bookshelfService.save(
                        requireUserId(),
                        bookId,
                        dto
                )
        );
    }
    /**
     * 移除书架
     * */
    @DeleteMapping("/{bookId}")
    public RestResp<Void> remove(@PathVariable Long bookId) {
        bookshelfService.remove(requireUserId(), bookId);
        return RestResp.ok();
    }

    private Long requireUserId() {
        Long userId = UserContext.getUid();

        if (userId == null) {
            throw new BusinessException(
                    ErrorCode.TOKEN_EXPIRE,
                    "登录过期或未登录"
            );
        }

        return userId;
    }
}