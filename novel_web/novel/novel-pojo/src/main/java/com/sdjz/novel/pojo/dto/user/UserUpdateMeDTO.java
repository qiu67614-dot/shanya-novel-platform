package com.sdjz.novel.pojo.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

@Data
public class UserUpdateMeDTO {

    @Size(max = 50, message = "昵称长度不能超过50")
    private String nickName;

    @Size(max = 255, message = "头像地址过长")
    private String userPhoto;

    /** 0未知 1男 2女 */
    private Integer userSex;

    private LocalDate userBirthday;

    @Size(max = 11, message = "手机号格式错误")
    private String userPhone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100)
    private String userEmail;
}

