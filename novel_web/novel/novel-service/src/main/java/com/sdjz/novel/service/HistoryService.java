package com.sdjz.novel.service;

import com.sdjz.novel.pojo.dto.reader.ProgressUpdateDTO;
import com.sdjz.novel.pojo.vo.reader.HistoryEntryVO;
import com.sdjz.novel.pojo.vo.reader.PageVO;

public interface HistoryService {

    PageVO<HistoryEntryVO> getHistory(Long userId, Integer page, Integer size);

    void updateProgress(Long userId, ProgressUpdateDTO dto);
}
