package com.sdjz.novel.service;

import com.sdjz.novel.common.exception.BusinessException;
import com.sdjz.novel.common.util.AdminJwtUtil;
import com.sdjz.novel.common.util.JsonRedisCache;
import com.sdjz.novel.mapper.PrivateNovelMapper;
import com.sdjz.novel.mapper.PublicationMapper;
import com.sdjz.novel.pojo.dto.reader.AdminDecisionDTO;
import com.sdjz.novel.pojo.dto.reader.SubmissionUpdateDTO;
import com.sdjz.novel.pojo.entity.NovelSubmission;
import com.sdjz.novel.pojo.entity.BookInfo;
import com.sdjz.novel.pojo.vo.reader.AdminSubmissionVO;
import com.sdjz.novel.pojo.vo.reader.PrivateNovelVO;
import com.sdjz.novel.service.imports.PrivateNovelTextParser;
import com.sdjz.novel.service.storage.OssCoverStorage;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PublicationServiceTest {
    private final PublicationMapper mapper = mock(PublicationMapper.class);
    private final PrivateNovelMapper privateMapper = mock(PrivateNovelMapper.class);
    private final OssCoverStorage coverStorage = mock(OssCoverStorage.class);
    private final PublicationService service = new PublicationService(mapper, privateMapper,
            new PrivateNovelTextParser(), mock(BCryptPasswordEncoder.class),
            mock(AdminJwtUtil.class), mock(JsonRedisCache.class), coverStorage);

    @Test
    void approvedCoverBecomesPublicBookCover() {
        AdminSubmissionVO submission = new AdminSubmissionVO();
        submission.setStatus("PENDING");
        submission.setUserNovelId(7L);
        submission.setChapterCount(1);
        submission.setWordCount(100L);
        submission.setRightsConfirmed(true);
        submission.setBookName("测试作品");
        submission.setAuthorName("测试作者");
        submission.setCategoryId(3L);
        submission.setPicUrl("https://example.oss-cn-hangzhou.aliyuncs.com/covers/cover.jpg");
        when(coverStorage.publish(submission.getPicUrl())).thenReturn("https://example.oss-cn-hangzhou.aliyuncs.com/covers/public/cover.jpg");
        when(mapper.lockAdminSubmission(5L)).thenReturn(submission);
        when(mapper.countEnabledCategory(3L)).thenReturn(1);
        doAnswer(invocation -> { invocation.getArgument(0, BookInfo.class).setId(55L); return 1; })
                .when(mapper).insertBookInfo(any());
        when(mapper.copyPublicChapters(7L, 55L)).thenReturn(1);
        when(mapper.copyPublicContents(7L, 55L)).thenReturn(1);
        when(mapper.updatePublicBookSummary(55L)).thenReturn(1);
        when(mapper.approveSubmission(5L, 1L, 55L)).thenReturn(1);
        TransactionSynchronizationManager.initSynchronization();
        try {
            assertEquals(55L, service.approve(1L, 5L, new AdminDecisionDTO()));
            var book = org.mockito.ArgumentCaptor.forClass(BookInfo.class);
            verify(mapper).insertBookInfo(book.capture());
            assertEquals("https://example.oss-cn-hangzhou.aliyuncs.com/covers/public/cover.jpg", book.getValue().getPicUrl());
        } finally { TransactionSynchronizationManager.clearSynchronization(); }
    }

    @Test
    void withdrawalRequiresOwnershipAndDoesNotDuplicatePendingRequest() {
        assertCode("B0003", () -> service.requestWithdrawal(2L, 10L, null));
        verify(mapper, never()).insertWithdrawal(anyLong(), any(), anyLong(), any());

        NovelSubmission owned = new NovelSubmission();
        owned.setUserNovelId(7L);
        when(mapper.lockPublicBook(10L)).thenReturn(0);
        when(mapper.lockActivePublication(1L, 10L)).thenReturn(owned);
        when(mapper.countPendingWithdrawal(1L, 10L)).thenReturn(1);
        assertCode("A0400", () -> service.requestWithdrawal(1L, 10L, null));
        verify(mapper, never()).insertWithdrawal(anyLong(), any(), anyLong(), any());
    }

    @Test
    void approvalDoesNotPublishWhenChapterCopyIsIncomplete() {
        AdminSubmissionVO submission = new AdminSubmissionVO();
        submission.setStatus("PENDING");
        submission.setUserNovelId(7L);
        submission.setChapterCount(2);
        submission.setWordCount(100L);
        submission.setRightsConfirmed(true);
        submission.setBookName("测试作品");
        submission.setAuthorName("测试作者");
        submission.setCategoryId(3L);
        when(mapper.lockAdminSubmission(5L)).thenReturn(submission);
        when(mapper.countEnabledCategory(3L)).thenReturn(1);
        when(mapper.copyPublicChapters(eq(7L), any())).thenReturn(1);

        assertCode("B0000", () -> service.approve(1L, 5L, new AdminDecisionDTO()));
        verify(mapper, never()).approveSubmission(anyLong(), anyLong(), any());
        verify(mapper, never()).insertAudit(anyLong(), anyString(), anyLong(), anyString(), any());
    }

    @Test
    void replacingRejectedTxtResetsProgressBeforeWritingNewChapters() {
        NovelSubmission submission = new NovelSubmission();
        submission.setId(5L);
        submission.setUserId(1L);
        submission.setStatus("REJECTED");
        when(mapper.lockSubmissionByNovel(1L, 7L)).thenReturn(submission);
        when(mapper.countEnabledCategory(3L)).thenReturn(1);
        when(mapper.updateSubmissionForResubmit(submission)).thenReturn(1);
        PrivateNovelVO result = new PrivateNovelVO();
        when(privateMapper.selectNovel(1L, 7L)).thenReturn(result);
        SubmissionUpdateDTO dto = new SubmissionUpdateDTO();
        dto.setBookName("重投作品");
        dto.setAuthorName("作者");
        dto.setCategoryId(3L);
        dto.setRightsConfirmed(true);
        MockMultipartFile file = new MockMultipartFile("file", "novel.txt", "text/plain",
                "第一章 开端\n这是替换后的正文。".getBytes(StandardCharsets.UTF_8));

        assertSame(result, service.resubmit(1L, 7L, dto, file));
        var order = inOrder(privateMapper);
        order.verify(privateMapper).deleteProgress(1L, 7L);
        order.verify(privateMapper).updateNovelMeta(eq(1L), eq(7L), anyString(), anyString(),
                anyString(), anyLong(), anyLong(), anyInt(), isNull());
        order.verify(privateMapper).deleteChapters(1L, 7L);
        order.verify(privateMapper, atLeastOnce()).insertChapter(any());
    }

    @Test
    void resubmitDoesNotRevealAnotherUsersNovel() {
        SubmissionUpdateDTO dto = new SubmissionUpdateDTO();
        assertCode("B0003", () -> service.resubmit(2L, 7L, dto, null));
        verify(privateMapper, never()).deleteChapters(anyLong(), anyLong());
    }

    private void assertCode(String code, Runnable action) {
        BusinessException error = assertThrows(BusinessException.class, action::run);
        assertEquals(code, error.getCode());
    }
}
