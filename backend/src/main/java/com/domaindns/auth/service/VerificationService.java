package com.domaindns.auth.service;

import com.domaindns.auth.dto.AuthDtos.VerificationReq;
import com.domaindns.auth.dto.AuthDtos.VerificationStatusResp;
import com.domaindns.auth.entity.User;
import com.domaindns.auth.entity.UserProfile;
import com.domaindns.auth.mapper.UserMapper;
import com.domaindns.auth.mapper.UserProfileMapper;
import com.domaindns.common.EncryptionUtil;
import com.domaindns.settings.SettingsService;
import com.domaindns.user.mapper.PointsMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class VerificationService {

    private final UserProfileMapper userProfileMapper;
    private final UserMapper userMapper;
    private final PointsMapper pointsMapper;
    private final SettingsService settingsService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${aliyun.api.appcode:}")
    private String appCode;

    public VerificationService(UserProfileMapper userProfileMapper, UserMapper userMapper, PointsMapper pointsMapper,
            SettingsService settingsService) {
        this.userProfileMapper = userProfileMapper;
        this.userMapper = userMapper;
        this.pointsMapper = pointsMapper;
        this.settingsService = settingsService;
    }

    @Transactional
    public void verify(Long userId, VerificationReq req) {
        UserProfile existing = userProfileMapper.findByUserId(userId);
        if (existing != null && existing.getIsVerified() == 1) {
            throw new IllegalArgumentException("已完成实名认证，无需重复认证");
        }

        String limitKey = "verification:limit:" + userId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(limitKey))) {
            throw new IllegalArgumentException("24 小时内只能进行一次实名认证，请稍后再试");
        }

        try {
            String encryptedIdCard = EncryptionUtil.encrypt(req.idCard);
            UserProfile duplicateProfile = userProfileMapper.findByIdCard(encryptedIdCard);
            if (duplicateProfile != null && duplicateProfile.getIsVerified() == 1
                    && !duplicateProfile.getUserId().equals(userId)) {
                throw new IllegalArgumentException("该身份证号已被其他账号认证，请勿重复使用");
            }
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) e;
            }
            throw new RuntimeException("系统错误：预检查失败", e);
        }

        redisTemplate.opsForValue().set(limitKey, "1", 24, TimeUnit.HOURS);

        boolean isValid = callAliyunApi(req.realName, req.idCard);
        if (!isValid) {
            throw new IllegalArgumentException("实名认证失败，请检查姓名和身份证号是否匹配");
        }

        try {
            String encName = EncryptionUtil.encrypt(req.realName);
            String encId = EncryptionUtil.encrypt(req.idCard);

            if (existing == null) {
                UserProfile profile = new UserProfile();
                profile.setUserId(userId);
                profile.setRealName(encName);
                profile.setIdCard(encId);
                profile.setIsVerified(1);
                profile.setVerifiedAt(LocalDateTime.now());
                userProfileMapper.insert(profile);
            } else {
                existing.setRealName(encName);
                existing.setIdCard(encId);
                existing.setIsVerified(1);
                existing.setVerifiedAt(LocalDateTime.now());
                userProfileMapper.update(existing);
            }

            int rewardPoints = parseIntSetting("verification_reward_points", 15);
            if (rewardPoints > 0) {
                pointsMapper.adjust(userId, rewardPoints);
                User user = userMapper.findById(userId);
                pointsMapper.insertTxn(userId, rewardPoints, user.getPoints(), "VERIFICATION_REWARD", "实名认证奖励", null);
            }

            processPendingInviteRewards(userId);
        } catch (Exception e) {
            throw new RuntimeException("系统错误：实名认证保存失败", e);
        }
    }

    @Transactional
    public void processPendingInviteRewards(Long userId) {
        User currentUser = userMapper.findById(userId);
        if (currentUser == null || !isVerified(userId)) {
            return;
        }

        Long inviterId = currentUser.getInviterId();
        if (inviterId != null) {
            issueInviteRewardIfEligible(currentUser, inviterId);
        }

        List<User> invitees = userMapper.findByInviterId(userId);
        for (User invitee : invitees) {
            issueInviteRewardIfEligible(invitee, userId);
        }
    }

    public VerificationStatusResp getStatus(Long userId) {
        UserProfile profile = userProfileMapper.findByUserId(userId);
        VerificationStatusResp resp = new VerificationStatusResp();
        if (profile != null && profile.getIsVerified() != null && profile.getIsVerified() == 1) {
            resp.isVerified = true;
            try {
                String realName = EncryptionUtil.decrypt(profile.getRealName());
                String idCard = EncryptionUtil.decrypt(profile.getIdCard());
                resp.realName = maskName(realName);
                resp.idCard = maskIdCard(idCard);
                resp.verifiedAt = profile.getVerifiedAt().toString();
            } catch (Exception e) {
                resp.realName = "Error";
                resp.idCard = "Error";
            }
        } else {
            resp.isVerified = false;
        }
        return resp;
    }

    public boolean isVerified(Long userId) {
        UserProfile profile = userProfileMapper.findByUserId(userId);
        return profile != null && profile.getIsVerified() != null && profile.getIsVerified() == 1;
    }

    private void issueInviteRewardIfEligible(User invitee, Long inviterId) {
        if (invitee == null || invitee.getId() == null || inviterId == null) {
            return;
        }
        if (!isVerified(invitee.getId()) || !isVerified(inviterId)) {
            return;
        }
        if (pointsMapper.countByUserTypeRelatedId(invitee.getId(), "INVITE_CODE", inviterId) > 0) {
            return;
        }
        if (pointsMapper.countByUserTypeRelatedId(inviterId, "INVITE_REWARD", invitee.getId()) > 0) {
            return;
        }

        int inviteePoints = parseIntSetting("invitee_points", 3);
        int inviterPoints = parseIntSetting("inviter_points", 3);

        if (inviteePoints > 0) {
            pointsMapper.adjust(invitee.getId(), inviteePoints);
            User updatedInvitee = userMapper.findById(invitee.getId());
            pointsMapper.insertTxn(invitee.getId(), inviteePoints,
                    updatedInvitee != null ? updatedInvitee.getPoints() : null,
                    "INVITE_CODE", "邀请实名通过后发放奖励积分", inviterId);
        }

        if (inviterPoints > 0) {
            pointsMapper.adjust(inviterId, inviterPoints);
            User updatedInviter = userMapper.findById(inviterId);
            pointsMapper.insertTxn(inviterId, inviterPoints,
                    updatedInviter != null ? updatedInviter.getPoints() : null,
                    "INVITE_REWARD", "邀请用户 " + invitee.getUsername() + " 实名后获得奖励积分", invitee.getId());
        }
    }

    private int parseIntSetting(String key, int defaultValue) {
        String value = settingsService.get(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        return name.substring(0, 1) + (name.length() > 1 ? "*" : "");
    }

    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 18) {
            return idCard;
        }
        return idCard.substring(0, 6) + "**********" + idCard.substring(16);
    }

    private boolean callAliyunApi(String name, String idCard) {
        if (appCode == null || appCode.isBlank()) {
            return true;
        }

        String url = "https://eid.shumaidata.com/eid/checkbody";

        try {
            OkHttpClient client = new OkHttpClient.Builder().build();
            FormBody body = new FormBody.Builder()
                    .add("idcard", idCard)
                    .add("name", name)
                    .build();
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "APPCODE " + appCode)
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println("实名认证 API 请求失败: " + response.code() + " " + response.message());
                    return false;
                }

                String resultStr = response.body().string();
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> map = mapper.readValue(resultStr, Map.class);
                Object codeObj = map.get("code");
                String code = codeObj != null ? codeObj.toString() : "";
                if (!"0".equals(code)) {
                    return false;
                }

                Map<String, Object> result = (Map<String, Object>) map.get("result");
                if (result == null) {
                    return false;
                }
                Object resObj = result.get("res");
                String res = resObj != null ? resObj.toString() : "";
                return "1".equals(res);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
