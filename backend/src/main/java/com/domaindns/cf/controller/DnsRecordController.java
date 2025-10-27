package com.domaindns.cf.controller;

import com.domaindns.auth.service.JwtService;
import com.domaindns.cf.service.DnsRecordService;
import com.domaindns.common.ApiResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/zones/{zoneId}")
public class DnsRecordController {
    private final DnsRecordService service;
    private final JwtService jwtService;

    public DnsRecordController(DnsRecordService service, JwtService jwtService) {
        this.service = service;
        this.jwtService = jwtService;
    }

    @PostMapping("/sync-records")
    public ApiResponse<Void> sync(@RequestHeader("Authorization") String authorization,
            @PathVariable("zoneId") Long zoneDbId) {
        // 验证管理员权限
        validateAdminAuth(authorization);
        service.syncZoneRecords(zoneDbId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/records")
    public ApiResponse<List<com.domaindns.cf.dto.DnsRecordDtos.DnsRecordWithUser>> list(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("zoneId") Long zoneDbId,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "name", required = false) String name) {
        // 验证管理员权限
        validateAdminAuth(authorization);
        return ApiResponse.ok(service.listWithUser(zoneDbId, type, name));
    }

    @PostMapping("/records")
    public ApiResponse<Void> create(@RequestHeader("Authorization") String authorization,
            @PathVariable("zoneId") Long zoneDbId, @RequestBody String body) throws Exception {
        // 验证管理员权限
        validateAdminAuth(authorization);
        service.create(zoneDbId, body);
        return ApiResponse.ok(null);
    }

    @PutMapping("/records/{recordId}")
    public ApiResponse<Void> update(@RequestHeader("Authorization") String authorization,
            @PathVariable("zoneId") Long zoneDbId, @PathVariable String recordId,
            @RequestBody String body) throws Exception {
        // 验证管理员权限
        validateAdminAuth(authorization);
        service.update(zoneDbId, recordId, body);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/records/{recordId}")
    public ApiResponse<Void> delete(@RequestHeader("Authorization") String authorization,
            @PathVariable("zoneId") Long zoneDbId, @PathVariable String recordId)
            throws Exception {
        // 验证管理员权限
        validateAdminAuth(authorization);
        service.delete(zoneDbId, recordId);
        return ApiResponse.ok(null);
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