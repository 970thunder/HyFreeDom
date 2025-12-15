package com.domaindns.user.controller;

import com.domaindns.common.ApiResponse;
import com.domaindns.user.service.UserDomainService;
import com.domaindns.auth.service.JwtService;
import com.domaindns.auth.service.VerificationService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user/domains")
public class UserDomainController {
    private final UserDomainService service;
    private final JwtService jwtService;
    private final VerificationService verificationService;

    public UserDomainController(UserDomainService service, JwtService jwtService,
            VerificationService verificationService) {
        this.service = service;
        this.jwtService = jwtService;
        this.verificationService = verificationService;
    }

    private void checkVerified(long userId) {
        if (!verificationService.isVerified(userId)) {
            throw new RuntimeException("请先完成实名认证");
        }
    }

    @PostMapping("/apply")
    public ApiResponse<Map<String, Object>> apply(@RequestHeader("Authorization") String authorization,
            @RequestBody Map<String, Object> body) {
        long userId = currentUserId(authorization);
        checkVerified(userId);
        Object zoneKey = body.get("zoneId");
        String prefix = String.valueOf(body.get("prefix"));
        String type = String.valueOf(body.get("type"));
        String value = String.valueOf(body.get("value"));
        Integer ttl = body.get("ttl") == null ? null : Integer.valueOf(body.get("ttl").toString());
        String remark = body.get("remark") == null ? null : String.valueOf(body.get("remark"));
        service.applySubdomain(userId, zoneKey, prefix, type, value, ttl, remark);
        return ApiResponse.ok(Map.of("status", "ok"));
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> myDomains(@RequestHeader("Authorization") String authorization,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        long userId = currentUserId(authorization);
        checkVerified(userId);
        return ApiResponse.ok(service.listUserDomains(userId, page, size));
    }

    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> update(@RequestHeader("Authorization") String authorization,
            @PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        long userId = currentUserId(authorization);
        checkVerified(userId);
        String type = String.valueOf(body.get("type"));
        String value = String.valueOf(body.get("value"));
        Integer ttl = body.get("ttl") == null ? null : Integer.valueOf(body.get("ttl").toString());
        String remark = body.get("remark") == null ? null : String.valueOf(body.get("remark"));
        service.updateDomainRecord(userId, id, type, value, ttl, remark);
        return ApiResponse.ok(Map.of("status", "ok"));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Object>> release(@RequestHeader("Authorization") String authorization,
            @PathVariable("id") Long id) {
        long userId = currentUserId(authorization);
        checkVerified(userId);
        service.releaseDomain(userId, id);
        return ApiResponse.ok(Map.of("status", "ok"));
    }

    private long currentUserId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer "))
            throw new RuntimeException("未登录");
        String token = authorization.substring(7);
        Jws<Claims> jws = jwtService.parse(token);
        return Long.parseLong(jws.getBody().getSubject());
    }
}
