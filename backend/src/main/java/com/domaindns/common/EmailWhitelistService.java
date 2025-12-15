package com.domaindns.common;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 邮箱白名单验证服务
 * 用于验证用户注册时使用的邮箱域名是否在白名单中
 */
@Service
public class EmailWhitelistService {

    // 允许的邮箱域名白名单
    private static final List<String> ALLOWED_DOMAINS = Arrays.asList(
            "gmail.com",
            "googlemail.com",
            "outlook.com",
            "icloud.com",
            "qq.com",
            "vip.qq.com",
            "163.com",
            "vip.163.com",
            "yeah.net",
            "sina.com",
            "sina.cn",
            "139.com",
            "189.cn");

    // 允许的教育域名后缀
    private static final List<String> ALLOWED_EDU_SUFFIXES = Arrays.asList(
            ".edu.cn",
            ".edu");

    // 邮箱格式验证正则表达式
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    /**
     * 验证邮箱是否在白名单中
     * 
     * @param email 要验证的邮箱地址
     * @return 验证结果
     */
    public EmailValidationResult validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return new EmailValidationResult(false, "邮箱地址不能为空");
        }

        email = email.trim().toLowerCase();

        // 验证邮箱格式
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return new EmailValidationResult(false, "邮箱格式不正确");
        }

        // 提取域名
        String domain = extractDomain(email);
        if (domain == null) {
            return new EmailValidationResult(false, "无法解析邮箱域名");
        }

        // 检查是否在白名单中
        if (isDomainAllowed(domain)) {
            return new EmailValidationResult(true, "邮箱验证通过");
        }

        return new EmailValidationResult(false, "该邮箱域名不在允许列表中，请使用主流邮箱服务");
    }

    /**
     * 从邮箱地址中提取域名
     * 
     * @param email 邮箱地址
     * @return 域名
     */
    private String extractDomain(String email) {
        int atIndex = email.lastIndexOf('@');
        if (atIndex == -1 || atIndex == email.length() - 1) {
            return null;
        }
        return email.substring(atIndex + 1);
    }

    /**
     * 检查域名是否被允许
     * 
     * @param domain 域名
     * @return 是否允许
     */
    private boolean isDomainAllowed(String domain) {
        // 检查精确匹配
        if (ALLOWED_DOMAINS.contains(domain)) {
            return true;
        }

        // 检查教育域名后缀
        for (String eduSuffix : ALLOWED_EDU_SUFFIXES) {
            if (domain.endsWith(eduSuffix)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取允许的邮箱域名列表（用于前端显示）
     * 
     * @return 允许的域名列表
     */
    public List<String> getAllowedDomains() {
        return ALLOWED_DOMAINS;
    }

    /**
     * 获取允许的教育域名后缀（用于前端显示）
     * 
     * @return 教育域名后缀列表
     */
    public List<String> getAllowedEduSuffixes() {
        return ALLOWED_EDU_SUFFIXES;
    }

    /**
     * 邮箱验证结果类
     */
    public static class EmailValidationResult {
        private final boolean valid;
        private final String message;

        public EmailValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
}
