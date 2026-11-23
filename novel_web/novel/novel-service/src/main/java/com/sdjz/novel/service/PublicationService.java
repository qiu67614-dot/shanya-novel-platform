package com.sdjz.novel.service;

import com.sdjz.novel.common.exception.BusinessException;
import com.sdjz.novel.common.exception.ErrorCode;
import com.sdjz.novel.common.util.AdminJwtUtil;
import com.sdjz.novel.common.util.JsonRedisCache;
import com.sdjz.novel.mapper.PrivateNovelMapper;
import com.sdjz.novel.mapper.PublicationMapper;
import com.sdjz.novel.pojo.dto.reader.AdminDecisionDTO;
import com.sdjz.novel.pojo.dto.reader.PrivateImportDTO;
import com.sdjz.novel.pojo.dto.reader.SubmissionUpdateDTO;
import com.sdjz.novel.pojo.dto.user.AdminLoginDTO;
import com.sdjz.novel.pojo.entity.AdminUser;
import com.sdjz.novel.pojo.entity.BookInfo;
import com.sdjz.novel.pojo.entity.NovelSubmission;
import com.sdjz.novel.pojo.entity.UserNovelChapter;
import com.sdjz.novel.pojo.vo.reader.AdminAuditLogVO;
import com.sdjz.novel.pojo.vo.reader.AdminSubmissionVO;
import com.sdjz.novel.pojo.vo.reader.PageVO;
import com.sdjz.novel.pojo.vo.reader.PrivateNovelVO;
import com.sdjz.novel.pojo.vo.reader.WithdrawalRequestVO;
import com.sdjz.novel.pojo.vo.user.AdminLoginVO;
import com.sdjz.novel.service.imports.PrivateNovelTextParser;
import com.sdjz.novel.service.imports.PrivateNovelTextParser.ParsedChapter;
import com.sdjz.novel.service.imports.PrivateNovelTextParser.ParsedNovel;
import com.sdjz.novel.service.storage.OssCoverStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PublicationService implements ApplicationRunner {
    private static final Set<String> SUBMISSION_STATUSES = Set.of("PENDING", "REJECTED", "APPROVED", "CANCELLED");
    private static final Set<String> WITHDRAWAL_STATUSES = Set.of("PENDING", "APPROVED", "REJECTED", "CLOSED");
    private final PublicationMapper mapper;
    private final PrivateNovelMapper privateMapper;
    private final PrivateNovelTextParser parser;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AdminJwtUtil adminJwtUtil;
    private final JsonRedisCache readerCache;
    private final OssCoverStorage coverStorage;

    @Value("${admin.bootstrap.username:}")
    private String bootstrapUsername;
    @Value("${admin.bootstrap.password:}")
    private String bootstrapPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (bootstrapUsername == null || bootstrapUsername.isBlank()
                || bootstrapPassword == null || bootstrapPassword.isBlank()) return;
        if (mapper.selectAdminByUsername(bootstrapUsername) != null) return;
        AdminUser admin = new AdminUser();
        admin.setUsername(bootstrapUsername);
        admin.setPasswordHash(passwordEncoder.encode(bootstrapPassword));
        mapper.insertAdmin(admin);
    }

    public AdminLoginVO login(AdminLoginDTO dto) {
        AdminUser admin = mapper.selectAdminByUsername(dto.getUsername());
        if (admin == null || admin.getStatus() != 0
                || !passwordEncoder.matches(dto.getPassword(), admin.getPasswordHash())) {
            throw new BusinessException(ErrorCode.LOGIN_ERROR, "管理员账号或密码错误");
        }
        mapper.touchAdminLogin(admin.getId());
        return new AdminLoginVO(adminJwtUtil.generateToken(admin.getId()), admin.getId(), admin.getUsername());
    }

    public void requireAdmin(Long adminId) {
        AdminUser admin = adminId == null ? null : mapper.selectAdminById(adminId);
        if (admin == null || admin.getStatus() != 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无管理员权限");
        }
    }

    public void validatePublicFields(String bookName, String authorName, Long categoryId, Boolean rightsConfirmed) {
        if (bookName == null || bookName.isBlank() || bookName.length() > 128
                || authorName == null || authorName.isBlank() || authorName.length() > 64
                || categoryId == null || mapper.countEnabledCategory(categoryId) == 0
                || !Boolean.TRUE.equals(rightsConfirmed)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请填写书名、笔名、有效分类并确认发布权");
        }
    }

    public void createSubmission(Long userId, Long novelId, PrivateImportDTO dto) {
        createSubmission(userId, novelId, dto, null);
    }

    public void createSubmission(Long userId, Long novelId, PrivateImportDTO dto, String coverUrl) {
        validatePublicFields(dto.getBookName(), dto.getAuthorName(), dto.getCategoryId(), dto.getRightsConfirmed());
        NovelSubmission submission = new NovelSubmission();
        submission.setUserId(userId);
        submission.setUserNovelId(novelId);
        submission.setBookName(dto.getBookName().trim());
        submission.setAuthorName(dto.getAuthorName().trim());
        submission.setCategoryId(dto.getCategoryId());
        submission.setBookDesc(dto.getBookDesc());
        submission.setCoverUrl(coverUrl);
        submission.setRightsConfirmed(true);
        mapper.insertSubmission(submission);
    }

    public List<PrivateNovelVO> getPublications(Long userId) {
        return mapper.selectPublications(userId);
    }

    @Transactional
    public PrivateNovelVO resubmit(Long userId, Long novelId, SubmissionUpdateDTO dto, MultipartFile file) {
        return resubmit(userId, novelId, dto, file, null);
    }

    @Transactional
    public PrivateNovelVO resubmit(Long userId, Long novelId, SubmissionUpdateDTO dto, MultipartFile file, MultipartFile cover) {
        NovelSubmission submission = mapper.lockSubmissionByNovel(userId, novelId);
        if (submission == null || !("REJECTED".equals(submission.getStatus())
                || "CANCELLED".equals(submission.getStatus())) || submission.getPublicBookId() != null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "作品不可重新投稿");
        }
        validatePublicFields(dto.getBookName(), dto.getAuthorName(), dto.getCategoryId(), dto.getRightsConfirmed());
        if (file != null && !file.isEmpty()) {
            ParsedNovel parsed = parseFile(file);
            privateMapper.deleteProgress(userId, novelId);
            privateMapper.updateNovelMeta(userId, novelId, parsed.bookName(), parsed.originalFileName(),
                    parsed.encoding(), parsed.fileSize(), parsed.wordCount(), parsed.chapters().size(), null);
            privateMapper.deleteChapters(userId, novelId);
            Long firstChapterId = null;
            int chapterNum = 1;
            for (ParsedChapter parsedChapter : parsed.chapters()) {
                UserNovelChapter chapter = new UserNovelChapter();
                chapter.setNovelId(novelId);
                chapter.setChapterNum(chapterNum++);
                chapter.setChapterName(parsedChapter.name());
                chapter.setContent(parsedChapter.content());
                chapter.setWordCount(parsedChapter.wordCount());
                privateMapper.insertChapter(chapter);
                if (firstChapterId == null) firstChapterId = chapter.getId();
            }
            privateMapper.setFirstChapter(novelId, firstChapterId);
        } else if (file != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "替换文件不能为空");
        }
        submission.setBookName(dto.getBookName().trim());
        submission.setAuthorName(dto.getAuthorName().trim());
        submission.setCategoryId(dto.getCategoryId());
        submission.setBookDesc(dto.getBookDesc());
        if (cover != null) {
            submission.setCoverUrl(coverStorage.upload(cover));
            privateMapper.updateCover(userId, novelId, submission.getCoverUrl());
        }
        submission.setRightsConfirmed(true);
        if (mapper.updateSubmissionForResubmit(submission) != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "投稿状态已变化，请刷新重试");
        }
        PrivateNovelVO result = privateMapper.selectNovel(userId, novelId);
        if (result != null && result.getPicUrl() != null) result.setPicUrl(coverStorage.signedUrl(result.getPicUrl()));
        return result;
    }

    private ParsedNovel parseFile(MultipartFile file) {
        if (file.getSize() > PrivateNovelTextParser.MAX_BYTES) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件不能超过 20 MB");
        }
        try {
            return parser.parse(file.getOriginalFilename(), file.getBytes());
        } catch (IOException ex) {
            throw new BusinessException("B0000", "读取上传文件失败");
        }
    }

    @Transactional
    public void cancelSubmission(Long userId, Long novelId) {
        NovelSubmission submission = mapper.lockSubmissionByNovel(userId, novelId);
        if (submission == null || mapper.cancelSubmission(userId, novelId) != 1) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "待审核投稿不存在");
        }
    }

    @Transactional
    public void onPrivateDelete(Long userId, Long novelId) {
        NovelSubmission submission = mapper.lockSubmissionByNovel(userId, novelId);
        if (submission != null && "PENDING".equals(submission.getStatus())) {
            mapper.cancelSubmissionForDeletedNovel(userId, novelId);
        }
    }

    @Transactional
    public void requestWithdrawal(Long userId, Long bookId, String reason) {
        if (reason != null && reason.length() > 500) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "撤回原因不能超过 500 字");
        }
        if (!Integer.valueOf(0).equals(mapper.lockPublicBook(bookId))) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "公开作品不存在");
        }
        NovelSubmission publication = mapper.lockActivePublication(userId, bookId);
        if (publication == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "公开作品不存在");
        if (mapper.countPendingWithdrawal(userId, bookId) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "撤回申请已提交");
        }
        mapper.insertWithdrawal(userId, publication.getUserNovelId(), bookId, reason == null ? null : reason.trim());
    }

    public PageVO<AdminSubmissionVO> submissions(String status, int page, int size) {
        validateStatus(status, SUBMISSION_STATUSES);
        int p = Math.max(page, 1), s = normalizeSize(size);
        List<AdminSubmissionVO> entries = mapper.selectSubmissionPage(status, (long) (p - 1) * s, s);
        entries.forEach(this::signCover);
        return new PageVO<>(entries, p, s, mapper.countSubmissions(status));
    }

    public AdminSubmissionVO submission(Long id) {
        AdminSubmissionVO result = mapper.selectAdminSubmission(id);
        if (result == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "投稿不存在");
        signCover(result);
        return result;
    }

    public List<UserNovelChapter> submissionChapters(Long id) {
        AdminSubmissionVO result = submission(id);
        if (result.getUserNovelId() == null) return List.of();
        return mapper.selectAdminChapters(id);
    }

    public UserNovelChapter submissionChapter(Long id, Long chapterId) {
        submission(id);
        UserNovelChapter chapter = mapper.selectAdminChapter(id, chapterId);
        if (chapter == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "章节不存在");
        return chapter;
    }

    @Transactional
    public void reject(Long adminId, Long id, String reason) {
        AdminSubmissionVO submission = mapper.lockAdminSubmission(id);
        if (submission == null || !"PENDING".equals(submission.getStatus())) invalidState();
        if (reason == null || reason.isBlank() || reason.length() > 500) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请填写不超过 500 字的退稿原因");
        }
        mapper.rejectSubmission(id, adminId, reason.trim());
        mapper.insertAudit(adminId, "SUBMISSION", id, "REJECT", reason.trim());
    }

    @Transactional
    public Long approve(Long adminId, Long id, AdminDecisionDTO decision) {
        AdminSubmissionVO submission = mapper.lockAdminSubmission(id);
        if (submission == null || !"PENDING".equals(submission.getStatus())
                || submission.getUserNovelId() == null || submission.getChapterCount() == null
                || submission.getChapterCount() < 1 || !Boolean.TRUE.equals(submission.getRightsConfirmed())) invalidState();
        String bookName = decision.getBookName() == null ? submission.getBookName() : decision.getBookName().trim();
        String authorName = decision.getAuthorName() == null ? submission.getAuthorName() : decision.getAuthorName().trim();
        Long categoryId = decision.getCategoryId() == null ? submission.getCategoryId() : decision.getCategoryId();
        String bookDesc = decision.getBookDesc() == null ? submission.getBookDesc() : decision.getBookDesc();
        validatePublicFields(bookName, authorName, categoryId, true);
        mapper.updateSubmissionMetadata(id, bookName, authorName, categoryId, bookDesc);
        BookInfo book = new BookInfo();
        book.setBookName(bookName);
        book.setAuthorName(authorName);
        book.setCategoryId(categoryId);
        book.setBookDesc(bookDesc);
        book.setPicUrl(coverStorage.publish(submission.getPicUrl()));
        book.setWordCount(submission.getWordCount());
        book.setChapterCount(submission.getChapterCount());
        mapper.insertBookInfo(book);
        int copied = mapper.copyPublicChapters(submission.getUserNovelId(), book.getId());
        if (copied != submission.getChapterCount() || mapper.copyPublicContents(submission.getUserNovelId(), book.getId()) != copied
                || mapper.updatePublicBookSummary(book.getId()) != 1
                || mapper.approveSubmission(id, adminId, book.getId()) != 1) {
            throw new BusinessException("B0000", "发布章节失败，已回滚");
        }
        mapper.insertAudit(adminId, "SUBMISSION", id, "APPROVE", null);
        evictPublicAfterCommit();
        return book.getId();
    }

    public PageVO<WithdrawalRequestVO> withdrawals(String status, int page, int size) {
        validateStatus(status, WITHDRAWAL_STATUSES);
        int p = Math.max(page, 1), s = normalizeSize(size);
        return new PageVO<>(mapper.selectWithdrawalPage(status, (long) (p - 1) * s, s), p, s,
                mapper.countWithdrawals(status));
    }

    @Transactional
    public void reviewWithdrawal(Long adminId, Long requestId, boolean approve, String reason) {
        WithdrawalRequestVO existing = mapper.selectWithdrawal(requestId);
        if (existing == null || mapper.lockPublicBook(existing.getPublicBookId()) == null) invalidState();
        WithdrawalRequestVO request = mapper.lockWithdrawal(requestId);
        if (request == null || !"PENDING".equals(request.getStatus())) invalidState();
        if (!approve && (reason == null || reason.isBlank() || reason.length() > 500)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请填写不超过 500 字的驳回原因");
        }
        int rows = approve ? mapper.approveWithdrawal(requestId, adminId)
                : mapper.rejectWithdrawal(requestId, adminId, reason.trim());
        if (rows != 1) invalidState();
        if (approve) {
            if (mapper.takeDownPublication(request.getPublicBookId()) != 1
                    || mapper.setSubmissionPublicStatus(request.getPublicBookId(), "WITHDRAWN") != 1) invalidState();
            evictPublicAfterCommit();
        }
        mapper.insertAudit(adminId, "WITHDRAWAL", requestId, approve ? "APPROVE" : "REJECT",
                approve ? null : reason.trim());
    }

    @Transactional
    public void takeDown(Long adminId, Long bookId, String reason) {
        if (reason == null || reason.isBlank() || reason.length() > 500) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请填写不超过 500 字的下架原因");
        }
        if (mapper.lockPublicBook(bookId) == null || mapper.takeDownPublication(bookId) != 1) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "已发布作品不存在");
        }
        mapper.closePendingWithdrawals(bookId, adminId);
        if (mapper.setSubmissionPublicStatus(bookId, "TAKEN_DOWN") != 1) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "已发布作品不存在");
        }
        mapper.insertAudit(adminId, "PUBLIC_BOOK", bookId, "TAKE_DOWN", reason.trim());
        evictPublicAfterCommit();
    }

    public PageVO<AdminAuditLogVO> auditLogs(int page, int size) {
        int p = Math.max(page, 1), s = normalizeSize(size);
        return new PageVO<>(mapper.selectAuditPage((long) (p - 1) * s, s), p, s, mapper.countAudit());
    }

    private void evictPublicAfterCommit() {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                readerCache.evict("home");
                readerCache.evict("ranks:visit");
                readerCache.evict("ranks:update");
                readerCache.evict("ranks:new");
            }
        });
    }

    private static int normalizeSize(int size) { return size < 1 ? 20 : Math.min(size, 100); }
    private void signCover(AdminSubmissionVO submission) {
        if (submission.getPicUrl() != null) submission.setPicUrl(coverStorage.signedUrl(submission.getPicUrl()));
    }
    private static void validateStatus(String status, Set<String> allowed) {
        if (status != null && !status.isBlank() && !allowed.contains(status)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "状态参数不合法");
        }
    }
    private static void invalidState() {
        throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "记录不存在或状态已变化");
    }
}
