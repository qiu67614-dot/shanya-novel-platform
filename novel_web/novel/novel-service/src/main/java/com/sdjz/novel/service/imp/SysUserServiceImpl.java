package com.sdjz.novel.service.imp;

import com.sdjz.novel.common.exception.ErrorCode;
import com.sdjz.novel.common.context.UserContext;
import com.sdjz.novel.common.exception.BusinessException;
import com.sdjz.novel.common.exception.ServiceUnavailableException;
import com.sdjz.novel.common.util.JwtUtil;
import com.sdjz.novel.common.util.RedisUtil;
import com.sdjz.novel.mapper.SysUserMapper;
import com.sdjz.novel.pojo.dto.user.UserLoginDTO;
import com.sdjz.novel.pojo.dto.user.UserRegisterDTO;
import com.sdjz.novel.pojo.entity.SysUser;
import com.sdjz.novel.pojo.vo.user.UserInfoVO;
import com.sdjz.novel.pojo.vo.user.UserLoginVO;
import com.sdjz.novel.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sdjz.novel.pojo.vo.user.UserSummaryVO;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {
    @Autowired
    private   RedisUtil redisUtil;
    @Autowired
    private  SysUserMapper sysUserMapper;

    @Autowired
    private  JwtUtil jwtUtil;

    @Autowired
    private  BCryptPasswordEncoder passwordEncoder;

    private static final String CAPTCHA_PREFIX = "captcha:";

    /**
     * 登录失败计数：按<b>用户名</b>计，不按 IP。
     *
     * <p>按 IP 计的话，一个宿舍共用出口时，某个人输错密码会连累全宿舍被锁；
     * 按用户名计才锁得准。IP 维度的粗保护已经由 Nginx 和全局闸门负责。
     */
    private static final String LOGIN_FAIL_PREFIX = "login:fail:";
    /** 连续失败多少次锁定。 */
    private static final int LOGIN_FAIL_LIMIT = 5;
    /** 锁定时长（分钟）。 */
    private static final int LOGIN_LOCK_MINUTES = 15;



    @Override
    @Transactional
    public UserLoginVO register(UserRegisterDTO dto) {
        if (dto.getEffectiveCode() == null || dto.getEffectiveCode().isBlank()
                || dto.getEffectiveKey() == null || dto.getEffectiveKey().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "验证码参数不能为空");
        }
        //1.校验验证码
        // 验证码必须「失败即拒绝」：验证不了就不能放行，否则等于把注册接口敞开。
        // 但拒绝的原因要可区分——Redis 挂了不是「验证码错误」，不能返回 200 + A0002，
        // 否则用户会一直重输验证码，运维也看不到故障。
        String redisCode;
        try {
            redisCode = redisUtil.get(CAPTCHA_PREFIX + dto.getEffectiveKey());
        } catch (RuntimeException e) {
            throw new ServiceUnavailableException("验证码服务暂不可用，请稍后重试", e);
        }
        if(redisCode == null || !redisCode.equalsIgnoreCase(dto.getEffectiveCode())){
            throw new BusinessException(ErrorCode.CODE_ERROR,"验证码错误");
        }
        // 校验手机号是否已注册
        SysUser existUser = sysUserMapper.selectByUsername(dto.getUsername());
        if(existUser != null){
            throw new BusinessException(ErrorCode.PHONE_EXIST,"该手机号已注册");
        }
        // 组装用户，密码BCrypt加密；nickName默认生成
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickName("书友"+System.currentTimeMillis());
        sysUserMapper.insert(user);
        // 删除验证码是「清理」而非「校验」，失败不该回滚已经写好的用户
        safeDelete(CAPTCHA_PREFIX + dto.getEffectiveKey());
        //生成token返回
        String token = jwtUtil.generateToken(user.getId());
        UserLoginVO vo = new UserLoginVO();
        vo.setToken(token);
        vo.setUid(user.getId());
        vo.setNickName(user.getNickName());
        vo.setUser(new UserSummaryVO(user.getId(), user.getUsername(), user.getNickName(), user.getUserPhoto()));
        return vo;
    }

    @Override
    public UserLoginVO login(UserLoginDTO dto) {
        String failKey = LOGIN_FAIL_PREFIX + dto.getUsername();

        // 先判断是否已被锁定，再查库和跑 BCrypt。
        // BCrypt 是刻意设计的慢哈希（约 100ms/次），锁定期内继续校验等于免费帮攻击者
        // 消耗 CPU —— 这一步是「撞库防护」里最容易漏掉的成本控制。
        //
        // 失败计数是「锦上添花」的防护，不是登录的必要条件：Redis 挂了就跳过这层检查，
        // 让用户还能登录。反过来（因为 Redis 挂掉而全站无法登录）是可用性事故。
        try {
            String failCount = redisUtil.get(failKey);
            if (failCount != null && parseCount(failCount) >= LOGIN_FAIL_LIMIT) {
                long remainSeconds = redisUtil.getExpireSeconds(failKey);
                long remainMinutes = Math.max(1, (remainSeconds + 59) / 60);
                throw new BusinessException(ErrorCode.LOGIN_LOCKED,
                        "密码错误次数过多，请 " + remainMinutes + " 分钟后再试");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn("登录失败计数不可用，本次跳过锁定检查", e);
        }

        SysUser user = sysUserMapper.selectByUsername(dto.getUsername());
        // 「用户不存在」与「密码错误」统一文案：分别提示等于把用户名是否存在告诉攻击者，
        // 是典型的用户名枚举漏洞
        if (user == null) {
            recordLoginFail(failKey, dto.getUsername());
            throw new BusinessException(ErrorCode.LOGIN_ERROR, "用户名或密码错误");
        }
        //校验密码
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            recordLoginFail(failKey, dto.getUsername());
            throw new BusinessException(ErrorCode.LOGIN_ERROR, "用户名或密码错误");
        }
        // 账号禁用
        if (user.getStatus() == 1) {
            throw new BusinessException(ErrorCode.LOGIN_ERROR, "账号已被禁用");
        }

        // 登录成功清零，避免历史失败次数挤占后续额度
        safeDelete(failKey);

        String token = jwtUtil.generateToken(user.getId());
        UserLoginVO vo = new UserLoginVO();
        vo.setToken(token);
        vo.setUid(user.getId());
        vo.setNickName(user.getNickName());
        vo.setUser(new UserSummaryVO(user.getId(), user.getUsername(), user.getNickName(), user.getUserPhoto()));
        return vo;
    }

    /**
     * 记录一次登录失败。
     *
     * <p>INCR 与 EXPIRE 合并在一段 Lua 里执行：拆成两次调用的话，进程若在两者之间
     * 崩溃或重启，key 会永不过期，用户被永久锁定且日志里查不出原因。
     *
     * <p>达到阈值时把 TTL 重置为完整锁定期，让 15 分钟从「第 N 次失败」开始算，
     * 而不是从「第一次失败」开始算。
     */
    private void recordLoginFail(String failKey, String username) {
        try {
            long count = redisUtil.incrementWithTtl(failKey, 1, LOGIN_LOCK_MINUTES * 60L);
            if (count >= LOGIN_FAIL_LIMIT) {
                redisUtil.expire(failKey, LOGIN_LOCK_MINUTES * 60L, TimeUnit.SECONDS);
                log.warn("账号 {} 连续登录失败 {} 次，锁定 {} 分钟", username, count, LOGIN_LOCK_MINUTES);
            }
        } catch (RuntimeException e) {
            // 计数失败不影响「本次登录失败」的返回结果
            log.warn("登录失败计数写入失败，账号 {}", username, e);
        }
    }

    private void safeDelete(String key) {
        try {
            redisUtil.delete(key);
        } catch (RuntimeException e) {
            log.warn("Redis 清理失败，忽略：{}", key, e);
        }
    }

    private int parseCount(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            // 脏数据不应阻断登录，交给 TTL 自然淘汰
            log.warn("登录失败计数格式异常：{}", raw);
            return 0;
        }
    }

    /** 获取当前登录用户信息 */
    @Override
    public UserInfoVO getCurrentUserInfo() {
        Long uid = UserContext.getUid();
        SysUser sysUser = sysUserMapper.selectById(uid);
        if(sysUser == null){
            throw new BusinessException(ErrorCode.TOKEN_EXPIRE,"登录已过期，请重新登录");
        }
        UserInfoVO vo = new UserInfoVO();
        // 对象拷贝，忽略password敏感字段
        BeanUtils.copyProperties(sysUser,vo);
        return vo;
    }


}

