package com.domaindns.user.controller;

import com.domaindns.auth.service.JwtService;
import com.domaindns.common.ApiResponse;
import com.domaindns.auth.entity.User;
import com.domaindns.auth.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user/info")
public class UserInfoController {
    private final UserMapper userMapper;
    private final JwtService jwtService;
    private final com.domaindns.auth.mapper.UserProfileMapper userProfileMapper;

    public UserInfoController(UserMapper userMapper, JwtService jwtService, com.domaindns.auth.mapper.UserProfileMapper userProfileMapper) {
        this.userMapper = userMapper;
        this.jwtService = jwtService;
        this.userProfileMapper = userProfileMapper;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> getUserInfo(@RequestHeader("Authorization") String authorization) {
        long userId = getCurrentUserId(authorization);

        // 从数据库获取用户信息
        User user = userMapper.findById(userId);
        if (user == null) {
            return ApiResponse.error(50000, "用户不存在");
        }

        // 构建返回数据
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("points", user.getPoints());
        userInfo.put("createdAt", user.getCreatedAt());
        userInfo.put("status", getStatusText(user.getStatus()));
        userInfo.put("role", user.getRole());

        // 获取实名认证状态
        com.domaindns.auth.entity.UserProfile profile = userProfileMapper.findByUserId(userId);
        boolean isVerified = profile != null && profile.getIsVerified() != null && profile.getIsVerified() == 1;
        userInfo.put("isVerified", isVerified);

        return ApiResponse.ok(userInfo);
    }

    private long getCurrentUserId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new RuntimeException("未登录");
        }
        String token = authorization.substring(7);
        Jws<Claims> jws = jwtService.parse(token);
        String role = jws.getBody().get("role", String.class);
        if (!"USER".equals(role)) {
            throw new RuntimeException("权限不足，需要用户权限");
        }
        return Long.parseLong(jws.getBody().getSubject());
    }

    private String getStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 1:
                return "ACTIVE";
            case 0:
                return "INACTIVE";
            case 2:
                return "DELETED";
            case -1:
                return "BANNED";
            default:
                return "未知";
        }
    }
}
