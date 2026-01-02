package com.domaindns.user.service;

import com.domaindns.admin.mapper.InviteMapper;
import com.domaindns.admin.model.InviteCode;
import com.domaindns.auth.entity.User;
import com.domaindns.auth.mapper.UserMapper;
import com.domaindns.user.mapper.PointsMapper;
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
    private final PointsMapper pointsMapper;
    private final SettingsService settingsService;
    private final SecureRandom random = new SecureRandom();

    public UserInviteService(InviteMapper inviteMapper, UserMapper userMapper,
            PointsMapper pointsMapper, SettingsService settingsService) {
        this.inviteMapper = inviteMapper;
        this.userMapper = userMapper;
        this.pointsMapper = pointsMapper;
        this.settingsService = settingsService;
    }

    @Transactional
    public Map<String, Object> generateOrResetInviteCode(Long userId, Integer maxUses, Integer validDays) {
        // 检查是否已有邀请码
        InviteCode existing = inviteMapper.findByOwnerUserId(userId);

        String code = generateInviteCode();
        LocalDateTime expiredAt = validDays != null ? LocalDateTime.now().plusDays(validDays) : null;

        if (existing != null) {
            // 重置现有邀请码
            inviteMapper.updateByOwnerUserId(userId, code, maxUses, expiredAt);
        } else {
            // 创建新邀请码
            inviteMapper.insert(code, userId, maxUses, expiredAt);
        }

        // 更新用户表中的 invite_code 字段
        userMapper.updateInviteCode(userId, code);

        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("maxUses", maxUses);
        result.put("validDays", validDays);
        result.put("expiredAt", expiredAt);
        return result;
    }

    /**
     * 补填邀请码：为尚未绑定邀请人的用户绑定邀请关系并发放积分。
     */
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
        if (invite.getExpiredAt() != null && invite.getExpiredAt().isBefore(java.time.LocalDateTime.now())) {
            throw new IllegalArgumentException("邀请码已过期");
        }
        if (invite.getMaxUses() != null && invite.getMaxUses() > 0 && invite.getUsedCount() >= invite.getMaxUses()) {
            throw new IllegalArgumentException("邀请码使用次数已达上限");
        }

        Long inviterId = invite.getOwnerUserId();
        if (inviterId.equals(userId)) {
            throw new IllegalArgumentException("不能绑定自己的邀请码");
        }

        // 读取配置积分
        java.util.Map<String, String> settings = settingsService.getAll();
        int inviteePoints = Integer.parseInt(settings.getOrDefault("invitee_points", "3"));
        int inviterPoints = Integer.parseInt(settings.getOrDefault("inviter_points", "3"));

        // 绑定邀请关系
        userMapper.updateInviterId(userId, inviterId);

        // 发放积分（被邀请者 + 邀请者）
        pointsMapper.adjust(userId, inviteePoints);
        User updatedMe = userMapper.findById(userId);
        pointsMapper.insertTxn(userId, inviteePoints, updatedMe != null ? updatedMe.getPoints() : null, "INVITE_CODE",
                "补填邀请码奖励积分", inviterId);

        pointsMapper.adjust(inviterId, inviterPoints);
        User updatedInviter = userMapper.findById(inviterId);
        pointsMapper.insertTxn(inviterId, inviterPoints, updatedInviter != null ? updatedInviter.getPoints() : null, "INVITE_REWARD",
                "邀请用户 " + me.getUsername() + " 获得奖励积分", userId);

        // 更新邀请码使用次数
        inviteMapper.incrementUsedCount(code);

        User inviter = userMapper.findById(inviterId);
        Map<String, Object> result = new HashMap<>();
        result.put("inviterId", inviterId);
        result.put("inviterUsername", inviter != null ? inviter.getUsername() : null);
        result.put("inviteePoints", inviteePoints);
        result.put("inviterPoints", inviterPoints);
        return result;
    }

    /**
     * 查询当前用户的邀请人信息
     */
    public Map<String, Object> getMyInviter(Long userId) {
        User me = userMapper.findById(userId);
        Map<String, Object> m = new HashMap<>();
        if (me == null || me.getInviterId() == null) {
            m.put("hasInviter", false);
            return m;
        }
        User inviter = userMapper.findById(me.getInviterId());
        m.put("hasInviter", true);
        m.put("inviterId", me.getInviterId());
        m.put("inviterUsername", inviter != null ? inviter.getUsername() : null);
        return m;
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

        // 计算剩余使用次数
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
