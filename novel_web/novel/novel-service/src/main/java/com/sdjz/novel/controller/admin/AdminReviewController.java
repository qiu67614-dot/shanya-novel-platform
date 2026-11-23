package com.sdjz.novel.controller.admin;

import com.sdjz.novel.common.context.AdminContext;
import com.sdjz.novel.common.response.RestResp;
import com.sdjz.novel.pojo.dto.reader.AdminDecisionDTO;
import com.sdjz.novel.pojo.entity.UserNovelChapter;
import com.sdjz.novel.pojo.vo.reader.AdminAuditLogVO;
import com.sdjz.novel.pojo.vo.reader.AdminSubmissionVO;
import com.sdjz.novel.pojo.vo.reader.PageVO;
import com.sdjz.novel.pojo.vo.reader.WithdrawalRequestVO;
import com.sdjz.novel.service.PublicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminReviewController {
    private final PublicationService service;

    @GetMapping("/submissions")
    public RestResp<PageVO<AdminSubmissionVO>> submissions(@RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        adminId();
        return RestResp.ok(service.submissions(status, page, size));
    }

    @GetMapping("/submissions/{id}")
    public RestResp<AdminSubmissionVO> submission(@PathVariable Long id) {
        adminId();
        return RestResp.ok(service.submission(id));
    }

    @GetMapping("/submissions/{id}/chapters")
    public RestResp<List<UserNovelChapter>> chapters(@PathVariable Long id) {
        adminId();
        return RestResp.ok(service.submissionChapters(id));
    }

    @GetMapping("/submissions/{id}/chapters/{chapterId}")
    public RestResp<UserNovelChapter> chapter(@PathVariable Long id, @PathVariable Long chapterId) {
        adminId();
        return RestResp.ok(service.submissionChapter(id, chapterId));
    }

    @PostMapping("/submissions/{id}/approve")
    public RestResp<Long> approve(@PathVariable Long id, @Valid @RequestBody AdminDecisionDTO decision) {
        return RestResp.ok(service.approve(adminId(), id, decision));
    }

    @PostMapping("/submissions/{id}/reject")
    public RestResp<Void> reject(@PathVariable Long id, @Valid @RequestBody AdminDecisionDTO decision) {
        service.reject(adminId(), id, decision.getReason());
        return RestResp.ok();
    }

    @GetMapping("/withdrawals")
    public RestResp<PageVO<WithdrawalRequestVO>> withdrawals(@RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        adminId();
        return RestResp.ok(service.withdrawals(status, page, size));
    }

    @PostMapping("/withdrawals/{id}/approve")
    public RestResp<Void> approveWithdrawal(@PathVariable Long id) {
        service.reviewWithdrawal(adminId(), id, true, null);
        return RestResp.ok();
    }

    @PostMapping("/withdrawals/{id}/reject")
    public RestResp<Void> rejectWithdrawal(@PathVariable Long id, @Valid @RequestBody AdminDecisionDTO decision) {
        service.reviewWithdrawal(adminId(), id, false, decision.getReason());
        return RestResp.ok();
    }

    @PostMapping("/publications/{bookId}/take-down")
    public RestResp<Void> takeDown(@PathVariable Long bookId, @Valid @RequestBody AdminDecisionDTO decision) {
        service.takeDown(adminId(), bookId, decision.getReason());
        return RestResp.ok();
    }

    @GetMapping("/audit-logs")
    public RestResp<PageVO<AdminAuditLogVO>> auditLogs(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        adminId();
        return RestResp.ok(service.auditLogs(page, size));
    }

    private Long adminId() {
        Long id = AdminContext.getAdminId();
        service.requireAdmin(id);
        return id;
    }
}
