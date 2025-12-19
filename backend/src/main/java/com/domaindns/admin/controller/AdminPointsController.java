package com.domaindns.admin.controller;

import com.domaindns.auth.service.JwtService;
import com.domaindns.common.ApiResponse;
import com.domaindns.user.mapper.PointsMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/points")
public class AdminPointsController {
    private final PointsMapper pointsMapper;
    private final JwtService jwtService;

    public AdminPointsController(PointsMapper pointsMapper, JwtService jwtService) {
        this.pointsMapper = pointsMapper;
        this.jwtService = jwtService;
    }

    @GetMapping("/transactions")
    public ApiResponse<Map<String, Object>> listTransactions(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        
        validateAdminAuth(authorization);

        int offset = (Math.max(page, 1) - 1) * Math.max(size, 1);
        List<Map<String, Object>> list = pointsMapper.listAll(keyword, type, offset, size);
        int total = pointsMapper.countAll(keyword, type);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);

        return ApiResponse.ok(result);
    }

    private void validateAdminAuth(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new RuntimeException("未登录");
        }
        String token = authorization.substring(7);
        try {
            Jws<Claims> jws = jwtService.parse(token);
            String role = jws.getBody().get("role", String.class);
            if (!"ADMIN".equals(role)) {
                throw new RuntimeException("权限不足，需要管理员权限");
            }
        } catch (Exception e) {
            throw new RuntimeException("Token无效或已过期");
        }
    }
}
