package com.domaindns.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthDtos {
    public static class RegisterReq {
        @NotBlank(message = "用户名必填")
        public String username;
        @Email(message = "邮箱格式不正确")
        public String email;
        @NotBlank(message = "密码必填")
        public String password;
        public String inviteCode;
        @NotBlank(message = "邮箱验证码必填")
        public String emailCode;
    }

    public static class RegisterResp {
        public Long userId;
    }

    public static class AdminRegisterReq {
        @NotBlank(message = "用户名必填")
        public String username;
        @Email(message = "邮箱格式不正确")
        public String email;
        @NotBlank(message = "密码必填")
        public String password;
    }

    public static class LoginReq {
        @NotBlank(message = "用户名或邮箱必填")
        public String username; // 可以接受用户名或邮箱
        @NotBlank(message = "密码必填")
        public String password;
    }

    public static class LoginResp {
        public String token;
        public String role;
        public UserInfo user;
    }

    public static class UserInfo {
        public Long id;
        public String username;
        public String email;
        public String displayName;
        public Integer points;
        public String role;
        public Boolean isVerified;
    }

    public static class ForgotPasswordReq {
        @Email(message = "邮箱格式不正确")
        @NotBlank(message = "邮箱必填")
        public String email;
    }

    public static class ResetPasswordReq {
        @Email(message = "邮箱格式不正确")
        @NotBlank(message = "邮箱必填")
        public String email;
        @NotBlank(message = "验证码必填")
        public String code;
        @NotBlank(message = "新密码必填")
        public String newPassword;
    }

    public static class SendRegisterCodeReq {
        @Email(message = "邮箱格式不正确")
        @NotBlank(message = "邮箱必填")
        public String email;
    }

    public static class VerificationReq {
        @NotBlank(message = "真实姓名必填")
        public String realName;
        @NotBlank(message = "身份证号必填")
        public String idCard;
    }

    public static class VerificationStatusResp {
        public boolean isVerified;
        public String realName; // Masked
        public String idCard; // Masked
        public String verifiedAt;
    }
}
