package com.domaindns.admin.controller;

import com.domaindns.admin.service.StatsService;
import com.domaindns.auth.service.JwtService;
import com.domaindns.common.ApiResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/stats")
public class StatsController {
    private final StatsService statsService;
    private final JwtService jwtService;

    public StatsController(StatsService statsService, JwtService jwtService) {
        this.statsService = statsService;
        this.jwtService = jwtService;
    }

    /**
     * 获取Dashboard统计数据
     */
    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> getDashboardStats(@RequestHeader("Authorization") String authorization) {
        // 验证管理员权限
        validateAdminAuth(authorization);

        try {
            Map<String, Object> stats = statsService.getDashboardStats();
            return ApiResponse.ok(stats);
        } catch (Exception e) {
            return ApiResponse.error(50000, "获取统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户统计
     */
    @GetMapping("/users")
    public ApiResponse<Map<String, Object>> getUserStats(@RequestHeader("Authorization") String authorization) {
        validateAdminAuth(authorization);

        try {
            Map<String, Object> stats = statsService.getUserStats();
            return ApiResponse.ok(stats);
        } catch (Exception e) {
            return ApiResponse.error(50000, "获取用户统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取域名统计
     */
    @GetMapping("/zones")
    public ApiResponse<Map<String, Object>> getZoneStats(@RequestHeader("Authorization") String authorization) {
        validateAdminAuth(authorization);

        try {
            Map<String, Object> stats = statsService.getZoneStats();
            return ApiResponse.ok(stats);
        } catch (Exception e) {
            return ApiResponse.error(50000, "获取域名统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取DNS记录统计
     */
    @GetMapping("/dns-records")
    public ApiResponse<Map<String, Object>> getDnsRecordStats(@RequestHeader("Authorization") String authorization) {
        validateAdminAuth(authorization);

        try {
            Map<String, Object> stats = statsService.getDnsRecordStats();
            return ApiResponse.ok(stats);
        } catch (Exception e) {
            return ApiResponse.error(50000, "获取DNS记录统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取积分统计
     */
    @GetMapping("/points")
    public ApiResponse<Map<String, Object>> getPointsStats(@RequestHeader("Authorization") String authorization) {
        validateAdminAuth(authorization);

        try {
            Map<String, Object> stats = statsService.getPointsStats();
            return ApiResponse.ok(stats);
        } catch (Exception e) {
            return ApiResponse.error(50000, "获取积分统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户注册统计
     */
    @GetMapping("/user-registration")
    public ApiResponse<Map<String, Object>> getUserRegistrationStats(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(value = "type", defaultValue = "day") String type,
            @RequestParam(value = "days", defaultValue = "7") Integer days) {
        validateAdminAuth(authorization);

        try {
            Map<String, Object> stats = statsService.getUserRegistrationStats(type, days);
            return ApiResponse.ok(stats);
        } catch (Exception e) {
            return ApiResponse.error(50000, "获取用户注册统计失败: " + e.getMessage());
        }
    }

    /**
     * 刷新统计数据缓存
     */
    @PostMapping("/refresh")
    public ApiResponse<Void> refreshStats(@RequestHeader("Authorization") String authorization) {
        validateAdminAuth(authorization);

        try {
            statsService.refreshAllStats();
            return ApiResponse.ok(null);
        } catch (Exception e) {
            return ApiResponse.error(50000, "刷新统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 测试DNS记录统计（调试用）
     */
    @GetMapping("/debug/dns-records")
    public ApiResponse<Map<String, Object>> debugDnsRecords(@RequestHeader("Authorization") String authorization) {
        validateAdminAuth(authorization);

        try {
            Map<String, Object> result = statsService.getDnsRecordStats();
            return ApiResponse.ok(result);
        } catch (Exception e) {
            return ApiResponse.error(50000, "调试DNS记录统计失败: " + e.getMessage());
        }
    }

    /**
     * 验证管理员权限
     */
    private void validateAdminAuth(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new RuntimeException("无效的认证头");
        }

        String token = authorization.substring(7);
        Jws<Claims> claims = jwtService.parse(token);

        if (claims == null) {
            throw new RuntimeException("无效的token");
        }

        String role = claims.getBody().get("role", String.class);
        if (!"ADMIN".equals(role)) {
            throw new RuntimeException("权限不足");
        }
    }
}