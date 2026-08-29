package com.domaindns.auth;

import com.domaindns.auth.dto.AuthDtos.AdminRegisterReq;
import com.domaindns.auth.dto.AuthDtos.ForgotPasswordReq;
import com.domaindns.auth.dto.AuthDtos.LoginReq;
import com.domaindns.auth.dto.AuthDtos.LoginResp;
import com.domaindns.auth.dto.AuthDtos.RegisterReq;
import com.domaindns.auth.dto.AuthDtos.RegisterResp;
import com.domaindns.auth.dto.AuthDtos.ResetPasswordReq;
import com.domaindns.auth.dto.AuthDtos.SendRegisterCodeReq;
import com.domaindns.auth.service.AuthService;
import com.domaindns.auth.service.JwtService;
import com.domaindns.common.ApiResponse;
import com.domaindns.common.ClientIpUtils;
import com.domaindns.common.EmailWhitelistService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;
    private final StringRedisTemplate redis;
    private final EmailWhitelistService emailWhitelistService;

    @Value("${app.security.allow-public-admin-register:false}")
    private boolean allowPublicAdminRegister;

    public AuthController(AuthService authService, JwtService jwtService, StringRedisTemplate redis,
            EmailWhitelistService emailWhitelistService) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.redis = redis;
        this.emailWhitelistService = emailWhitelistService;
    }

    @GetMapping("/email-whitelist")
    public ApiResponse<EmailWhitelistInfo> getEmailWhitelist() {
        EmailWhitelistInfo info = new EmailWhitelistInfo(
                emailWhitelistService.getAllowedDomains(),
                emailWhitelistService.getAllowedEduSuffixes());
        return ApiResponse.ok(info);
    }

    @PostMapping("/register/send-code")
    public ApiResponse<Void> sendRegisterCode(@Valid @RequestBody SendRegisterCodeReq req, HttpServletRequest request) {
        authService.sendRegisterCode(req.email, ClientIpUtils.resolveClientIp(request), request.getHeader("User-Agent"));
        return ApiResponse.ok(null);
    }

    @PostMapping("/register")
    public ApiResponse<RegisterResp> register(@Valid @RequestBody RegisterReq req) {
        RegisterResp resp = authService.registerUser(req);
        return ApiResponse.ok(resp);
    }

    @PostMapping("/login")
    public ApiResponse<LoginResp> login(@Valid @RequestBody LoginReq req, HttpServletRequest request) {
        String ipAddress = ClientIpUtils.resolveClientIp(request);
        LoginResp resp = authService.loginUser(req, ipAddress);
        return ApiResponse.ok(resp);
    }

    @PostMapping("/admin/register")
    public ApiResponse<RegisterResp> adminRegister(@Valid @RequestBody AdminRegisterReq req) {
        if (!allowPublicAdminRegister) {
            throw new IllegalArgumentException("管理员公开注册已关闭");
        }
        RegisterResp resp = authService.registerAdminSimple(req.username, req.email, req.password);
        return ApiResponse.ok(resp);
    }

    @PostMapping("/admin/login")
    public ApiResponse<LoginResp> adminLogin(@Valid @RequestBody LoginReq req, HttpServletRequest request) {
        String ipAddress = ClientIpUtils.resolveClientIp(request);
        LoginResp resp = authService.loginAdmin(req, ipAddress);
        return ApiResponse.ok(resp);
    }

    @PostMapping("/forgot")
    public ApiResponse<Void> forgot(@Valid @RequestBody ForgotPasswordReq req, HttpServletRequest request) {
        authService.startReset(req.email, ClientIpUtils.resolveClientIp(request), request.getHeader("User-Agent"));
        return ApiResponse.ok(null);
    }

    @PostMapping("/reset")
    public ApiResponse<Void> reset(@Valid @RequestBody ResetPasswordReq req) {
        authService.resetPassword(req.email, req.code, req.newPassword);
        return ApiResponse.ok(null);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            try {
                Jws<Claims> jws = jwtService.parse(token);
                String jti = jws.getBody().getId();
                Date exp = jws.getBody().getExpiration();
                if (jti != null && exp != null) {
                    long ttl = Math.max(1, (exp.getTime() - System.currentTimeMillis()) / 1000);
                    redis.opsForValue().set("jwt:blacklist:" + jti, "1", Duration.ofSeconds(ttl));
                }
            } catch (Exception ignored) {
            }
        }
        return ApiResponse.ok(null);
    }

    public static class EmailWhitelistInfo {
        private final List<String> allowedDomains;
        private final List<String> allowedEduSuffixes;

        public EmailWhitelistInfo(List<String> allowedDomains, List<String> allowedEduSuffixes) {
            this.allowedDomains = allowedDomains;
            this.allowedEduSuffixes = allowedEduSuffixes;
        }

        public List<String> getAllowedDomains() {
            return allowedDomains;
        }

        public List<String> getAllowedEduSuffixes() {
            return allowedEduSuffixes;
        }
    }
}
