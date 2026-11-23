package com.sdjz.novel.service;

import com.sdjz.novel.common.util.AdminJwtUtil;
import com.sdjz.novel.common.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminJwtUtilTest {
    @Test
    void ordinaryUserTokenCannotAuthenticateAsAdministrator() {
        String secret = "test-secret-with-at-least-thirty-two-bytes";
        AdminJwtUtil adminJwt = new AdminJwtUtil();
        ReflectionTestUtils.setField(adminJwt, "secret", secret);
        ReflectionTestUtils.setField(adminJwt, "expireHour", 8);
        JwtUtil userJwt = new JwtUtil();
        ReflectionTestUtils.setField(userJwt, "secret", secret);
        ReflectionTestUtils.setField(userJwt, "expireHour", 8);

        assertEquals(1L, adminJwt.getAdminId(adminJwt.generateToken(1L)));
        assertThrows(IllegalArgumentException.class, () -> adminJwt.getAdminId(userJwt.generateToken(1L)));
    }
}
