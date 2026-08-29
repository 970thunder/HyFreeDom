package com.domaindns.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Value("${app.name:DomainDNS}")
    private String appName;

    @Value("${app.url:http://localhost:3000}")
    private String appUrl;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    public void sendVerificationCode(String email, String code, String type) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            if (mailFrom != null && !mailFrom.isBlank()) {
                helper.setFrom(mailFrom);
            }
            helper.setTo(email);

            String subject = type.equals("register") ? appName + " 注册验证码" : appName + " 密码重置验证码";
            helper.setSubject(subject);

            // 准备模板数据
            Context context = new Context();
            context.setVariable("appName", appName);
            context.setVariable("appUrl", appUrl);
            context.setVariable("code", code);
            context.setVariable("email", email);
            context.setVariable("type", type);
            context.setVariable("expireMinutes", 10);

            // 使用HTML模板
            String htmlContent = templateEngine.process("verification-code", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            System.out.println("验证码邮件发送成功: " + email);
        } catch (Exception e) {
            System.err.println("验证码邮件发送失败: " + email + ", 错误: " + e.getMessage());
            throw new IllegalStateException("验证码邮件发送失败，请稍后再试", e);
        }
    }

    @Async
    public void sendWelcomeEmail(String email, String username) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            if (mailFrom != null && !mailFrom.isBlank()) {
                helper.setFrom(mailFrom);
            }
            helper.setTo(email);
            helper.setSubject("欢迎使用 " + appName);

            // 准备模板数据
            Context context = new Context();
            context.setVariable("appName", appName);
            context.setVariable("appUrl", appUrl);
            context.setVariable("username", username);
            context.setVariable("email", email);

            // 使用HTML模板
            String htmlContent = templateEngine.process("welcome", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            System.out.println("欢迎邮件发送成功: " + email);
        } catch (MessagingException e) {
            System.err.println("欢迎邮件发送失败: " + email + ", 错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
