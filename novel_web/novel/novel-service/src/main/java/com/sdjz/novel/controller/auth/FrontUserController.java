package com.sdjz.novel.controller.auth;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.sdjz.novel.common.exception.ServiceUnavailableException;
import com.sdjz.novel.common.ratelimit.RateLimit;
import com.sdjz.novel.common.ratelimit.RateLimitDimension;
import com.sdjz.novel.common.ratelimit.RateLimitGroup;
import com.sdjz.novel.common.response.RestResp;
import com.sdjz.novel.common.util.RedisUtil;
import com.sdjz.novel.pojo.dto.user.UserLoginDTO;
import com.sdjz.novel.pojo.dto.user.UserRegisterDTO;
import com.sdjz.novel.pojo.vo.user.CaptchaVO;
import com.sdjz.novel.pojo.vo.user.UserLoginVO;
import com.sdjz.novel.service.SysUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;



@Slf4j
@RestController
@RequestMapping("/front/auth")
@RequiredArgsConstructor
public class FrontUserController {

    @Autowired
    SysUserService sysUserService;
    @Autowired
    RedisUtil redisUtil;
    private static final String CAPTCHA_KEY_PREFIX = "captcha:";
    // 验证码过期时间 5分钟
    private static final int CAPTCHA_EXPIRE_MINUTE = 5;

    /**
     * 注册
     */
    @RateLimit(group = RateLimitGroup.AUTH, dimension = RateLimitDimension.IP)
    @PostMapping("/register")
    public RestResp<UserLoginVO> register(@Valid @RequestBody UserRegisterDTO dto) {
        String name = dto.getUsername();
        log.info("{},开始注册", name);
        UserLoginVO vo = sysUserService.register(dto);
        return RestResp.ok(vo);
    }

    /**
     * 登录。
     *
     * <p>按 IP 限流（此时还没有 uid），配合 Service 层按用户名的失败计数：
     * IP 这层挡「一台机器高频尝试」，用户名计数挡「针对某个账号的撞库」。
     */
    @RateLimit(group = RateLimitGroup.AUTH, dimension = RateLimitDimension.IP)
    @PostMapping("/login")
    public RestResp<UserLoginVO> login(@Valid @RequestBody UserLoginDTO dto) {
        String name = dto.getUsername();
        log.info("{},开始登录", name);
        UserLoginVO vo = sysUserService.login(dto);
        return RestResp.ok(vo);
    }
    /**
     * 发送验证码
     */
    @RateLimit(group = RateLimitGroup.AUTH, dimension = RateLimitDimension.IP)
    @GetMapping("/captcha")
    public RestResp<CaptchaVO> getImgVerifyCode() throws IOException {
        log.info("开始生成验证码");
        // 生成4位验证码
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 20);
        String code = lineCaptcha.getCode();

        // sessionId UUID
        String sessionId = UUID.randomUUID().toString().replace("-", "");

        // 验证码存入Redis，5分钟过期
        // 写入失败必须返回 503：拿不到 captchaKey 的话，前端后面的注册请求必然失败，
        // 但用户只会看到「验证码错误」，反复重试也没用
        try {
            redisUtil.set(CAPTCHA_KEY_PREFIX + sessionId, code, CAPTCHA_EXPIRE_MINUTE, TimeUnit.MINUTES);
        } catch (RuntimeException e) {
            log.error("验证码写入 Redis 失败", e);
            throw new ServiceUnavailableException("验证码服务暂不可用，请稍后重试", e);
        }

        // 输出base64，不要 data:image/png;base64, 前缀（前端文档约定）
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            lineCaptcha.write(bos);
            byte[] bytes = bos.toByteArray();
            String base64Img = Base64.getEncoder().encodeToString(bytes);

            CaptchaVO vo = new CaptchaVO();
            vo.setImg(base64Img);
            vo.setSessionId(sessionId);
            vo.setCaptchaKey(sessionId);
            vo.setImage("data:image/png;base64," + base64Img);
            return RestResp.ok(vo);
        }
    }
}
