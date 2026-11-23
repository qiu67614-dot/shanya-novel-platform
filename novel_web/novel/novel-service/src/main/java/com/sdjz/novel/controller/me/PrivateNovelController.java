package com.sdjz.novel.controller.me;

import com.sdjz.novel.common.context.UserContext;
import com.sdjz.novel.common.exception.BusinessException;
import com.sdjz.novel.common.exception.ErrorCode;
import com.sdjz.novel.common.response.RestResp;
import com.sdjz.novel.pojo.dto.reader.PrivateProgressDTO;
import com.sdjz.novel.pojo.dto.reader.PrivateImportDTO;
import com.sdjz.novel.pojo.vo.reader.ChapterContentVO;
import com.sdjz.novel.pojo.vo.reader.ChapterSummaryVO;
import com.sdjz.novel.pojo.vo.reader.PageVO;
import com.sdjz.novel.pojo.vo.reader.PrivateNovelVO;
import com.sdjz.novel.service.PrivateNovelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/front/me/imports")
@RequiredArgsConstructor
public class PrivateNovelController {
    private final PrivateNovelService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RestResp<PrivateNovelVO> importTxt(@RequestParam(value = "file", required = false) MultipartFile file,
                                              @RequestParam(value = "cover", required = false) MultipartFile cover,
                                              @ModelAttribute PrivateImportDTO options) {
        return RestResp.ok(service.importTxt(userId(), file, options, cover));
    }

    @GetMapping
    public RestResp<PageVO<PrivateNovelVO>> getPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return RestResp.ok(service.getPage(userId(), page, size));
    }

    @GetMapping("/{novelId}")
    public RestResp<PrivateNovelVO> getNovel(@PathVariable Long novelId) {
        return RestResp.ok(service.getNovel(userId(), novelId));
    }

    @GetMapping("/{novelId}/chapters")
    public RestResp<PageVO<ChapterSummaryVO>> getChapters(
            @PathVariable Long novelId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int size) {
        return RestResp.ok(service.getChapters(userId(), novelId, page, size));
    }

    @GetMapping("/{novelId}/chapters/{chapterId}")
    public RestResp<ChapterContentVO> getChapter(@PathVariable Long novelId,
                                                   @PathVariable Long chapterId) {
        return RestResp.ok(service.getChapter(userId(), novelId, chapterId));
    }

    @PutMapping("/{novelId}/progress")
    public RestResp<Void> updateProgress(@PathVariable Long novelId,
                                         @Valid @RequestBody PrivateProgressDTO dto) {
        service.updateProgress(userId(), novelId, dto);
        return RestResp.ok();
    }

    @DeleteMapping("/{novelId}")
    public RestResp<Void> delete(@PathVariable Long novelId) {
        service.delete(userId(), novelId);
        return RestResp.ok();
    }

    private Long userId() {
        Long userId = UserContext.getUid();
        if (userId == null) throw new BusinessException(ErrorCode.TOKEN_EXPIRE, "登录过期或未登录");
        return userId;
    }
}
