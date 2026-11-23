package com.sdjz.novel.service.imp;

import com.sdjz.novel.common.exception.ErrorCode;
import com.sdjz.novel.common.context.UserContext;
import com.sdjz.novel.common.exception.BusinessException;
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



    @Override
    @Transactional
    public UserLoginVO register(UserRegisterDTO dto) {
        if (dto.getEffectiveCode() == null || dto.getEffectiveCode().isBlank()
                || dto.getEffectiveKey() == null || dto.getEffectiveKey().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "验证码参数不能为空");
        }
        //1.校验验证码
        String redisCode = redisUtil.get(CAPTCHA_PREFIX + dto.getEffectiveKey());
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
        // 删除验证码
        redisUtil.delete(CAPTCHA_PREFIX + dto.getEffectiveKey());
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
        SysUser user = sysUserMapper.selectByUsername(dto.getUsername());
        if(user == null){
            throw new BusinessException(ErrorCode.LOGIN_ERROR,"用户不存在");
        }
        //校验密码
        if(!passwordEncoder.matches(dto.getPassword(), user.getPassword())){
            throw new BusinessException(ErrorCode.LOGIN_ERROR,"用户名或密码错误");
        }
        // 账号禁用
        if(user.getStatus() == 1){
            throw new BusinessException(ErrorCode.LOGIN_ERROR,"账号已被禁用");
        }
        String token = jwtUtil.generateToken(user.getId());
        UserLoginVO vo = new UserLoginVO();
        vo.setToken(token);
        vo.setUid(user.getId());
        vo.setNickName(user.getNickName());
        vo.setUser(new UserSummaryVO(user.getId(), user.getUsername(), user.getNickName(), user.getUserPhoto()));
        return vo;
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

