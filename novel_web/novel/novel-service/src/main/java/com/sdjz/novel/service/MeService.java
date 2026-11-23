package com.sdjz.novel.service;

import com.sdjz.novel.pojo.dto.user.UserUpdateMeDTO;
import com.sdjz.novel.pojo.vo.user.UserInfoVO;
import jakarta.validation.Valid;

public interface MeService {

    void updateUserMe(Long userId, @Valid UserUpdateMeDTO dto) ;

    UserInfoVO getgetUserInfo(Long userId);
}
