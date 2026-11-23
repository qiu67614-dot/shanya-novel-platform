package com.sdjz.novel.pojo.vo.reader;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AdminAuditLogVO {
    private Long id;
    private Long adminId;
    private String adminUsername;
    private String targetType;
    private Long targetId;
    private String action;
    private String reason;
    private LocalDateTime createTime;
}
