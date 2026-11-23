package com.sdjz.novel.controller.me;

import com.sdjz.novel.common.context.UserContext;
import com.sdjz.novel.common.exception.BusinessException;
import com.sdjz.novel.common.exception.ErrorCode;
import com.sdjz.novel.common.exception.RateLimitException;
import com.sdjz.novel.common.ratelimit.RateLimit;
import com.sdjz.novel.common.ratelimit.RateLimitDimension;
import com.sdjz.novel.common.ratelimit.RateLimitGroup;
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

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/front/me/imports")
@RequiredArgsConstructor
public class PrivateNovelController {

    /** 同一时刻允许几个导入在跑：一个导入 = 20 MB 堆内存 + 一条数据库连接 + 一轮 CPU 解析 */
    private static final int IMPORT_CONCURRENCY = 2;
    private static final long IMPORT_WAIT_SECONDS = 1;

    private final PrivateNovelService service;

    /** 进程级闸门。限流注解管的是「一个人调多快」，这里管的是「同时有几个在跑」。 */
    private final Semaphore importGate = new Semaphore(IMPORT_CONCURRENCY);

    /**
     * TXT 导入：本项目最重的接口（20 MB 全堆解析 + 最多 5000 条章节插入）。
     *
     * <p>两道保护叠加：{@code @RateLimit} 按用户 3 次/小时挡住「一个人反复导」，
     * 信号量挡住「N 个不同用户同时导」。后者不能靠 IP 或用户维度限流解决——
     * 每个用户都只导了一次，但合起来仍可能占满连接池。
     */
    @RateLimit(group = RateLimitGroup.HEAVY, dimension = RateLimitDimension.USER)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RestResp<PrivateNovelVO> importTxt(@RequestParam(value = "file", required = false) MultipartFile file,
                                              @RequestParam(value = "cover", required = false) MultipartFile cover,
                                              @ModelAttribute PrivateImportDTO options) {
        if (!tryEnter()) {
            throw new RateLimitException("当前导入任务较多，请稍后重试", 5);
        }
        try {
            return RestResp.ok(service.importTxt(userId(), file, options, cover));
        } finally {
            importGate.release();
        }
    }

    /** 排队最多等 1 秒。等不到就快速失败，不让 Tomcat 线程在这里堆积。 */
    private boolean tryEnter() {
        try {
            return importGate.tryAcquire(IMPORT_WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
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
