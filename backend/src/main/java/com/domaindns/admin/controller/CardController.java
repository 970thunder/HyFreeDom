package com.domaindns.admin.controller;

import com.domaindns.admin.mapper.CardMapper;
import com.domaindns.admin.model.Card;
import com.domaindns.auth.service.JwtService;
import com.domaindns.common.ApiResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/cards")
public class CardController {
    private final CardMapper mapper;
    private final SecureRandom random = new SecureRandom();
    private final JwtService jwtService;

    public CardController(CardMapper mapper, JwtService jwtService) {
        this.mapper = mapper;
        this.jwtService = jwtService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestHeader("Authorization") String authorization,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        // 验证管理员权限
        validateAdminAuth(authorization);
        int offset = (Math.max(page, 1) - 1) * Math.max(size, 1);
        List<Card> list = mapper.list(status, offset, size);
        int total = mapper.count(status);
        Map<String, Object> m = new HashMap<>();
        m.put("list", list);
        m.put("total", total);
        m.put("page", page);
        m.put("size", size);
        return ApiResponse.ok(m);
    }

    @PostMapping("/generate")
    public ApiResponse<Map<String, Object>> generate(@RequestHeader("Authorization") String authorization,
            @RequestBody Map<String, Object> body) {
        // 验证管理员权限
        validateAdminAuth(authorization);
        int count = Integer.parseInt(body.getOrDefault("count", 0).toString());
        int points = Integer.parseInt(body.getOrDefault("points", 0).toString());
        Integer validDays = body.get("validDays") == null ? null : Integer.valueOf(body.get("validDays").toString());
        LocalDateTime expiredAt = validDays == null ? null : LocalDateTime.now().plusDays(validDays);
        int created = 0;
        for (int i = 0; i < count; i++) {
            String code = randomCode(16);
            created += mapper.insert(code, points, expiredAt);
        }
        Map<String, Object> m = new HashMap<>();
        m.put("count", created);
        return ApiResponse.ok(m);
    }

    @DeleteMapping("/batch")
    public ApiResponse<Map<String, Object>> batchDelete(@RequestHeader("Authorization") String authorization,
            @RequestBody Map<String, Object> body) {
        // 验证管理员权限
        validateAdminAuth(authorization);

        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) body.get("ids");
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("请选择要删除的卡密");
        }

        int deleted = mapper.batchDelete(ids);
        Map<String, Object> result = new HashMap<>();
        result.put("deleted", deleted);
        result.put("message", "成功删除 " + deleted + " 张卡密");
        return ApiResponse.ok(result);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Object>> delete(@RequestHeader("Authorization") String authorization,
            @PathVariable Long id) {
        // 验证管理员权限
        validateAdminAuth(authorization);

        int deleted = mapper.deleteById(id);
        Map<String, Object> result = new HashMap<>();
        result.put("deleted", deleted);
        result.put("message", deleted > 0 ? "卡密删除成功" : "卡密不存在或已被删除");
        return ApiResponse.ok(result);
    }

    private String randomCode(int len) {
        final String dict = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++)
            sb.append(dict.charAt(random.nextInt(dict.length())));
        return sb.toString();
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