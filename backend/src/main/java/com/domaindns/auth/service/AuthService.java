package com.domaindns.auth.service;

import com.domaindns.auth.dto.AuthDtos;
import com.domaindns.auth.dto.AuthDtos.LoginReq;
import com.domaindns.auth.dto.AuthDtos.LoginResp;
import com.domaindns.auth.dto.AuthDtos.RegisterReq;
import com.domaindns.auth.dto.AuthDtos.RegisterResp;
import com.domaindns.auth.entity.User;
import com.domaindns.auth.mapper.UserMapper;
import com.domaindns.common.RateLimiter;
import com.domaindns.common.EmailService;
import com.domaindns.common.EmailWhitelistService;
import com.domaindns.settings.SettingsService;
import com.domaindns.user.mapper.PointsMapper;
import com.domaindns.admin.mapper.InviteMapper;
import com.domaindns.admin.model.InviteCode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Random;

@Service
public class AuthService {
    private final UserMapper userMapper;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final StringRedisTemplate redis;
    private final RateLimiter rateLimiter;
    private final SettingsService settingsService;
    private final PointsMapper pointsMapper;
    private final InviteMapper inviteMapper;
    private final EmailWhitelistService emailWhitelistService;
    private final com.domaindns.auth.mapper.UserProfileMapper userProfileMapper;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthService(UserMapper userMapper, JwtService jwtService, EmailService emailService,
            StringRedisTemplate redis, RateLimiter rateLimiter, SettingsService settingsService,
            PointsMapper pointsMapper, InviteMapper inviteMapper, EmailWhitelistService emailWhitelistService,
            com.domaindns.auth.mapper.UserProfileMapper userProfileMapper) {
        this.userMapper = userMapper;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.redis = redis;
        this.rateLimiter = rateLimiter;
        this.settingsService = settingsService;
        this.pointsMapper = pointsMapper;
        this.inviteMapper = inviteMapper;
        this.emailWhitelistService = emailWhitelistService;
        this.userProfileMapper = userProfileMapper;
    }

    public void sendRegisterCode(String email) {
        boolean allowed = rateLimiter.tryConsume("rl:regcode:" + email, 3, Duration.ofMinutes(1));
        if (!allowed)
            throw new IllegalArgumentException("请求过于频繁，请稍后再试");
        String code = String.format("%06d", new Random().nextInt(1_000_000));
        redis.opsForValue().set("regcode:" + email, code, Duration.ofMinutes(10));

        // 异步发送邮件，立即返回
        emailService.sendVerificationCode(email, code, "register");
    }

    @Transactional
    public RegisterResp registerUser(RegisterReq req) {
        if (req.email == null || req.email.isBlank())
            throw new IllegalArgumentException("邮箱必填");
        String key = "regcode:" + req.email;
        String expect = redis.opsForValue().get(key);
        if (expect == null || !expect.equals(req.emailCode))
            throw new IllegalArgumentException("邮箱验证码无效或已过期");
        RegisterResp resp = doRegister(req.username, req.email, req.password, "USER", req.inviteCode);
        redis.delete(key);
        return resp;
    }

    @Transactional
    public RegisterResp registerAdmin(RegisterReq req) {
        int adminCount = userMapper.countByRole("ADMIN");
        if (adminCount > 0) {
            throw new IllegalArgumentException("管理员已存在，禁止再次注册，请使用账号密码登录或通过邮箱找回密码");
        }
        return doRegister(req.username, req.email, req.password, "ADMIN", null);
    }

    @Transactional
    public RegisterResp registerAdminSimple(String username, String email, String password) {
        int adminCount = userMapper.countByRole("ADMIN");
        if (adminCount > 0) {
            throw new IllegalArgumentException("管理员已存在，禁止再次注册，请使用账号密码登录或通过邮箱找回密码");
        }
        return doRegister(username, email, password, "ADMIN", null);
    }

    public LoginResp loginUser(LoginReq req, String ipAddress) {
        return doLogin(req, "USER", ipAddress);
    }

    public LoginResp loginAdmin(LoginReq req, String ipAddress) {
        return doLogin(req, "ADMIN", ipAddress);
    }

