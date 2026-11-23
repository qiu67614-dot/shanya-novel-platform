package com.sdjz.novel.service.imports;

import com.sdjz.novel.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class PrivateNovelTextParserTest {
    private final PrivateNovelTextParser parser = new PrivateNovelTextParser();

    @Test
    void splitsUtf8BomAndMixedNewlinesWithPreface() {
        byte[] text = ("\uFEFF开场白\r\n第一章 初见\r\n第一章的内容\r第二章 重逢\n第二章的内容")
                .getBytes(StandardCharsets.UTF_8);
        var result = parser.parse("作品.txt", text);
        assertEquals("UTF-8", result.encoding());
        assertEquals(3, result.chapters().size());
        assertEquals("序章", result.chapters().get(0).name());
        assertEquals("第一章 初见", result.chapters().get(1).name());
        assertEquals("第二章的内容", result.chapters().get(2).content());
    }

    @Test
    void readsGb18030() {
        var result = parser.parse("旧书.TXT", "第一章 开始\n中文正文".getBytes(Charset.forName("GB18030")));
        assertEquals("GB18030", result.encoding());
        assertEquals("中文正文", result.chapters().get(0).content());
    }

    @Test
    void makesSingleChapterWhenNoHeadingExists() {
        var result = parser.parse("无标题.txt", "这是一整本小说。".getBytes(StandardCharsets.UTF_8));
        assertEquals(1, result.chapters().size());
        assertEquals("正文", result.chapters().get(0).name());
    }

    @Test
    void rejectsEmptyOversizedAndBinaryFiles() {
        assertThrows(BusinessException.class, () -> parser.parse("空.txt", new byte[0]));
        assertThrows(BusinessException.class,
                () -> parser.parse("过大.txt", new byte[PrivateNovelTextParser.MAX_BYTES + 1]));
        assertThrows(BusinessException.class,
                () -> parser.parse("二进制.txt", new byte[] {65, 0, 66}));
        assertThrows(BusinessException.class,
                () -> parser.parse("错误.epub", "正文".getBytes(StandardCharsets.UTF_8)));
        assertThrows(BusinessException.class,
                () -> parser.parse("编码.txt", new byte[] {(byte) 0x81}));
    }
}
