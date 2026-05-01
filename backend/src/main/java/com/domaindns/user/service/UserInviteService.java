package com.domaindns.user.service;

import com.domaindns.admin.mapper.InviteMapper;
import com.domaindns.admin.model.InviteCode;
import com.domaindns.auth.entity.User;
import com.domaindns.auth.mapper.UserMapper;
import com.domaindns.auth.service.VerificationService;
import com.domaindns.settings.SettingsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserInviteService {
    private final InviteMapper inviteMapper;
    private final UserMapper userMapper;
    private final SettingsService settingsService;
    private final VerificationService verificationService;
    private final SecureRandom random = new SecureRandom();

    public UserInviteService(InviteMapper inviteMapper, UserMapper userMapper,
            SettingsService settingsService, VerificationService verificationService) {
        this.inviteMapper = inviteMapper;
        this.userMapper = userMapper;
        this.settingsService = settingsService;
        this.verificationService = verificationService;
    }

    @Transactional
    public Map<String, Object> generateOrResetInviteCode(Long userId, Integer maxUses, Integer validDays) {
        InviteCode existing = inviteMapper.findByOwnerUserId(userId);
        String code = generateInviteCode();
        LocalDateTime expiredAt = validDays != null ? LocalDateTime.now().plusDays(validDays) : null;

        if (existing != null) {
            inviteMapper.updateByOwnerUserId(userId, code, maxUses, expiredAt);
        } else {
            inviteMapper.insert(code, userId, maxUses, expiredAt);
        }

        userMapper.updateInviteCode(userId, code);

        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("maxUses", maxUses);
        result.put("validDays", validDays);
        result.put("expiredAt", expiredAt);
        return result;
    }

    @Transactional
    public Map<String, Object> bindInviteCode(Long userId, String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("邀请码不能为空");
        }
        User me = userMapper.findById(userId);
        if (me == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (me.getInviterId() != null) {
            throw new IllegalArgumentException("已绑定过邀请人，无法重复绑定");
        }

        InviteCode invite = inviteMapper.findByCode(code);
        if (invite == null || !"ACTIVE".equals(invite.getStatus())) {
            throw new IllegalArgumentException("邀请码无效或已失效");
        }
        if (invite.getExpiredAt() != null && invite.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("邀请码已过期");
        }
        if (invite.getMaxUses() != null && invite.getMaxUses() > 0 && invite.getUsedCount() >= invite.getMaxUses()) {
            throw new IllegalArgumentException("邀请码使用次数已达上限");
        }

        Long inviterId = invite.getOwnerUserId();
        if (inviterId.equals(userId)) {
            throw new IllegalArgumentException("不能绑定自己的邀请码");
        }

        userMapper.updateInviterId(userId, inviterId);
        inviteMapper.incrementUsedCount(code);
        verificationService.processPendingInviteRewards(userId);

        User inviter = userMapper.findById(inviterId);
        Map<String, Object> result = new HashMap<>();
        result.put("inviterId", inviterId);
        result.put("inviterUsername", inviter != null ? inviter.getUsername() : null);
        result.put("inviteePoints", Integer.parseInt(settingsService.get("invitee_points", "3")));
        result.put("inviterPoints", Integer.parseInt(settingsService.get("inviter_points", "3")));
        result.put("rewardIssued", verificationService.isVerified(userId) && verificationService.isVerified(inviterId));
        result.put("message", "邀请关系已绑定，双方完成实名认证后发放奖励积分");
        return result;
    }

    public Map<String, Object> getMyInviter(Long userId) {
        User me = userMapper.findById(userId);
        Map<String, Object> result = new HashMap<>();
        if (me == null || me.getInviterId() == null) {
            result.put("hasInviter", false);
            return result;
        }
        User inviter = userMapper.findById(me.getInviterId());
        result.put("hasInviter", true);
        result.put("inviterId", me.getInviterId());
        result.put("inviterUsername", inviter != null ? inviter.getUsername() : null);
        return result;
    }

    public Map<String, Object> getMyInviteCode(Long userId) {
        InviteCode invite = inviteMapper.findByOwnerUserId(userId);
        if (invite == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("hasCode", false);
            return result;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("hasCode", true);
        result.put("code", invite.getCode());
        result.put("maxUses", invite.getMaxUses());
        result.put("usedCount", invite.getUsedCount());
        result.put("status", invite.getStatus());
        result.put("createdAt", invite.getCreatedAt());
        result.put("expiredAt", invite.getExpiredAt());
        if (invite.getMaxUses() != null && invite.getMaxUses() > 0) {
            result.put("remainingUses", invite.getMaxUses() - invite.getUsedCount());
        } else {
            result.put("remainingUses", "unlimited");
        }
        return result;
    }

    private String generateInviteCode() {
        StringBuilder sb = new StringBuilder();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
