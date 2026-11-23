package com.sdjz.novel.pojo.vo.user;

import lombok.Data;

@Data
public class UserLoginVO {
    private String token;
    private Long uid;
    private String nickName;
    private UserSummaryVO user;
}

