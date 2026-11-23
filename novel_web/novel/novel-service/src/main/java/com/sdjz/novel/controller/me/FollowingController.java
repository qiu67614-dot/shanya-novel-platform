package com.sdjz.novel.controller.me;

import com.sdjz.novel.common.context.UserContext;
import com.sdjz.novel.common.exception.BusinessException;
import com.sdjz.novel.common.exception.ErrorCode;
import com.sdjz.novel.common.response.RestResp;
import com.sdjz.novel.pojo.vo.reader.FollowingBookVO;
import com.sdjz.novel.service.FollowingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/front/me/following")
@RequiredArgsConstructor
public class FollowingController {

    private final FollowingService followingService;

    @GetMapping
    public RestResp<List<FollowingBookVO>> list() {
        return RestResp.ok(followingService.list(requireUserId()));
    }

    @PutMapping("/{bookId}")
    public RestResp<FollowingBookVO> follow(@PathVariable Long bookId) {
        return RestResp.ok(followingService.follow(requireUserId(), bookId));
    }

    @DeleteMapping("/{bookId}")
    public RestResp<Void> unfollow(@PathVariable Long bookId) {
        followingService.unfollow(requireUserId(), bookId);
        return RestResp.ok();
    }

    @PutMapping("/{bookId}/read")
    public RestResp<FollowingBookVO> markRead(@PathVariable Long bookId) {
        return RestResp.ok(followingService.markRead(requireUserId(), bookId));
    }

    private Long requireUserId() {
        Long userId = UserContext.getUid();
        if (userId == null) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRE, "登录过期或未登录");
        }
        return userId;
    }
}
