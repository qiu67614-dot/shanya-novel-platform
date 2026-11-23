package com.sdjz.novel.service.imp;

import com.sdjz.novel.common.exception.ErrorCode;
import com.sdjz.novel.common.exception.BusinessException;
import com.sdjz.novel.mapper.MeMapper;
import com.sdjz.novel.mapper.SysUserMapper;
import com.sdjz.novel.pojo.dto.user.UserUpdateMeDTO;
import com.sdjz.novel.pojo.entity.SysUser;
import com.sdjz.novel.pojo.vo.user.UserInfoVO;
import com.sdjz.novel.service.MeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MeserviceImp  implements MeService {
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private MeMapper memapper;

    @Override
    public void updateUserMe(Long userId, UserUpdateMeDTO dto) {
        SysUser dbUser = sysUserMapper.selectById(userId);
        if(dbUser == null){
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        // 手动赋值：只有dto不为null才设置到dbUser，配合mybatis动态sql
        if(dto.getNickName() != null){
            dbUser.setNickName(dto.getNickName());
        }
        if(dto.getUserPhoto() != null){
            dbUser.setUserPhoto(dto.getUserPhoto());
        }
        if(dto.getUserSex() != null){
            dbUser.setUserSex(dto.getUserSex());
        }
        if(dto.getUserBirthday() != null){
            dbUser.setUserBirthday(dto.getUserBirthday().toString());
        }
        if(dto.getUserPhone() != null){
            dbUser.setUserPhone(dto.getUserPhone());
        }
        if(dto.getUserEmail() != null){
            dbUser.setUserEmail(dto.getUserEmail());
        }

        memapper.updateById(dbUser);
    }

    @Override
    public UserInfoVO getgetUserInfo(Long userId) {
        SysUser sysUser = sysUserMapper.selectById(userId);
        if (sysUser == null) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRE,"登录过期或未登录");
        }
        UserInfoVO userInfoVO = new UserInfoVO();

        BeanUtils.copyProperties(sysUser, userInfoVO);
        return userInfoVO;
    }
}
