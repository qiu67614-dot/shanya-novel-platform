package com.sdjz.novel.pojo.vo.user;

import lombok.Data;

@Data
public class CaptchaVO {
    /** base64图片，不带data:前缀 */
    private String img;
    private String sessionId;
    private String captchaKey;
    private String image;
}

