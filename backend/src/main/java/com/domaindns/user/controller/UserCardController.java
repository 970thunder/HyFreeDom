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
        if (!"ACTIVE".equals(card.getStatus())) {
            throw new IllegalArgumentException("卡密已被使用或已失效");
        }

        // 检查卡密是否过期
        if (card.getExpiredAt() != null && card.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("卡密已过期");
        }
        
        // 检查用户是否已兑换过该卡密 (防止重复兑换同一个卡密)
        if (pointsMapper.checkTransactionExists(userId, "CARD_REDEEM", card.getId()) > 0) {
            throw new IllegalArgumentException("您已兑换过该卡密，无法重复使用");
        }

        // 检查卡密使用次数限制
        Integer limit = card.getUsageLimit();
        Integer used = card.getUsedCount() == null ? 0 : card.getUsedCount();
        
        // 如果有次数限制且已达上限 (兼容旧数据: limit为null视为1次, usedCount为null视为0)
        // 注意：旧数据 limit=null, usedCount=0, usedBy=null -> 视为单次卡
        // 如果 limit=null, 且 usedBy != null -> 已使用
        // 新逻辑下，limit=null 表示无限次？
        // 不，之前决定 limit=null 是无限次。
        // 但是旧数据的 limit 都是 null。如果视 limit=null 为无限次，那旧卡密全变成无限次了？
        // 必须区分旧卡密和新无限卡密。
        // 旧卡密：created_at 早于更新时间？或者我们在DB迁移时把旧卡密 limit 设为 1？
        // 假设我们在DB迁移时会将所有现存卡密的 usage_limit 设为 1。
        // 如果没有迁移，我们需要代码兼容。
        // 方案：如果 limit 是 null， check usedBy。如果 usedBy 有值，则已使用。
        // 但新生成的无限卡密 limit 是 null。
        // 所以，新生成的无限卡密必须有一个标志。或者我们规定无限卡密的 limit 是 -1 或 0？
        // 用户输入框为空代表无限制。Controller里我们可以存 -1 代表无限制。
        // 让我们修改 Controller，存 -1 代表无限制。
        // 这样 limit=null 可以保留给旧数据，视为 limit=1。
        
        // 重新思考 CardController 的修改：
        // Integer usageLimit = body.get("usageLimit") ...
        // if (usageLimit == null) usageLimit = 1; (random mode)
        // visible in Controller: "如果未指定使用次数，默认为1次"
        // 对于 Custom Code, explicit null means unlimited?
        // Let's change Controller to store -1 for unlimited.
        
        // Back to UserCardController logic assuming limit=-1 is infinite, limit=null is 1 (legacy).
        int effectiveLimit = (limit == null) ? 1 : limit;
        
        if (effectiveLimit != -1 && used >= effectiveLimit) {
             throw new IllegalArgumentException("卡密已被使用");
        }
        
        // 再次检查 usedBy (针对旧数据或单次卡)
        if (effectiveLimit == 1 && card.getUsedBy() != null) {
             throw new IllegalArgumentException("卡密已被使用");
        }

        // 获取用户信息
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        // 更新卡密状态
        // 增加使用次数
        cardMapper.incrementUsage(card.getId());
        
        // 如果是单次卡(或者限制次数卡且达到限制)，标记为 USED
        // 注意 incrementUsage 是原子操作，但这里我们在事务中，可以后置检查
        // 或者直接 updateStatus
        if (effectiveLimit != -1 && (used + 1) >= effectiveLimit) {
            cardMapper.markAsUsed(card.getId(), userId, LocalDateTime.now());
        } else {
            // 对于多次卡/无限卡，我们不设置 used_by (因为有多个用户)，也不设置 status 为 USED (除非耗尽)
            // 但我们需要记录这次使用吗？ points_transactions 已经记录了。
            // 仅仅 incrementUsage 即可。
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
