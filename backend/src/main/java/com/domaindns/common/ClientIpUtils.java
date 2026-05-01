package com.domaindns.common;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIpUtils {
    private ClientIpUtils() {
    }

    public static String resolveClientIp(HttpServletRequest request) {
        String[] headerNames = {
                "CF-Connecting-IP",
                "X-Real-IP",
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP"
        };

        for (String headerName : headerNames) {
            String value = request.getHeader(headerName);
            if (value == null || value.isBlank() || "unknown".equalsIgnoreCase(value)) {
                continue;
            }
            if (value.contains(",")) {
                return value.split(",")[0].trim();
            }
            return value.trim();
        }

        return request.getRemoteAddr();
    }
}
