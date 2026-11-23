package com.sdjz.novel.service.storage;

import com.sdjz.novel.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OssCoverStorageTest {
    private final OssCoverStorage storage = new OssCoverStorage();

    @Test
    void absentCoverDoesNotRequireOssConfiguration() {
        assertNull(storage.upload(null));
    }

    @Test
    void rejectsFakeImageBeforeSendingToOss() {
        ReflectionTestUtils.setField(storage, "endpoint", "https://oss-cn-hangzhou.aliyuncs.com");
        ReflectionTestUtils.setField(storage, "bucket", "test-bucket");
        ReflectionTestUtils.setField(storage, "accessKeyId", "test-id");
        ReflectionTestUtils.setField(storage, "accessKeySecret", "test-secret");
        ReflectionTestUtils.setField(storage, "publicBaseUrl", "https://test-bucket.oss-cn-hangzhou.aliyuncs.com");
        MockMultipartFile fake = new MockMultipartFile("cover", "cover.jpg", "image/jpeg", new byte[]{1, 2, 3, 4});
        BusinessException error = assertThrows(BusinessException.class, () -> storage.upload(fake));
        assertEquals("A0400", error.getCode());
    }
}
