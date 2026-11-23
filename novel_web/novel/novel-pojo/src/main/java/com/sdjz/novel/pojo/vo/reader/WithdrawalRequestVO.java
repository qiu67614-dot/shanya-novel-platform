package com.sdjz.novel.pojo.vo.reader;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WithdrawalRequestVO {
    private Long id;
    private Long userNovelId;
    private Long publicBookId;
    private Long userId;
    private String username;
    private String bookName;
    private String reason;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime reviewedAt;
}
