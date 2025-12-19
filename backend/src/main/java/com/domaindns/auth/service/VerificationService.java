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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
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
        // 1. Check if already verified
        UserProfile existing = userProfileMapper.findByUserId(userId);
        if (existing != null && existing.getIsVerified() == 1) {
            throw new IllegalArgumentException("已完成实名认证，无需重复认证");
        }

        // Check rate limit (1 attempt per 24 hours)
        String limitKey = "verification:limit:" + userId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(limitKey))) {
            throw new IllegalArgumentException("24小时内只能进行一次实名认证，请稍后再试");
        }

        // Check if ID card is already used by another user
        try {
            String encryptedIdCard = EncryptionUtil.encrypt(req.idCard);
            UserProfile duplicateProfile = userProfileMapper.findByIdCard(encryptedIdCard);

            // If duplicate exists and it's verified (isVerified=1)
            // And it's not the current user (although we checked by userId above, this is a
            // safeguard)
            if (duplicateProfile != null && duplicateProfile.getIsVerified() == 1) {
                if (!duplicateProfile.getUserId().equals(userId)) {
                    throw new IllegalArgumentException("该身份证号已被其他账号认证，请勿重复使用");
                }
            }
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) e;
            }
            throw new RuntimeException("系统错误：预检查失败", e);
        }

        // Record attempt (regardless of outcome, to prevent spam/cost)
        redisTemplate.opsForValue().set(limitKey, "1", 24, TimeUnit.HOURS);

        // 2. Call Aliyun API
        boolean isValid = callAliyunApi(req.realName, req.idCard);
        if (!isValid) {
            throw new IllegalArgumentException("实名认证失败，请检查姓名和身份证号是否匹配");
        }

        // 3. Encrypt and Save
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

            // 4. Award Points (from settings, default 15)
            String rewardStr = settingsService.get("verification_reward_points", "15");
            int rewardPoints = 15;
            try {
                rewardPoints = Integer.parseInt(rewardStr);
            } catch (NumberFormatException e) {
                // ignore, use default
            }

            if (rewardPoints > 0) {
                pointsMapper.adjust(userId, rewardPoints);
                User user = userMapper.findById(userId);
                pointsMapper.insertTxn(userId, rewardPoints, user.getPoints(), "VERIFICATION_REWARD", "实名认证奖励", null);
            }

        } catch (Exception e) {
            throw new RuntimeException("系统错误：加密失败", e);
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

    private String maskName(String name) {
        if (name == null || name.isEmpty())
            return "";
        return name.substring(0, 1) + (name.length() > 1 ? "*" : "");
    }

    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 18)
            return idCard;
        // Hide 7-16 (index 6 to 15)
        return idCard.substring(0, 6) + "**********" + idCard.substring(16);
    }

    private boolean callAliyunApi(String name, String idCard) {
        if (appCode == null || appCode.isBlank()) {
            // Fallback for development if no appCode is configured
            return true;
        }

        try {
            // User provided API details
            String host = "https://sfzsmyxb.market.alicloudapi.com";
            String path = "/get/idcard/checkV3";
            String method = "GET";

            Map<String, String> querys = new java.util.HashMap<String, String>();
            querys.put("name", name);
            querys.put("idcard", idCard);

            StringBuilder sbUrl = new StringBuilder();
            sbUrl.append(host).append(path).append("?");
            for (Map.Entry<String, String> e : querys.entrySet()) {
                sbUrl.append(e.getKey()).append("=").append(URLEncoder.encode(e.getValue(), "UTF-8")).append("&");
            }
            String urlStr = sbUrl.toString();
            if (urlStr.endsWith("&")) {
                urlStr = urlStr.substring(0, urlStr.length() - 1);
            }

            URL url = new URL(urlStr);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod(method);
            httpURLConnection.setRequestProperty("Authorization", "APPCODE " + appCode);

            int httpCode = httpURLConnection.getResponseCode();
            if (httpCode == 200) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(httpURLConnection.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                // Parse response
                // Based on user snippet, we should check the body.
                // Assuming success response structure based on standard Aliyun Market APIs or
                // simple string check if specific structure is unknown.
                // However, the user snippet provided System.out.println(response.toString()).
                // Usually these APIs return JSON. Let's parse it.
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> map = mapper.readValue(result.toString(), Map.class);

                // Response format provided by user:
                // {
                // "msg": "成功",
                // "success": true,
                // "code": 200,
                // "data": {
                // "result": 1, // 0:一致 1:不一致 2:无记录
                // "order_no": "...",
                // "desc": "不一致",
                // ...
                // }
                // }

                // Check success flag first
                Boolean success = (Boolean) map.get("success");
                if (success == null || !success) {
                    return false;
                }

                // Check data result
                Map<String, Object> data = (Map<String, Object>) map.get("data");
                if (data != null) {
                    Object res = data.get("result");
                    // 0 means consistent (success)
                    // The result might be Integer or String depending on JSON parsing
                    if (res != null) {
                        String resStr = res.toString();
                        if ("0".equals(resStr)) {
                            return true;
                        }
                    }
                }

                return false;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
