package com.domaindns.auth.service;

import com.domaindns.admin.mapper.InviteMapper;
import com.domaindns.admin.model.InviteCode;
import com.domaindns.auth.dto.AuthDtos;
import com.domaindns.auth.dto.AuthDtos.LoginReq;
import com.domaindns.auth.dto.AuthDtos.LoginResp;
import com.domaindns.auth.dto.AuthDtos.RegisterReq;
import com.domaindns.auth.dto.AuthDtos.RegisterResp;
import com.domaindns.auth.entity.User;
import com.domaindns.auth.entity.UserProfile;
import com.domaindns.auth.mapper.UserMapper;
import com.domaindns.auth.mapper.UserProfileMapper;
import com.domaindns.common.EmailService;
import com.domaindns.common.EmailWhitelistService;
import com.domaindns.common.RateLimiter;
import com.domaindns.settings.SettingsService;
import com.domaindns.user.mapper.PointsMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;

@Service
public class AuthService {
    private static final Duration ADMIN_LOGIN_WINDOW = Duration.ofMinutes(15);
    private static final Duration ADMIN_LOGIN_LOCK = Duration.ofMinutes(30);
    private static final int ADMIN_LOGIN_IP_ATTEMPT_LIMIT = 20;
    private static final int ADMIN_LOGIN_ACCOUNT_ATTEMPT_LIMIT = 10;
    private static final int ADMIN_LOGIN_FAILURE_LOCK_THRESHOLD = 5;

    private final UserMapper userMapper;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final StringRedisTemplate redis;
    private final RateLimiter rateLimiter;
    private final SettingsService settingsService;
    private final PointsMapper pointsMapper;
    private final InviteMapper inviteMapper;
    private final EmailWhitelistService emailWhitelistService;
    private final UserProfileMapper userProfileMapper;
    private final VerificationService verificationService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthService(UserMapper userMapper, JwtService jwtService, EmailService emailService,
            StringRedisTemplate redis, RateLimiter rateLimiter, SettingsService settingsService,
            PointsMapper pointsMapper, InviteMapper inviteMapper, EmailWhitelistService emailWhitelistService,
            UserProfileMapper userProfileMapper, VerificationService verificationService) {
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
        this.verificationService = verificationService;
    }

    public void sendRegisterCode(String email) {
        boolean allowed = rateLimiter.tryConsume("rl:regcode:" + email, 3, Duration.ofMinutes(1));
        if (!allowed) {
            throw new IllegalArgumentException("请求过于频繁，请稍后再试");
        }
        String code = String.format("%06d", new Random().nextInt(1_000_000));
        redis.opsForValue().set("regcode:" + email, code, Duration.ofMinutes(10));
        emailService.sendVerificationCode(email, code, "register");
    }

    @Transactional
    public RegisterResp registerUser(RegisterReq req) {
        if (req.email == null || req.email.isBlank()) {
            throw new IllegalArgumentException("邮箱必填");
        }
        String key = "regcode:" + req.email;
        String expect = redis.opsForValue().get(key);
        if (expect == null || !expect.equals(req.emailCode)) {
            throw new IllegalArgumentException("邮箱验证码无效或已过期");
        }
        RegisterResp resp = doRegister(req.username, req.email, req.password, "USER", req.inviteCode);
        redis.delete(key);
        return resp;
    }

    @Transactional
    public RegisterResp registerAdmin(RegisterReq req) {
        int adminCount = userMapper.countByRole("ADMIN");
        if (adminCount > 0) {
            throw new IllegalArgumentException("管理员已存在，请直接登录");
        }
        return doRegister(req.username, req.email, req.password, "ADMIN", null);
    }

