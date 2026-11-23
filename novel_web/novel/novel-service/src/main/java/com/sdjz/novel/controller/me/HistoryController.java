package com.sdjz.novel.controller.me;

import com.sdjz.novel.common.context.UserContext;
import com.sdjz.novel.common.exception.BusinessException;
import com.sdjz.novel.common.exception.ErrorCode;
import com.sdjz.novel.common.response.RestResp;
import com.sdjz.novel.pojo.dto.reader.ProgressUpdateDTO;
import com.sdjz.novel.pojo.vo.reader.HistoryEntryVO;
import com.sdjz.novel.pojo.vo.reader.PageVO;
import com.sdjz.novel.service.HistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/front/me")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    @GetMapping("/history")
    public RestResp<PageVO<HistoryEntryVO>> getHistory(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        return RestResp.ok(historyService.getHistory(requireUserId(), page, size));
    }

    @PutMapping("/progress")
    public RestResp<Void> updateProgress(
            @Valid @RequestBody ProgressUpdateDTO dto
    ) {
        historyService.updateProgress(requireUserId(), dto);
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
