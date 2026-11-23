package com.sdjz.novel.controller.me;

import com.sdjz.novel.common.context.UserContext;
import com.sdjz.novel.common.exception.BusinessException;
import com.sdjz.novel.common.exception.ErrorCode;
import com.sdjz.novel.common.response.RestResp;
import com.sdjz.novel.pojo.dto.reader.SubmissionUpdateDTO;
import com.sdjz.novel.pojo.dto.reader.WithdrawalDTO;
import com.sdjz.novel.pojo.vo.reader.PrivateNovelVO;
import com.sdjz.novel.service.PublicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/front/me")
@RequiredArgsConstructor
public class MePublicationController {
    private final PublicationService service;

    @PostMapping(value = "/imports/{novelId}/resubmit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RestResp<PrivateNovelVO> resubmit(@PathVariable Long novelId,
            @Valid @ModelAttribute SubmissionUpdateDTO dto,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "cover", required = false) MultipartFile cover) {
        return RestResp.ok(service.resubmit(userId(), novelId, dto, file, cover));
    }

    @PostMapping("/imports/{novelId}/cancel-submission")
    public RestResp<Void> cancel(@PathVariable Long novelId) {
        service.cancelSubmission(userId(), novelId);
        return RestResp.ok();
    }

    @GetMapping("/publications")
    public RestResp<List<PrivateNovelVO>> publications() {
        return RestResp.ok(service.getPublications(userId()));
    }

    @PostMapping("/publications/{bookId}/withdrawals")
    public RestResp<Void> requestWithdrawal(@PathVariable Long bookId,
            @Valid @RequestBody(required = false) WithdrawalDTO dto) {
        service.requestWithdrawal(userId(), bookId, dto == null ? null : dto.getReason());
        return RestResp.ok();
    }

    private Long userId() {
        Long id = UserContext.getUid();
        if (id == null) throw new BusinessException(ErrorCode.TOKEN_EXPIRE, "登录过期或未登录");
        return id;
    }
}
