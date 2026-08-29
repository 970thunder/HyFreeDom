package com.domaindns.admin.controller;

import com.domaindns.admin.mapper.AuditMapper;
import com.domaindns.admin.model.AuditLog;
import com.domaindns.auth.service.JwtService;
import com.domaindns.common.ApiResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/audit")
public class AuditController {
    private final AuditMapper mapper;
    private final JwtService jwtService;

    public AuditController(AuditMapper mapper, JwtService jwtService) {
        this.mapper = mapper;
        this.jwtService = jwtService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestHeader("Authorization") String authorization,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        // 验证管理员权限
        validateAdminAuth(authorization);
        int offset = (Math.max(page, 1) - 1) * Math.max(size, 1);
        List<AuditLog> list = mapper.list(action, offset, size);
        int total = mapper.count(action);
        Map<String, Object> m = new HashMap<>();
        m.put("list", list);
        m.put("total", total);
        m.put("page", page);
        m.put("size", size);
        return ApiResponse.ok(m);
    }

    @GetMapping("/email-codes")
    public ApiResponse<Map<String, Object>> listEmailCodeLogs(@RequestHeader("Authorization") String authorization,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "ip", required = false) String ip,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        validateAdminAuth(authorization);
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        List<AuditLog> list = mapper.listEmailCodeLogs(email, ip, offset, safeSize);
        int total = mapper.countEmailCodeLogs(email, ip);
        Map<String, Object> m = new HashMap<>();
        m.put("list", list);
        m.put("total", total);
        m.put("page", safePage);
        m.put("size", safeSize);
        return ApiResponse.ok(m);
    }

    // 验证管理员权限
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