    private RegisterResp doRegister(String username, String email, String password, String role, String inviteCode) {
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("用户名必填");
        if (password == null || password.isBlank())
            throw new IllegalArgumentException("密码必填");
        if (userMapper.findByUsername(username) != null)
            throw new IllegalArgumentException("用户名已存在");
        if (email != null && !email.isBlank() && userMapper.findByEmail(email) != null)
            throw new IllegalArgumentException("邮箱已存在");

        // 邮箱白名单验证
        if (email != null && !email.isBlank()) {
            EmailWhitelistService.EmailValidationResult validationResult = emailWhitelistService.validateEmail(email);
            if (!validationResult.isValid()) {
                throw new IllegalArgumentException(validationResult.getMessage());
            }
        }

        // 处理邀请码
        Long inviterId = null;
        if (inviteCode != null && !inviteCode.isBlank()) {
            InviteCode invite = inviteMapper.findByCode(inviteCode);
            if (invite == null || !"ACTIVE".equals(invite.getStatus()))
                throw new IllegalArgumentException("邀请码无效或已失效");
            if (invite.getExpiredAt() != null && invite.getExpiredAt().isBefore(java.time.LocalDateTime.now()))
                throw new IllegalArgumentException("邀请码已过期");
            if (invite.getMaxUses() != null && invite.getMaxUses() > 0 && invite.getUsedCount() >= invite.getMaxUses())
                throw new IllegalArgumentException("邀请码使用次数已达上限");
            inviterId = invite.getOwnerUserId();
        }

        // 获取系统设置
        java.util.Map<String, String> settings = settingsService.getAll();
        int initialPoints = Integer.parseInt(settings.getOrDefault("initial_register_points", "5"));
        int inviteePoints = Integer.parseInt(settings.getOrDefault("invitee_points", "3"));
        int inviterPoints = Integer.parseInt(settings.getOrDefault("inviter_points", "3"));

        // 计算积分
        int totalPoints = initialPoints;
        if (inviterId != null) {
            totalPoints += inviteePoints;
        }

        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(password));
        u.setDisplayName(username);
        u.setInviteCode(null); // 新用户注册时，自己的邀请码字段为空
        u.setInviterId(inviterId); // 这里存储邀请人的ID
        u.setPoints(totalPoints);
        u.setRole(role);
        u.setStatus(1);
        userMapper.insert(u);

        // 记录积分流水
        pointsMapper.insertTxn(u.getId(), initialPoints, totalPoints, "REGISTER", "注册赠送积分", null);
        if (inviterId != null) {
            pointsMapper.insertTxn(u.getId(), inviteePoints, totalPoints, "INVITE_CODE", "使用邀请码奖励积分", inviterId);

            // 给邀请人加积分
            pointsMapper.adjust(inviterId, inviterPoints);
            pointsMapper.insertTxn(inviterId, inviterPoints, null, "INVITE_REWARD", "邀请用户 " + username + " 获得奖励积分",
                    u.getId());

            // 更新邀请码使用次数
            inviteMapper.incrementUsedCount(inviteCode);
        }

        // 发送欢迎邮件（异步）
        if (email != null && !email.isBlank()) {
            emailService.sendWelcomeEmail(email, username);
        }

        RegisterResp resp = new RegisterResp();
        resp.userId = u.getId();
        return resp;
    }

    private LoginResp doLogin(LoginReq req, String requiredRole, String ipAddress) {
        User u = null;
        if (req.username != null && !req.username.isBlank()) {
            u = userMapper.findByUsername(req.username);
        }
        if (u == null && req.email != null && !req.email.isBlank()) {
            u = userMapper.findByEmail(req.email);
        }
        if (u == null)
            throw new IllegalArgumentException("用户不存在或密码错误");
        if (!encoder.matches(req.password, u.getPasswordHash()))
            throw new IllegalArgumentException("用户不存在或密码错误");
        if (!requiredRole.equals(u.getRole()))
            throw new IllegalArgumentException("角色不匹配：需要" + requiredRole);

        // 检查用户状态
        if (u.getStatus() == null || u.getStatus() != 1) {
            if (u.getStatus() == 2) {
                throw new IllegalArgumentException("账号已注销，无法登录");
            } else if (u.getStatus() == 0) {
                throw new IllegalArgumentException("账号已被禁用，无法登录");
            } else {
                throw new IllegalArgumentException("账号状态异常，无法登录");
            }
        }

        // 更新 IP 地址
        if (ipAddress != null) {
            userMapper.updateIp(u.getId(), ipAddress);
        }

        LoginResp resp = new LoginResp();
        resp.role = u.getRole();
        resp.token = jwtService.issueToken(String.valueOf(u.getId()), u.getRole());

        // 添加用户信息
        AuthDtos.UserInfo userInfo = new AuthDtos.UserInfo();
        userInfo.id = u.getId();
        userInfo.username = u.getUsername();
        userInfo.email = u.getEmail();
        userInfo.displayName = u.getDisplayName();
        userInfo.points = u.getPoints();
        userInfo.role = u.getRole();

        com.domaindns.auth.entity.UserProfile profile = userProfileMapper.findByUserId(u.getId());
        userInfo.isVerified = profile != null && profile.getIsVerified() != null && profile.getIsVerified() == 1;

        resp.user = userInfo;

        return resp;
    }

    public void startReset(String email) {
        User u = userMapper.findByEmail(email);
        if (u == null)
            throw new IllegalArgumentException("邮箱不存在");
        boolean allowed = rateLimiter.tryConsume("rl:reset:" + email, 3, Duration.ofMinutes(1));
        if (!allowed)
            throw new IllegalArgumentException("请求过于频繁，请稍后再试");
        String code = String.format("%06d", new Random().nextInt(1_000_000));
        String key2 = "pwdreset:" + email;
        redis.opsForValue().set(key2, code, Duration.ofMinutes(10));

        // 异步发送邮件，立即返回
        emailService.sendVerificationCode(email, code, "reset");
    }

    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        String key = "pwdreset:" + email;
        String expect = redis.opsForValue().get(key);
        if (expect == null || !expect.equals(code)) {
            throw new IllegalArgumentException("验证码无效或已过期");
        }
        User u = userMapper.findByEmail(email);
        if (u == null)
            throw new IllegalArgumentException("邮箱不存在");
        String hash = encoder.encode(newPassword);
        userMapper.updatePassword(u.getId(), hash);
        redis.delete(key);
    }
}
