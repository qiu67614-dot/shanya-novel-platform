package com.sdjz.novel.pojo.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserRegisterDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;
    @NotBlank(message = "密码不能为空")
    private String password;
    private String velCode;
    private String sessionId;
    private String captchaCode;
    private String captchaKey;

    public String getEffectiveCode() {
        return captchaCode != null && !captchaCode.isBlank() ? captchaCode : velCode;
    }

    public String getEffectiveKey() {
        return captchaKey != null && !captchaKey.isBlank() ? captchaKey : sessionId;
    }
}
