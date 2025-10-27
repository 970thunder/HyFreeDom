package com.domaindns.cf.dto;

import java.time.LocalDateTime;

public class DnsRecordDtos {
    public static class DnsRecordWithUser {
        private Long id;
        private Long zoneId;
        private String cfRecordId;
        private String name;
        private String type;
        private String content;
        private Integer ttl;
        private Integer proxied;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String username; // 创建该DNS记录的用户名

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getZoneId() {
            return zoneId;
        }

        public void setZoneId(Long zoneId) {
            this.zoneId = zoneId;
        }

        public String getCfRecordId() {
            return cfRecordId;
        }

        public void setCfRecordId(String cfRecordId) {
            this.cfRecordId = cfRecordId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Integer getTtl() {
            return ttl;
        }

        public void setTtl(Integer ttl) {
            this.ttl = ttl;
        }

        public Integer getProxied() {
            return proxied;
        }

        public void setProxied(Integer proxied) {
            this.proxied = proxied;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }
}
