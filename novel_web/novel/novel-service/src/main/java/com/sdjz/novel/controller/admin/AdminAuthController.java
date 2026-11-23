package com.sdjz.novel.controller.admin;

import com.sdjz.novel.common.response.RestResp;
import com.sdjz.novel.pojo.dto.user.AdminLoginDTO;
import com.sdjz.novel.pojo.vo.user.AdminLoginVO;
import com.sdjz.novel.service.PublicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {
    private final PublicationService service;

    @PostMapping("/login")
    public RestResp<AdminLoginVO> login(@Valid @RequestBody AdminLoginDTO dto) {
        return RestResp.ok(service.login(dto));
    }
}
