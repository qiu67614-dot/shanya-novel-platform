package com.sdjz.novel.pojo.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AdminUser {
    private Long id;
    private String username;
    private String passwordHash;
    private Integer status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
