package com.domaindns.user.service;

import com.domaindns.cf.mapper.ZoneMapper;
import com.domaindns.cf.model.Zone;
import com.domaindns.cf.service.DnsRecordService;
import com.domaindns.cf.mapper.DnsRecordMapper;
import com.domaindns.auth.mapper.UserMapper;
import com.domaindns.auth.entity.User;
import com.domaindns.user.mapper.PointsMapper;
import com.domaindns.user.mapper.UserDomainMapper;
import com.domaindns.settings.SettingsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;

@Service
public class UserDomainService {
    private final ZoneMapper zoneMapper;
    private final PointsMapper pointsMapper;
    private final UserDomainMapper userDomainMapper;
    private final SettingsService settingsService;
    private final UserMapper userMapper;
    private final DnsRecordService dnsRecordService;
    private final DnsRecordMapper dnsRecordMapper;

    public UserDomainService(ZoneMapper zoneMapper, PointsMapper pointsMapper, UserDomainMapper userDomainMapper,
            SettingsService settingsService, UserMapper userMapper, DnsRecordService dnsRecordService,
            DnsRecordMapper dnsRecordMapper) {
        this.zoneMapper = zoneMapper;
        this.pointsMapper = pointsMapper;
        this.userDomainMapper = userDomainMapper;
        this.settingsService = settingsService;
        this.userMapper = userMapper;
        this.dnsRecordService = dnsRecordService;
        this.dnsRecordMapper = dnsRecordMapper;
    }

    @Transactional
    public void applySubdomain(Long userId, Object zoneIdOrKey, String prefix, String type, String value, Integer ttl,
            String remark) {
        Zone z = resolveZone(zoneIdOrKey);
        if (z == null || z.getEnabled() == null || z.getEnabled() == 0)
            throw new IllegalArgumentException("zone 不可用");

        int baseCost = getBaseCost();
        double multiplier = tldMultiplier(z.getName());
        int cost = (int) Math.ceil(baseCost * multiplier);

        User u = userMapper.findById(userId);
        if (u == null)
            throw new IllegalArgumentException("用户不存在");
        if (u.getPoints() == null || u.getPoints() < cost)
            throw new IllegalStateException("积分不足");

        // 先在 Cloudflare 创建 DNS 记录，成功后再扣积分和落库
        int ttlToUse = ttl != null ? ttl : getDefaultTtl();
        validateRecord(type, value);
        String fullDomain = prefix + "." + z.getName();

        // 前置重复校验：同用户是否已申请过该域名
        if (userDomainMapper.countByUserAndDomain(userId, fullDomain) > 0)
            throw new IllegalArgumentException("你已申请过该子域名");
        // Cloudflare 侧是否已有记录（本地镜像）
        // 如果是 TXT 或 NS 记录，不校验重复（因为不同服务商的验证记录名可能相同）
        String typeUpper = type.toUpperCase(Locale.ROOT);
        if (!"TXT".equals(typeUpper) && !"NS".equals(typeUpper)) {
            if (dnsRecordMapper.countByZoneAndName(z.getId(), fullDomain) > 0)
                throw new IllegalArgumentException("该子域名已被占用");
        }

        Long localDnsRecordId;
        try {
            // 特殊处理 NS 记录：如果值包含空格，拆分为多个 NS 记录
            if ("NS".equals(typeUpper) && value != null && value.contains(" ")) {
                // 拆分多个 NS 记录值
                String[] nsValues = value.trim().split("\\s+");
                if (nsValues.length == 0) {
                    throw new IllegalArgumentException("NS 记录值不能为空");
                }

                // 创建第一个 NS 记录（作为主记录）
                String firstNsValue = normalizeNsValue(nsValues[0]);
                String bodyJson = buildCfRecordJson(fullDomain, type, firstNsValue, ttlToUse);
                dnsRecordService.create(z.getId(), bodyJson);
                localDnsRecordId = fetchLocalDnsRecordId(z.getId(), fullDomain);

                // 创建其他 NS 记录（使用相同的名称，Cloudflare 会合并）
                for (int i = 1; i < nsValues.length; i++) {
                    String nsValue = normalizeNsValue(nsValues[i]);
                    if (!nsValue.isEmpty()) {
                        String additionalBodyJson = buildCfRecordJson(fullDomain, type, nsValue, ttlToUse);
                        try {
                            dnsRecordService.create(z.getId(), additionalBodyJson);
                        } catch (Exception e) {
                            // 如果创建额外的 NS 记录失败，记录日志但不影响主流程
                            System.err.println("创建额外 NS 记录失败: " + e.getMessage());
                        }
                    }
                }
            } else {
                // 普通记录类型，直接创建
                String normalizedValue = "NS".equals(typeUpper) ? normalizeNsValue(value) : value;
                String bodyJson = buildCfRecordJson(fullDomain, type, normalizedValue, ttlToUse);
                dnsRecordService.create(z.getId(), bodyJson);
                localDnsRecordId = fetchLocalDnsRecordId(z.getId(), fullDomain);
            }
        } catch (Exception e) {
            throw new IllegalStateException("创建 DNS 记录失败: " + e.getMessage());
        }

        userDomainMapper.insert(userId, z.getId(), localDnsRecordId, prefix, fullDomain, remark);

        // 扣积分并记录流水
        pointsMapper.adjust(userId, -cost);
        User updatedUser = userMapper.findById(userId);
        pointsMapper.insertTxn(userId, -cost, updatedUser != null ? updatedUser.getPoints() : null, "DOMAIN_APPLY",
                "申请域名 " + fullDomain + " 扣除 " + cost + " 积分", null);
    }

