package com.sdjz.novel.pojo.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SysUser {
    private Long id;
    private String username;
    private String password;
    private String nickName;
    private String userPhoto;
    private Integer userSex;
    private String userBirthday;
    private String userPhone;
    private String userEmail;
    private Integer balance;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

