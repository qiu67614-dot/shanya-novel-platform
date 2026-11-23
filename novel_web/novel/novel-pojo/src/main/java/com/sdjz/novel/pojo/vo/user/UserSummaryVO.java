package com.sdjz.novel.pojo.vo.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
}