    public Map<String, Object> listUserDomains(Long userId, Integer page, Integer size) {
        int offset = (Math.max(page, 1) - 1) * Math.max(size, 1);
        java.util.List<com.domaindns.user.model.UserDomain> list = userDomainMapper.listByUser(userId, offset, size);
        int total = userDomainMapper.countByUser(userId);
        java.util.HashMap<String, Object> m = new java.util.HashMap<>();
        m.put("list", list);
        m.put("total", total);
        m.put("page", page);
        m.put("size", size);
        return m;
    }

    @Transactional
    public void updateDomainRecord(Long userId, Long id, String type, String value, Integer ttl, String remark) {
        com.domaindns.user.model.UserDomain ud = userDomainMapper.findByIdAndUser(id, userId);
        if (ud == null)
            throw new IllegalArgumentException("记录不存在");

        // 验证记录类型和值
        validateRecord(type, value);

        Zone z = zoneMapper.findById(ud.getZoneId());
        if (z == null) {
            throw new IllegalArgumentException("域名区域不存在");
        }

        int ttlToUse = ttl != null ? ttl : getDefaultTtl();
        String typeUpper = type.toUpperCase(Locale.ROOT);

        // 获取所有现有记录
        java.util.List<com.domaindns.cf.model.DnsRecord> existingRecords = dnsRecordMapper
                .findAllByZoneAndName(z.getId(), ud.getFullDomain());

        // 策略：如果是 NS 记录或者现有记录多于1条，则采取"全删全建"策略
        // 否则（普通记录且只有1条），采取"更新"策略

        boolean isNsUpdate = "NS".equals(typeUpper);
        boolean hasMultipleExisting = existingRecords != null && existingRecords.size() > 1;

        if (isNsUpdate || hasMultipleExisting) {
            // 1. 删除所有现有记录
            // 先断开外键关联，防止删除记录时报错
            userDomainMapper.updateDnsRecordId(ud.getId(), null);

            if (existingRecords != null) {
                for (com.domaindns.cf.model.DnsRecord r : existingRecords) {
                    try {
                        dnsRecordService.delete(z.getId(), r.getCfRecordId());
                    } catch (Exception ignored) {
                    }
                    dnsRecordMapper.deleteByZoneAndCfRecordId(z.getId(), r.getCfRecordId());
                }
            }

            // 2. 创建新记录
            Long newMainRecordId = null;

            if (isNsUpdate && value.contains(" ")) {
                // NS 记录拆分
                String[] nsValues = value.trim().split("\\s+");
                for (int i = 0; i < nsValues.length; i++) {
                    String nsVal = normalizeNsValue(nsValues[i]);
                    if (!nsVal.isEmpty()) {
                        String bodyJson = buildCfRecordJson(ud.getFullDomain(), type, nsVal, ttlToUse);
                        try {
                            dnsRecordService.create(z.getId(), bodyJson);
                            // 如果是第一个记录，获取ID用于更新 UserDomain
                            if (newMainRecordId == null) {
                                newMainRecordId = fetchLocalDnsRecordId(z.getId(), ud.getFullDomain());
                            }
                        } catch (Exception e) {
                            throw new IllegalStateException("创建 NS 记录失败: " + e.getMessage());
                        }
                    }
                }
            } else {
                // 单条记录创建 (A, CNAME, etc. or Single NS)
                String val = isNsUpdate ? normalizeNsValue(value) : value;
                String bodyJson = buildCfRecordJson(ud.getFullDomain(), type, val, ttlToUse);
                try {
                    dnsRecordService.create(z.getId(), bodyJson);
                    newMainRecordId = fetchLocalDnsRecordId(z.getId(), ud.getFullDomain());
                } catch (Exception e) {
                    throw new IllegalStateException("创建记录失败: " + e.getMessage());
                }
            }

            // 3. 更新 UserDomain 指向新的主记录
            if (newMainRecordId != null) {
                userDomainMapper.updateDnsRecordId(ud.getId(), newMainRecordId);
            }

        } else {
            // 现有记录0或1条，且非NS拆分情况 -> 尝试更新
            com.domaindns.cf.model.DnsRecord dnsRecord = null;
            if (existingRecords != null && !existingRecords.isEmpty()) {
                dnsRecord = existingRecords.get(0);
            }

            if (dnsRecord != null) {
                // 更新Cloudflare记录
                String bodyJson = buildCfRecordJson(ud.getFullDomain(), type, value, ttlToUse);
                try {
                    dnsRecordService.update(dnsRecord.getZoneId(), dnsRecord.getCfRecordId(), bodyJson);
                } catch (Exception e) {
                    throw new IllegalStateException("更新DNS记录失败: " + e.getMessage());
                }
            } else {
                // 理论上不应该发生（记录不存在但UserDomain存在），但作为兜底创建
                String bodyJson = buildCfRecordJson(ud.getFullDomain(), type, value, ttlToUse);
                try {
                    dnsRecordService.create(z.getId(), bodyJson);
                    Long newId = fetchLocalDnsRecordId(z.getId(), ud.getFullDomain());
                    userDomainMapper.updateDnsRecordId(ud.getId(), newId);
                } catch (Exception e) {
                    throw new IllegalStateException("创建DNS记录失败: " + e.getMessage());
                }
            }
        }

        // 更新本地记录信息 (remark)
        userDomainMapper.updateRecordInfo(id, type, value, ttl, remark);
    }

