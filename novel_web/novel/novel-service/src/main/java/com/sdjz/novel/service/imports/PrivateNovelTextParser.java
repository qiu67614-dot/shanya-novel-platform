package com.sdjz.novel.service.imports;

import com.sdjz.novel.common.exception.BusinessException;
import com.sdjz.novel.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PrivateNovelTextParser {
    public static final int MAX_BYTES = 20 * 1024 * 1024;
    private static final int MAX_CHAPTERS = 5000;
    private static final Pattern CHAPTER_TITLE = Pattern.compile(
            "^\\s*(第\\s*[0-9０-９〇零一二三四五六七八九十百千万两]+\\s*[章节回卷篇]"
                    + "(?:[\\s:：.、-]+[^\\n]{0,70})?)\\s*$");

    public ParsedNovel parse(String fileName, byte[] bytes) {
        String safeName = fileName == null ? "" : fileName.replace('\\', '/');
        safeName = safeName.substring(safeName.lastIndexOf('/') + 1).trim();
        if (!safeName.toLowerCase(Locale.ROOT).endsWith(".txt") || safeName.length() <= 4) {
            throw invalid("只支持 TXT 文件");
        }
        if (bytes == null || bytes.length == 0) {
            throw invalid("文件为空");
        }
        if (bytes.length > MAX_BYTES) {
            throw invalid("文件不能超过 20 MB");
        }
        rejectBinary(bytes);

        boolean utf8Bom = bytes.length >= 3 && (bytes[0] & 0xff) == 0xef
                && (bytes[1] & 0xff) == 0xbb && (bytes[2] & 0xff) == 0xbf;
        String encoding;
        String decoded;
        try {
            decoded = decode(bytes, utf8Bom ? 3 : 0, StandardCharsets.UTF_8);
            encoding = "UTF-8";
        } catch (CharacterCodingException ignored) {
            try {
                decoded = decode(bytes, 0, Charset.forName("GB18030"));
                encoding = "GB18030";
            } catch (CharacterCodingException e) {
                throw invalid("文件编码无法识别");
            }
        }
        rejectBinaryText(decoded);
        String normalized = decoded.replace("\r\n", "\n").replace('\r', '\n').strip();
        if (normalized.isEmpty()) {
            throw invalid("文件没有正文内容");
        }

        List<ParsedChapter> chapters = new ArrayList<>();
        StringBuilder body = new StringBuilder();
        String currentTitle = null;
        for (String line : normalized.split("\n", -1)) {
            Matcher matcher = line.length() <= 100 ? CHAPTER_TITLE.matcher(line) : null;
            if (matcher != null && matcher.matches()) {
                addChapter(chapters, currentTitle == null ? "序章" : currentTitle, body);
                currentTitle = matcher.group(1).trim();
            } else {
                body.append(line).append('\n');
            }
        }
        addChapter(chapters, currentTitle == null ? "正文" : currentTitle, body);
        if (chapters.isEmpty()) {
            throw invalid("文件没有正文内容");
        }

        String bookName = safeName.substring(0, safeName.length() - 4).strip();
        if (bookName.isEmpty()) {
            throw invalid("文件名不能作为书名");
        }
        if (bookName.length() > 200 || safeName.length() > 255) {
            throw invalid("文件名过长");
        }
        long words = chapters.stream().mapToLong(ParsedChapter::wordCount).sum();
        return new ParsedNovel(bookName, safeName, encoding, bytes.length, words, List.copyOf(chapters));
    }

    private static String decode(byte[] bytes, int offset, Charset charset) throws CharacterCodingException {
        return charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes, offset, bytes.length - offset)).toString();
    }

    private static void rejectBinary(byte[] bytes) {
        int controls = 0;
        for (byte value : bytes) {
            int unsigned = value & 0xff;
            if (unsigned == 0) throw invalid("文件包含二进制内容");
            if (unsigned < 32 && unsigned != 9 && unsigned != 10 && unsigned != 13) controls++;
        }
        if (controls > Math.max(1, bytes.length / 100)) throw invalid("文件包含二进制内容");
    }

    private static void rejectBinaryText(String text) {
        long controls = text.codePoints().filter(cp -> Character.isISOControl(cp)
                && cp != '\n' && cp != '\r' && cp != '\t').count();
        if (controls > Math.max(1, text.length() / 100)) throw invalid("文件包含二进制内容");
    }

    private static void addChapter(List<ParsedChapter> chapters, String name, StringBuilder body) {
        String content = body.toString().strip();
        body.setLength(0);
        if (content.isEmpty()) return;
        if (chapters.size() >= MAX_CHAPTERS) throw invalid("章节数不能超过 5000");
        int wordCount = (int) content.codePoints().filter(cp -> !Character.isWhitespace(cp)).count();
        chapters.add(new ParsedChapter(name, content, wordCount));
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.PARAM_ERROR, message);
    }

    public record ParsedChapter(String name, String content, int wordCount) {}

    public record ParsedNovel(String bookName, String originalFileName, String encoding,
                              long fileSize, long wordCount, List<ParsedChapter> chapters) {}
}
