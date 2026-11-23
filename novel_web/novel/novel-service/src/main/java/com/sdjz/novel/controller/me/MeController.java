package com.sdjz.novel.controller.me;
import com.sdjz.novel.common.exception.ErrorCode;
import com.sdjz.novel.common.context.UserContext;
import com.sdjz.novel.common.exception.BusinessException;
import com.sdjz.novel.common.response.RestResp;
import com.sdjz.novel.pojo.dto.user.UserUpdateMeDTO;
import com.sdjz.novel.pojo.vo.user.UserInfoVO;
import com.sdjz.novel.service.MeService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@Slf4j
@RequestMapping("/front/me")
public class MeController {


    @Autowired
    private MeService meService;

    /**
     * 获取个人资料 get /front/me
     */
    @GetMapping("/me")
    public  RestResp<UserInfoVO> getUserInfo(){


        Long userId = UserContext.getUid();
        if (userId == null) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRE,"登录过期或未登录");
        }
        log.info("{},开始获取个人资料", userId);
        UserInfoVO userInfoVO = new UserInfoVO();
        userInfoVO = meService.getgetUserInfo(userId);
        return RestResp.ok(userInfoVO);
    }

    /**
     * 【新增】更新个人资料 PUT /front/me
     */
    @PutMapping("/me")
    public RestResp updateMe(@Valid @RequestBody UserUpdateMeDTO dto){
        Long userId = UserContext.getUid();
        if (userId == null) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRE,"登录过期或未登录");
        }
        log.info("{},开始更新个人资料", userId);
        meService.updateUserMe(userId, dto);
        return RestResp.ok(null);
    }

}
