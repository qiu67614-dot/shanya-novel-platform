package com.sdjz.novel.service.imp;

import com.sdjz.novel.common.exception.BusinessException;
import com.sdjz.novel.mapper.PrivateNovelMapper;
import com.sdjz.novel.pojo.dto.reader.PrivateProgressDTO;
import com.sdjz.novel.pojo.vo.reader.PrivateNovelVO;
import com.sdjz.novel.pojo.dto.reader.PrivateImportDTO;
import com.sdjz.novel.pojo.entity.UserNovel;
import com.sdjz.novel.pojo.entity.UserNovelChapter;
import com.sdjz.novel.service.PublicationService;
import com.sdjz.novel.service.imports.PrivateNovelTextParser;
import com.sdjz.novel.service.storage.OssCoverStorage;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class PrivateNovelServiceImplTest {
    private final PrivateNovelMapper mapper = mock(PrivateNovelMapper.class);
    private final PublicationService publicationService = mock(PublicationService.class);
    private final OssCoverStorage coverStorage = mock(OssCoverStorage.class);
    private final TransactionTemplate transactionTemplate =
            new TransactionTemplate(mock(PlatformTransactionManager.class));
    private final PrivateNovelServiceImpl service = new PrivateNovelServiceImpl(mapper,
            new PrivateNovelTextParser(), publicationService, coverStorage, transactionTemplate);

    @Test
    void publicImportCarriesUploadedCoverIntoSubmission() {
        MockMultipartFile txt = new MockMultipartFile("file", "novel.txt", "text/plain",
                "第一章 开端\n正文。".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile cover = new MockMultipartFile("cover", "cover.jpg", "image/jpeg", new byte[]{1});
        PrivateImportDTO options = new PrivateImportDTO();
        options.setVisibility("PUBLIC");
        options.setBookName("作品");
        options.setAuthorName("作者");
        options.setCategoryId(3L);
        options.setRightsConfirmed(true);
        when(coverStorage.upload(cover)).thenReturn("https://example.com/covers/cover.jpg");
        doAnswer(invocation -> { invocation.getArgument(0, UserNovel.class).setId(9L); return 1; })
                .when(mapper).insertNovel(any());
        // 批量插入后主键回填，第一批的第一个就是首章
        when(mapper.insertChapters(anyList())).thenAnswer(invocation -> {
            List<UserNovelChapter> batch = invocation.getArgument(0);
            batch.get(0).setId(100L);
            return batch.size();
        });
        when(mapper.selectNovel(1L, 9L)).thenReturn(new PrivateNovelVO());

        service.importTxt(1L, txt, options, cover);

        var novel = org.mockito.ArgumentCaptor.forClass(UserNovel.class);
        verify(mapper).insertNovel(novel.capture());
        assertEquals("https://example.com/covers/cover.jpg", novel.getValue().getCoverUrl());
        verify(mapper).insertChapters(anyList());
        verify(mapper).setFirstChapter(9L, 100L);
        verify(publicationService).createSubmission(1L, 9L, options, novel.getValue().getCoverUrl());
    }

    @Test
    void anotherUsersNovelIsInvisibleToDetailChapterAndProgress() {
        when(mapper.selectNovel(2L, 10L)).thenReturn(null);
        assertNotFound(() -> service.getNovel(2L, 10L));
        assertNotFound(() -> service.getChapter(2L, 10L, 100L));
        PrivateProgressDTO progress = new PrivateProgressDTO();
        progress.setChapterId(100L);
        assertNotFound(() -> service.updateProgress(2L, 10L, progress));
        verify(mapper, never()).selectChapter(anyLong(), anyLong(), anyLong());
        verify(mapper, never()).upsertProgress(anyLong(), anyLong(), anyLong());
    }

    @Test
    void rejectsChapterFromDifferentNovel() {
        PrivateNovelVO novel = new PrivateNovelVO();
        novel.setId(10L);
        when(mapper.selectNovel(1L, 10L)).thenReturn(novel);
        assertNotFound(() -> service.getChapter(1L, 10L, 200L));
        PrivateProgressDTO progress = new PrivateProgressDTO();
        progress.setChapterId(200L);
        assertNotFound(() -> service.updateProgress(1L, 10L, progress));
        verify(mapper, never()).upsertProgress(anyLong(), anyLong(), anyLong());
    }

    @Test
    void anotherUserCannotDeleteNovel() {
        when(mapper.deleteNovel(2L, 10L)).thenReturn(0);
        assertNotFound(() -> service.delete(2L, 10L));
    }

    @Test
    void invalidUploadDoesNotWriteAnyRows() {
        MockMultipartFile file = new MockMultipartFile("file", "错误.epub", "text/plain",
                "正文".getBytes(StandardCharsets.UTF_8));
        assertThrows(BusinessException.class, () -> service.importTxt(1L, file));
        verify(mapper, never()).insertNovel(any());
    }

    private void assertNotFound(Runnable action) {
        BusinessException error = assertThrows(BusinessException.class, action::run);
        assertEquals("B0003", error.getCode());
    }
}