    @Transactional
    public void releaseDomain(Long userId, Long id) {
        com.domaindns.user.model.UserDomain ud = userDomainMapper.findByIdAndUser(id, userId);
        if (ud == null)
            throw new IllegalArgumentException("记录不存在");

        Zone zForDelete = zoneMapper.findById(ud.getZoneId());
        if (zForDelete != null) {
            // 查找所有相关的DNS记录（特别是针对专属域名NS记录，可能有多条）
            java.util.List<com.domaindns.cf.model.DnsRecord> records = dnsRecordMapper
                    .findAllByZoneAndName(zForDelete.getId(), ud.getFullDomain());

            // 先断开外键，避免删除 dns_records 时违反约束
            userDomainMapper.updateDnsRecordId(ud.getId(), null);

            if (records != null && !records.isEmpty()) {
                for (com.domaindns.cf.model.DnsRecord r : records) {
                    try {
                        dnsRecordService.delete(zForDelete.getId(), r.getCfRecordId());
                    } catch (Exception ignored) {
                    }
                    dnsRecordMapper.deleteByZoneAndCfRecordId(zForDelete.getId(), r.getCfRecordId());
                }
            } else {
                // 如果没有找到记录，尝试按名称兜底清理
                dnsRecordMapper.deleteByZoneAndName(zForDelete.getId(), ud.getFullDomain());
            }
        }

        // 删除 user_domain
        userDomainMapper.deleteByIdAndUser(id, userId);

        // 返还 50% 创建时消耗的积分（按当前规则重算成本的一半）
        Zone z = zoneMapper.findById(ud.getZoneId());
        int baseCost = getBaseCost();
        double multiplier = z != null ? tldMultiplier(z.getName()) : 1.0;
        int cost = (int) Math.ceil(baseCost * multiplier);
        int refund = Math.max(1, cost / 2);
        pointsMapper.adjust(userId, refund);
        User updatedUser = userMapper.findById(userId);
        pointsMapper.insertTxn(userId, refund, updatedUser != null ? updatedUser.getPoints() : null, "DOMAIN_RELEASE",
                "释放域名 " + ud.getFullDomain() + " 返还 " + refund + " 积分", id);
    }

