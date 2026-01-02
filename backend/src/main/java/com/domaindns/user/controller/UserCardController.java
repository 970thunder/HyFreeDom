package com.domaindns.user.controller;

import com.domaindns.admin.mapper.CardMapper;
import com.domaindns.admin.model.Card;
import com.domaindns.auth.entity.User;
import com.domaindns.auth.mapper.UserMapper;
import com.domaindns.auth.service.JwtService;
import com.domaindns.common.ApiResponse;
import com.domaindns.user.mapper.PointsMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user/card")
public class UserCardController {
    private final CardMapper cardMapper;
    private final UserMapper userMapper;
    private final PointsMapper pointsMapper;
    private final JwtService jwtService;

    public UserCardController(CardMapper cardMapper, UserMapper userMapper,
            PointsMapper pointsMapper, JwtService jwtService) {
        this.cardMapper = cardMapper;
        this.userMapper = userMapper;
        this.pointsMapper = pointsMapper;
        this.jwtService = jwtService;
    }

    /**
     * 卡密兑换积分
     */
    @PostMapping("/redeem")
    @Transactional
    public ApiResponse<Map<String, Object>> redeemCard(@RequestHeader("Authorization") String authorization,
            @RequestBody Map<String, Object> body) {
        long userId = getCurrentUserId(authorization);
        String cardCode = String.valueOf(body.get("cardCode"));

        if (cardCode == null || cardCode.trim().isEmpty()) {
            throw new IllegalArgumentException("卡密不能为空");
        }

        cardCode = cardCode.trim().toUpperCase();

        // 查找卡密
        Card card = cardMapper.findByCode(cardCode);
        if (card == null) {
            throw new IllegalArgumentException("卡密不存在");
        }

        // 检查卡密状态
        if ("USED".equals(card.getStatus())) {
            throw new IllegalArgumentException("卡密已被使用完毕");
        }
        if (!"ACTIVE".equals(card.getStatus())) {
            throw new IllegalArgumentException("卡密无效或已过期");
        }

        // 检查卡密是否过期
        if (card.getExpiredAt() != null && card.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("卡密已过期");
        }

        // 检查卡密使用次数限制
        if (card.getUsageLimit() != null && card.getUsedCount() >= card.getUsageLimit()) {
            throw new IllegalArgumentException("卡密已被使用完毕");
        }

        // 检查用户是否已经使用过该卡密
        if (pointsMapper.countUserCardRedemption(userId, card.getId()) > 0) {
            throw new IllegalArgumentException("您已经兑换过该卡密，不能重复兑换");
        }

        // 获取用户信息
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        // 更新卡密状态
        int updated = cardMapper.markAsUsed(card.getId(), userId, LocalDateTime.now());
        if (updated == 0) {
            throw new IllegalArgumentException("卡密兑换失败，可能已被抢先兑换完毕");
        }

        // 更新用户积分
        int currentPoints = user.getPoints() == null ? 0 : user.getPoints();
        int newPoints = currentPoints + card.getPoints();
        userMapper.updatePoints(userId, newPoints);

        // 记录积分流水
        pointsMapper.insertTxn(userId, card.getPoints(), newPoints, "CARD_REDEEM",
                "卡密兑换积分", card.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("points", card.getPoints());
        result.put("newBalance", newPoints);
        result.put("message", "卡密兑换成功！获得 " + card.getPoints() + " 积分");

        return ApiResponse.ok(result);
    }

    private long getCurrentUserId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new RuntimeException("未登录");
        }
        String token = authorization.substring(7);
        try {
            Jws<Claims> jws = jwtService.parse(token);
            return Long.parseLong(jws.getBody().getSubject());
        } catch (Exception e) {
            throw new RuntimeException("Token无效或已过期");
        }
    }
}
