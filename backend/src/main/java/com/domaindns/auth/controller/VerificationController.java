package com.domaindns.auth.controller;

import java.util.Map;

import com.domaindns.auth.dto.AuthDtos.VerificationReq;
import com.domaindns.auth.dto.AuthDtos.VerificationStatusResp;
import com.domaindns.auth.service.JwtService;
import com.domaindns.auth.service.VerificationService;
import com.domaindns.common.ApiResponse;
import com.domaindns.settings.SettingsService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/verification")
public class VerificationController {

    private final VerificationService verificationService;
    private final JwtService jwtService;
    private final SettingsService settingsService;

    public VerificationController(VerificationService verificationService, JwtService jwtService,
            SettingsService settingsService) {
        this.verificationService = verificationService;
        this.jwtService = jwtService;
        this.settingsService = settingsService;
    }

    @PostMapping
    public ApiResponse<Void> verify(@RequestHeader("Authorization") String authorization,
            @Valid @RequestBody VerificationReq req) {
        long userId = currentUserId(authorization);
        verificationService.verify(userId, req);
        return ApiResponse.ok(null);
    }

    @GetMapping
    public ApiResponse<VerificationStatusResp> getStatus(@RequestHeader("Authorization") String authorization) {
        long userId = currentUserId(authorization);
        return ApiResponse.ok(verificationService.getStatus(userId));
    }

    @GetMapping("/reward-config")
    public ApiResponse<Map<String, Object>> getRewardConfig() {
        String rewardStr = settingsService.get("verification_reward_points", "15");
        int rewardPoints = 15;
        try {
            rewardPoints = Integer.parseInt(rewardStr);
        } catch (NumberFormatException e) {
        }
        return ApiResponse.ok(Map.of("points", rewardPoints));
    }

    private long currentUserId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer "))
            throw new RuntimeException("未登录");
        String token = authorization.substring(7);
        Jws<Claims> jws = jwtService.parse(token);
        return Long.parseLong(jws.getBody().getSubject());
    }
}
