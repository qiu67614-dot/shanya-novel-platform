package com.sdjz.novel.service;

import com.sdjz.novel.pojo.dto.reader.BookshelfProgressDTO;
import com.sdjz.novel.pojo.vo.reader.BookshelfEntryVO;

import java.util.List;

public interface BookshelfService {

    List<BookshelfEntryVO> list(Long userId);

    BookshelfEntryVO save(
            Long userId,
            Long bookId,
            BookshelfProgressDTO dto
    );

    void remove(Long userId, Long bookId);
}