    private Zone resolveZone(Object zoneIdOrKey) {
        if (zoneIdOrKey == null)
            return null;
        try {
            Long id = Long.valueOf(zoneIdOrKey.toString());
            return zoneMapper.findById(id);
        } catch (NumberFormatException ignore) {
        }
        String s = zoneIdOrKey.toString();
        Zone z = zoneMapper.findByCfZoneId(s);
        if (z != null)
            return z;
        return zoneMapper.findByName(s);
    }

    private int getBaseCost() {
        Map<String, String> all = settingsService.getAll();
        String v = all.getOrDefault("domain_cost_points", "10");
        try {
            return Integer.parseInt(v);
        } catch (Exception e) {
            return 10;
        }
    }

    private double tldMultiplier(String zoneName) {
        if (zoneName == null)
            return 1.0;
        String lower = zoneName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".cn") || lower.endsWith(".com"))
            return 2.0;
        if (lower.endsWith(".top"))
            return 1.5;
        return 1.0;
    }

    private Long fetchLocalDnsRecordId(Long zoneId, String fullDomain) {
        com.domaindns.cf.model.DnsRecord r = dnsRecordMapper.findOneByZoneAndName(zoneId, fullDomain);
        return r == null ? null : r.getId();
    }

    private String buildCfRecordJson(String name, String type, String value, Integer ttl) {
        StringBuilder sb = new StringBuilder();
        sb.append('{')
                .append("\"type\":\"").append(type).append("\",")
                .append("\"name\":\"").append(name).append("\",")
                .append("\"content\":\"").append(value).append("\",")
                .append("\"ttl\":").append(ttl).append(",")
                .append("\"proxied\":false");
        sb.append('}');
        return sb.toString();
    }

    private int getDefaultTtl() {
        Map<String, String> all = settingsService.getAll();
        String v = all.getOrDefault("default_ttl", "120");
        try {
            return Integer.parseInt(v);
        } catch (Exception e) {
            return 120;
        }
    }

    private void validateRecord(String type, String value) {
        if (type == null || type.isEmpty())
            throw new IllegalArgumentException("记录类型不能为空");
        String t = type.toUpperCase(Locale.ROOT);
        if (t.equals("A")) {
            if (!value.matches("^(?:\\d{1,3}\\.){3}\\d{1,3}$"))
                throw new IllegalArgumentException("A 记录需要 IPv4 地址");
        } else if (t.equals("AAAA")) {
            if (!value.contains(":"))
                throw new IllegalArgumentException("AAAA 记录需要 IPv6 地址");
        } else if (t.equals("NS")) {
            // NS 记录验证：值必须是有效的域名格式
            if (value == null || value.trim().isEmpty())
                throw new IllegalArgumentException("NS 记录值不能为空");
            // 如果包含空格，验证每个部分
            String[] parts = value.trim().split("\\s+");
            for (String part : parts) {
                String normalized = normalizeNsValue(part);
                if (normalized.isEmpty() || !isValidDomain(normalized)) {
                    throw new IllegalArgumentException("NS 记录值格式无效: " + part);
                }
            }
        } else if (t.equals("CNAME") || t.equals("TXT")) {
            // 基础校验略过，可按需增强
        }
    }

    /**
     * 规范化 NS 记录值：确保是完整的域名格式
     * Cloudflare 要求 NS 记录的值必须是完整的域名（FQDN）
     */
    private String normalizeNsValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        String normalized = value.trim();
        // 如果域名不以点结尾，添加点使其成为 FQDN
        // 但 Cloudflare 实际上不需要点结尾，所以这里只做基本清理
        return normalized;
    }

    /**
     * 验证是否为有效的域名格式
     */
    private boolean isValidDomain(String domain) {
        if (domain == null || domain.isEmpty()) {
            return false;
        }
        // 基本的域名格式验证：允许字母、数字、点、连字符
        // 不能以点或连字符开头或结尾（除非是根域名）
        return domain.matches("^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$") ||
                domain.matches("^[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?$");
    }
}
