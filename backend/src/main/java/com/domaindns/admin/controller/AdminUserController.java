package com.domaindns.admin.controller;

import com.domaindns.admin.mapper.AdminUserMapper;
import com.domaindns.admin.model.AdminUser;
import com.domaindns.auth.mapper.UserMapper;
import com.domaindns.auth.service.JwtService;
import com.domaindns.common.ApiResponse;
import com.domaindns.user.mapper.PointsMapper;
import com.domaindns.user.mapper.UserDomainMapper;
import com.domaindns.user.model.UserDomain;
import com.domaindns.user.service.UserDomainService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final AdminUserMapper mapper;
    private final PointsMapper pointsMapper;
    private final UserMapper userMapper;
    private final UserDomainMapper userDomainMapper;
    private final UserDomainService userDomainService;
    private final JwtService jwtService;

    public AdminUserController(AdminUserMapper mapper, PointsMapper pointsMapper, UserMapper userMapper,
            UserDomainMapper userDomainMapper, UserDomainService userDomainService, JwtService jwtService) {
        this.mapper = mapper;
        this.pointsMapper = pointsMapper;
        this.userMapper = userMapper;
        this.userDomainMapper = userDomainMapper;
        this.userDomainService = userDomainService;
        this.jwtService = jwtService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestHeader("Authorization") String authorization,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "isVerified", required = false) Integer isVerified,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size) {
        // 验证管理员权限
        validateAdminAuth(authorization);

        int offset = (Math.max(page, 1) - 1) * Math.max(size, 1);
        List<AdminUser> list = mapper.list(status, role, isVerified, keyword, offset, size);
        int total = mapper.count(status, role, isVerified, keyword);
        Map<String, Object> m = new HashMap<>();
        m.put("list", list);
        m.put("total", total);
        m.put("page", page);
        m.put("size", size);
        return ApiResponse.ok(m);
    }

    @PostMapping("/{id}/points")
    public ApiResponse<Map<String, Object>> adjust(@RequestHeader("Authorization") String authorization,
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        // 验证管理员权限
        validateAdminAuth(authorization);

        Integer delta = (Integer) body.getOrDefault("delta", 0);
        String remark = (String) body.getOrDefault("remark", "管理员调整积分");

        // 获取用户当前积分
        AdminUser user = mapper.findById(id);
        if (user == null) {
            return ApiResponse.error(40001, "用户不存在");
        }

        Integer currentPoints = user.getPoints() != null ? user.getPoints() : 0;
        Integer newPoints = currentPoints + delta;

        // 更新用户积分
        int rows = mapper.adjustPoints(id, delta);
        if (rows == 0) {
            return ApiResponse.error(50000, "更新失败");
        }

        // 记录积分交易
        pointsMapper.insertTxn(
                id, // userId
                delta, // changeAmount
                newPoints, // balanceAfter
                "ADMIN_ADJUST", // type
                remark, // remark
                null // relatedId
        );

        Map<String, Object> m = new HashMap<>();
        m.put("updated", rows);
        m.put("oldPoints", currentPoints);
        m.put("newPoints", newPoints);
        m.put("delta", delta);
        return ApiResponse.ok(m);
    }

    @PostMapping("/{id}/reset-password")
    public ApiResponse<Map<String, Object>> resetPassword(@RequestHeader("Authorization") String authorization,
            @PathVariable Long id) {
        // 验证管理员权限
        validateAdminAuth(authorization);

        AdminUser user = mapper.findById(id);
        if (user == null) {
            return ApiResponse.error(40001, "用户不存在");
        }

        // 默认密码，可根据需要调整或改为从配置读取
        String defaultPassword = "123456";
        // 为了符合你提出的格式，追加固定后缀
        String finalPassword = defaultPassword + "@";

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(finalPassword);
        int rows = userMapper.updatePassword(id, hash);
        if (rows == 0) {
            return ApiResponse.error(50000, "密码重置失败");
        }
        Map<String, Object> m = new HashMap<>();
        m.put("updated", rows);
        m.put("password", "123456@ (已加密存储)");
        return ApiResponse.ok(m);
    }

    @PostMapping("/{id}/status")
    public ApiResponse<Map<String, Object>> updateStatus(@RequestHeader("Authorization") String authorization,
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        // 验证管理员权限
        validateAdminAuth(authorization);

        Integer status = (Integer) body.get("status");
        if (status == null || (status != 0 && status != 1)) {
            return ApiResponse.error(40000, "状态值无效，必须为0（禁用）或1（启用）");
        }

        AdminUser user = mapper.findById(id);
        if (user == null) {
            return ApiResponse.error(40001, "用户不存在");
        }

        int rows = mapper.updateStatus(id, status);
        if (rows == 0) {
            return ApiResponse.error(50000, "状态更新失败");
        }

        Map<String, Object> m = new HashMap<>();
        m.put("updated", rows);
        m.put("oldStatus", user.getStatus());
        m.put("newStatus", status);
        return ApiResponse.ok(m);
    }

    @PostMapping("/{id}/role")
    public ApiResponse<Map<String, Object>> updateRole(@RequestHeader("Authorization") String authorization,
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        // 验证管理员权限
        validateAdminAuth(authorization);

        String role = (String) body.get("role");
        if (role == null || (!role.equals("USER") && !role.equals("ADMIN"))) {
            return ApiResponse.error(40000, "角色值无效，必须为USER或ADMIN");
        }

        AdminUser user = mapper.findById(id);
        if (user == null) {
            return ApiResponse.error(40001, "用户不存在");
        }

        int rows = mapper.updateRole(id, role);
        if (rows == 0) {
            return ApiResponse.error(50000, "角色更新失败");
        }

        Map<String, Object> m = new HashMap<>();
        m.put("updated", rows);
        m.put("oldRole", user.getRole());
        m.put("newRole", role);
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

    /**
     * 管理员注销用户账号
     * 需要先释放用户的所有域名，然后软删除（status=2）
     */
    @PostMapping("/{id}/delete")
    public ApiResponse<Map<String, Object>> deleteUser(@RequestHeader("Authorization") String authorization,
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        validateAdminAuth(authorization);

        // 验证确认信息
        Boolean confirmDeletion = (Boolean) body.get("confirmDeletion");
        Boolean confirmDnsRelease = (Boolean) body.get("confirmDnsRelease");

        if (confirmDeletion == null || !confirmDeletion) {
            return ApiResponse.error(40001, "请确认注销操作");
        }

        if (confirmDnsRelease == null || !confirmDnsRelease) {
            return ApiResponse.error(40002, "请确认释放DNS域名");
        }

        AdminUser user = mapper.findById(id);
        if (user == null) {
            return ApiResponse.error(40001, "用户不存在");
        }

        // 检查用户状态
        if (user.getStatus() == null || user.getStatus() != 1) {
            return ApiResponse.error(40003, "用户状态异常，无法注销");
        }

        // 获取用户的所有域名
        List<UserDomain> domains = userDomainMapper.listByUser(id, 0, 1000);
        int releasedDomains = 0;
        int failedDomains = 0;

        // 释放用户的所有域名
        for (UserDomain domain : domains) {
            try {
                userDomainService.releaseDomain(id, domain.getId());
                releasedDomains++;
            } catch (Exception e) {
                System.err.println("释放域名失败: " + domain.getFullDomain() + ", 错误: " + e.getMessage());
                failedDomains++;
                // 继续释放其他域名，不因为单个域名失败而停止整个流程
            }
        }

        // 软删除：将状态设置为2（已注销）
        int rows = mapper.updateStatus(id, 2);
        if (rows == 0) {
            return ApiResponse.error(50000, "用户注销失败");
        }

        Map<String, Object> m = new HashMap<>();
        m.put("deleted", rows);
        m.put("username", user.getUsername());
        m.put("deletedAt", java.time.LocalDateTime.now());
        m.put("releasedDomains", releasedDomains);
        m.put("failedDomains", failedDomains);
        m.put("totalDomains", domains.size());

        return ApiResponse.ok(m);
    }

    /**
     * 获取用户的域名信息（用于注销确认）
     */
    @GetMapping("/{id}/domains")
    public ApiResponse<Map<String, Object>> getUserDomains(@RequestHeader("Authorization") String authorization,
            @PathVariable Long id) {
        validateAdminAuth(authorization);

        AdminUser user = mapper.findById(id);
        if (user == null) {
            return ApiResponse.error(40001, "用户不存在");
        }

        // 查询用户的域名信息
        List<UserDomain> domains = userDomainMapper.listByUser(id, 0, 1000); // 获取所有域名
        int domainCount = userDomainMapper.countByUser(id);

        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("displayName", user.getDisplayName());
        result.put("points", user.getPoints());
        result.put("domains", domains);
        result.put("domainCount", domainCount);

        return ApiResponse.ok(result);
    }
}