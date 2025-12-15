package com.domaindns.auth;

import com.domaindns.auth.dto.AuthDtos.ForgotPasswordReq;
import com.domaindns.auth.dto.AuthDtos.LoginReq;
import com.domaindns.auth.dto.AuthDtos.LoginResp;
import com.domaindns.auth.dto.AuthDtos.RegisterReq;
import com.domaindns.auth.dto.AuthDtos.RegisterResp;
import com.domaindns.auth.dto.AuthDtos.ResetPasswordReq;
import com.domaindns.auth.dto.AuthDtos.SendRegisterCodeReq;
import com.domaindns.auth.dto.AuthDtos.AdminRegisterReq;
import com.domaindns.auth.service.AuthService;
import com.domaindns.auth.service.JwtService;
import com.domaindns.common.ApiResponse;
import com.domaindns.common.EmailWhitelistService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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

    public AuthController(AuthService authService, JwtService jwtService, StringRedisTemplate redis,
            EmailWhitelistService emailWhitelistService) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.redis = redis;
        this.emailWhitelistService = emailWhitelistService;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    // ---------------- 邮箱白名单 ----------------
    @GetMapping("/email-whitelist")
    public ApiResponse<EmailWhitelistInfo> getEmailWhitelist() {
        EmailWhitelistInfo info = new EmailWhitelistInfo(
                emailWhitelistService.getAllowedDomains(),
                emailWhitelistService.getAllowedEduSuffixes());
        return ApiResponse.ok(info);
    }

    // ---------------- 注册验证码 ----------------
    @PostMapping("/register/send-code")
    public ApiResponse<Void> sendRegisterCode(@Valid @RequestBody SendRegisterCodeReq req) {
        authService.sendRegisterCode(req.email);
        return ApiResponse.ok(null);
    }

    // ---------------- 用户 ----------------
    @PostMapping("/register")
    public ApiResponse<RegisterResp> register(@Valid @RequestBody RegisterReq req) {
        RegisterResp resp = authService.registerUser(req);
        return ApiResponse.ok(resp);
    }

    @PostMapping("/login")
    public ApiResponse<LoginResp> login(@Valid @RequestBody LoginReq req, HttpServletRequest request) {
        String ipAddress = getClientIp(request);
        LoginResp resp = authService.loginUser(req, ipAddress);
        return ApiResponse.ok(resp);
    }

    // ---------------- 管理员 ----------------
    @PostMapping("/admin/register")
    public ApiResponse<RegisterResp> adminRegister(@Valid @RequestBody AdminRegisterReq req) {
        RegisterResp resp = authService.registerAdminSimple(req.username, req.email, req.password);
        return ApiResponse.ok(resp);
    }

    @PostMapping("/admin/login")
    public ApiResponse<LoginResp> adminLogin(@Valid @RequestBody LoginReq req, HttpServletRequest request) {
        String ipAddress = getClientIp(request);
        LoginResp resp = authService.loginAdmin(req, ipAddress);
        return ApiResponse.ok(resp);
    }

    // ---------------- 找回密码 ----------------
    @PostMapping("/forgot")
    public ApiResponse<Void> forgot(@Valid @RequestBody ForgotPasswordReq req) {
        authService.startReset(req.email);
        return ApiResponse.ok(null);
    }

    @PostMapping("/reset")
    public ApiResponse<Void> reset(@Valid @RequestBody ResetPasswordReq req) {
        authService.resetPassword(req.email, req.code, req.newPassword);
        return ApiResponse.ok(null);
    }

    // ---------------- 注销 ----------------
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

    // 邮箱白名单信息类
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
