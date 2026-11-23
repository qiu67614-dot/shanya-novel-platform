package com.sdjz.novel.pojo.vo.user;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminLoginVO {
    private String token;
    private Long adminId;
    private String username;
}