    @Transactional
    public RegisterResp registerAdminSimple(String username, String email, String password) {
        int adminCount = userMapper.countByRole("ADMIN");
        if (adminCount > 0) {
            throw new IllegalArgumentException("管理员已存在，请直接登录");
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
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("用户名必填");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("密码必填");
        }
        if (userMapper.findByUsername(username) != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        if (email != null && !email.isBlank() && userMapper.findByEmail(email) != null) {
            throw new IllegalArgumentException("邮箱已存在");
        }
        if (email != null && !email.isBlank()) {
            EmailWhitelistService.EmailValidationResult validationResult = emailWhitelistService.validateEmail(email);
            if (!validationResult.isValid()) {
                throw new IllegalArgumentException(validationResult.getMessage());
            }
        }

        Long inviterId = null;
        if (inviteCode != null && !inviteCode.isBlank()) {
            InviteCode invite = inviteMapper.findByCode(inviteCode);
            if (invite == null || !"ACTIVE".equals(invite.getStatus())) {
                throw new IllegalArgumentException("邀请码无效或已失效");
            }
            if (invite.getExpiredAt() != null && invite.getExpiredAt().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("邀请码已过期");
            }
            if (invite.getMaxUses() != null && invite.getMaxUses() > 0
                    && invite.getUsedCount() >= invite.getMaxUses()) {
                throw new IllegalArgumentException("邀请码使用次数已达上限");
            }
            inviterId = invite.getOwnerUserId();
        }

        Map<String, String> settings = settingsService.getAll();
        int initialPoints = Integer.parseInt(settings.getOrDefault("initial_register_points", "5"));

        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(password));
        u.setDisplayName(username);
        u.setInviteCode(null);
        u.setInviterId(inviterId);
        u.setPoints(initialPoints);
        u.setRole(role);
        u.setStatus(1);
        userMapper.insert(u);

        pointsMapper.insertTxn(u.getId(), initialPoints, initialPoints, "REGISTER", "注册赠送积分", null);
        if (inviterId != null) {
            inviteMapper.incrementUsedCount(inviteCode);
            verificationService.processPendingInviteRewards(u.getId());
        }

        if (email != null && !email.isBlank()) {
            emailService.sendWelcomeEmail(email, username);
        }

        RegisterResp resp = new RegisterResp();
        resp.userId = u.getId();
        return resp;
    }

    private LoginResp doLogin(LoginReq req, String requiredRole, String ipAddress) {
        String account = req.username == null ? "" : req.username.trim();
        if (account.isBlank()) {
            throw new IllegalArgumentException("请输入账号");
        }

        if ("ADMIN".equals(requiredRole)) {
            enforceAdminLoginProtection(account, ipAddress);
        }

        try {
            User u = account.contains("@") ? userMapper.findByEmail(account) : userMapper.findByUsername(account);

            if (u == null || !encoder.matches(req.password, u.getPasswordHash())) {
                throw new IllegalArgumentException("用户不存在或密码错误");
            }
            if (!requiredRole.equals(u.getRole())) {
                throw new IllegalArgumentException("角色不匹配");
            }
            if (u.getStatus() == null || u.getStatus() != 1) {
                if (u.getStatus() != null && u.getStatus() == 2) {
                    throw new IllegalArgumentException("账号已注销，无法登录");
                }
                if (u.getStatus() != null && u.getStatus() == 0) {
                    throw new IllegalArgumentException("账号已被禁用，无法登录");
                }
                throw new IllegalArgumentException("账号状态异常，无法登录");
            }

            if ("ADMIN".equals(requiredRole)) {
                clearAdminLoginFailureState(account, ipAddress);
            }

            if (ipAddress != null && !ipAddress.isBlank()) {
                userMapper.updateIp(u.getId(), ipAddress);
            }

            LoginResp resp = new LoginResp();
            resp.role = u.getRole();
            resp.token = "ADMIN".equals(requiredRole)
                    ? jwtService.issueAdminToken(String.valueOf(u.getId()))
                    : jwtService.issueToken(String.valueOf(u.getId()), u.getRole());

            AuthDtos.UserInfo userInfo = new AuthDtos.UserInfo();
            userInfo.id = u.getId();
            userInfo.username = u.getUsername();
            userInfo.email = u.getEmail();
            userInfo.displayName = u.getDisplayName();
            userInfo.points = u.getPoints();
            userInfo.role = u.getRole();

            UserProfile profile = userProfileMapper.findByUserId(u.getId());
            userInfo.isVerified = profile != null && profile.getIsVerified() != null && profile.getIsVerified() == 1;

            resp.user = userInfo;
            return resp;
        } catch (IllegalArgumentException ex) {
            if ("ADMIN".equals(requiredRole)) {
                recordAdminLoginFailure(account, ipAddress);
            }
            throw ex;
        }
    }

