package com.sdjz.novel.service;

import com.sdjz.novel.pojo.dto.user.UserLoginDTO;
import com.sdjz.novel.pojo.dto.user.UserRegisterDTO;
import com.sdjz.novel.pojo.vo.user.UserInfoVO;
import com.sdjz.novel.pojo.vo.user.UserLoginVO;
import java.util.Map;


public interface SysUserService {
    /** 注册 */
    UserLoginVO register(UserRegisterDTO dto);
    /** 登录 */
    UserLoginVO login(UserLoginDTO dto);
    // 获取当前登录用户信息
    UserInfoVO getCurrentUserInfo();

}

