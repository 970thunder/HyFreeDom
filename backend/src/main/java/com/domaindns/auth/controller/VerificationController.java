package com.domaindns.auth.controller;

import com.domaindns.auth.dto.AuthDtos.VerificationReq;
import com.domaindns.auth.dto.AuthDtos.VerificationStatusResp;
import com.domaindns.auth.service.JwtService;
import com.domaindns.auth.service.VerificationService;
import com.domaindns.common.ApiResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/verification")
public class VerificationController {

    private final VerificationService verificationService;
    private final JwtService jwtService;

    public VerificationController(VerificationService verificationService, JwtService jwtService) {
        this.verificationService = verificationService;
        this.jwtService = jwtService;
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

    private long currentUserId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer "))
            throw new RuntimeException("未登录");
        String token = authorization.substring(7);
        Jws<Claims> jws = jwtService.parse(token);
        return Long.parseLong(jws.getBody().getSubject());
    }
}