    public void startReset(String email) {
        User u = userMapper.findByEmail(email);
        if (u == null) {
            throw new IllegalArgumentException("邮箱不存在");
        }
        boolean allowed = rateLimiter.tryConsume("rl:reset:" + email, 3, Duration.ofMinutes(1));
        if (!allowed) {
            throw new IllegalArgumentException("请求过于频繁，请稍后再试");
        }
        String code = String.format("%06d", new Random().nextInt(1_000_000));
        String key = "pwdreset:" + email;
        redis.opsForValue().set(key, code, Duration.ofMinutes(10));
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
        if (u == null) {
            throw new IllegalArgumentException("邮箱不存在");
        }
        userMapper.updatePassword(u.getId(), encoder.encode(newPassword));
        redis.delete(key);
    }

    private void enforceAdminLoginProtection(String account, String ipAddress) {
        if (ipAddress != null && !ipAddress.isBlank()) {
            boolean ipAllowed = rateLimiter.tryConsume("rl:admin:login:ip:" + ipAddress,
                    ADMIN_LOGIN_IP_ATTEMPT_LIMIT, ADMIN_LOGIN_WINDOW);
            if (!ipAllowed) {
                throw new IllegalArgumentException("登录尝试过于频繁，请稍后再试");
            }
            if (Boolean.TRUE.equals(redis.hasKey(adminIpLockKey(ipAddress)))) {
                throw new IllegalArgumentException("该 IP 登录失败次数过多，已临时锁定");
            }
        }

        boolean accountAllowed = rateLimiter.tryConsume("rl:admin:login:account:" + account,
                ADMIN_LOGIN_ACCOUNT_ATTEMPT_LIMIT, ADMIN_LOGIN_WINDOW);
        if (!accountAllowed) {
            throw new IllegalArgumentException("该账号登录尝试过于频繁，请稍后再试");
        }

        if (Boolean.TRUE.equals(redis.hasKey(adminAccountLockKey(account)))) {
            throw new IllegalArgumentException("该账号登录失败次数过多，已临时锁定");
        }
    }

    private void recordAdminLoginFailure(String account, String ipAddress) {
        long accountFailures = incrementWithTtl(adminAccountFailureKey(account), ADMIN_LOGIN_WINDOW);
        if (accountFailures >= ADMIN_LOGIN_FAILURE_LOCK_THRESHOLD) {
            redis.opsForValue().set(adminAccountLockKey(account), "1", ADMIN_LOGIN_LOCK);
        }

        if (ipAddress != null && !ipAddress.isBlank()) {
            long ipFailures = incrementWithTtl(adminIpFailureKey(ipAddress), ADMIN_LOGIN_WINDOW);
            if (ipFailures >= ADMIN_LOGIN_FAILURE_LOCK_THRESHOLD) {
                redis.opsForValue().set(adminIpLockKey(ipAddress), "1", ADMIN_LOGIN_LOCK);
            }
        }
    }

    private void clearAdminLoginFailureState(String account, String ipAddress) {
        redis.delete(adminAccountFailureKey(account));
        redis.delete(adminAccountLockKey(account));
        if (ipAddress != null && !ipAddress.isBlank()) {
            redis.delete(adminIpFailureKey(ipAddress));
            redis.delete(adminIpLockKey(ipAddress));
        }
    }

    private long incrementWithTtl(String key, Duration ttl) {
        Long current = redis.opsForValue().increment(key);
        if (current != null && current == 1L) {
            redis.expire(key, ttl);
        }
        return current == null ? 0L : current;
    }

    private String adminAccountFailureKey(String account) {
        return "sec:admin:login:fail:account:" + account;
    }

    private String adminIpFailureKey(String ipAddress) {
        return "sec:admin:login:fail:ip:" + ipAddress;
    }

    private String adminAccountLockKey(String account) {
        return "sec:admin:login:lock:account:" + account;
    }

    private String adminIpLockKey(String ipAddress) {
        return "sec:admin:login:lock:ip:" + ipAddress;
    }
}
