package com.sdjz.novel.pojo.vo.user;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserInfoVO {
    private Long id;
    private String username;
    private String nickName;
    private String userPhoto;
    private Integer userSex;
    private LocalDate userBirthday;
    private String userPhone;
    private String userEmail;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
