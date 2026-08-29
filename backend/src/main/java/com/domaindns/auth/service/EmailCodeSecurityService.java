package com.domaindns.auth.service;

import com.domaindns.admin.mapper.AuditMapper;
import com.domaindns.admin.model.AuditLog;
import com.domaindns.common.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;

@Service
public class EmailCodeSecurityService {
    private static final Logger log = LoggerFactory.getLogger(EmailCodeSecurityService.class);

    private final RateLimiter rateLimiter;
    private final AuditMapper auditMapper;

    @Value("${app.security.email-code.cooldown-seconds:60}")
    private int cooldownSeconds;

    @Value("${app.security.email-code.recipient-day-limit:3}")
    private int recipientDayLimit;

    @Value("${app.security.email-code.ip-hour-limit:5}")
    private int ipHourLimit;

    @Value("${app.security.email-code.ip-day-limit:5}")
    private int ipDayLimit;

    public EmailCodeSecurityService(RateLimiter rateLimiter, AuditMapper auditMapper) {
        this.rateLimiter = rateLimiter;
        this.auditMapper = auditMapper;
    }

    public String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    public void enforceSendLimit(String type, String email, String ip, String userAgent) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedIp = normalizeIp(ip);

        if (!rateLimiter.tryConsume("rl:email-code:email:" + normalizedEmail + ":cooldown", 1,
                Duration.ofSeconds(Math.max(1, cooldownSeconds)))) {
            reject(type, normalizedEmail, normalizedIp, userAgent, "recipient_cooldown");
        }
        if (!rateLimiter.tryConsume("rl:email-code:email:" + normalizedEmail + ":day", Math.max(1, recipientDayLimit),
                Duration.ofDays(1))) {
            reject(type, normalizedEmail, normalizedIp, userAgent, "recipient_daily_limit");
        }
        if (!rateLimiter.tryConsume("rl:email-code:ip:" + normalizedIp + ":hour", Math.max(1, ipHourLimit),
                Duration.ofHours(1))) {
            reject(type, normalizedEmail, normalizedIp, userAgent, "ip_hourly_limit");
        }
        if (!rateLimiter.tryConsume("rl:email-code:ip:" + normalizedIp + ":day", Math.max(1, ipDayLimit),
                Duration.ofDays(1))) {
            reject(type, normalizedEmail, normalizedIp, userAgent, "ip_daily_limit");
        }
    }

    public void recordSent(String type, String email, String ip, String userAgent) {
        record(type, "SENT", email, ip, userAgent, "mail_accepted_by_provider");
    }

    public void recordFailure(String type, String email, String ip, String userAgent, String reason) {
        record(type, "FAILED", email, ip, userAgent, reason);
    }

    public void recordRejected(String type, String email, String ip, String userAgent, String reason) {
        record(type, "REJECTED", email, ip, userAgent, reason);
    }

    private void reject(String type, String email, String ip, String userAgent, String reason) {
        record(type, "RATE_LIMITED", email, ip, userAgent, reason);
        throw new IllegalArgumentException("验证码发送过于频繁，请稍后再试");
    }

    private void record(String type, String outcome, String email, String ip, String userAgent, String reason) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAction("EMAIL_CODE_" + type.toUpperCase(Locale.ROOT) + "_" + outcome);
        auditLog.setTarget(normalizeEmail(email));
        auditLog.setIp(normalizeIp(ip));
        auditLog.setDetail("reason=" + sanitize(reason, 120) + "; userAgent=" + sanitize(userAgent, 240));
        try {
            auditMapper.insert(auditLog);
        } catch (Exception ex) {
            // Logging must not make the public authentication endpoint unavailable.
            log.error("Failed to persist email code audit event", ex);
        }
    }

    private String normalizeIp(String ip) {
        return ip == null || ip.isBlank() ? "unknown" : ip.trim();
    }

    private String sanitize(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String sanitized = value.replaceAll("[\\r\\n\\t]", " ").trim();
        return sanitized.length() <= maxLength ? sanitized : sanitized.substring(0, maxLength);
    }
}
