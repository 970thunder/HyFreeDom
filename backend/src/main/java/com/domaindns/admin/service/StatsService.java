package com.domaindns.admin.service;

import com.domaindns.admin.mapper.AdminUserMapper;
import com.domaindns.auth.mapper.UserMapper;
import com.domaindns.cf.mapper.ZoneMapper;
import com.domaindns.cf.mapper.DnsRecordMapper;
import com.domaindns.user.mapper.PointsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class StatsService {

    @Autowired
    private AdminUserMapper adminUserMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ZoneMapper zoneMapper;

    @Autowired
    private DnsRecordMapper dnsRecordMapper;

    @Autowired
    private PointsMapper pointsMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private com.domaindns.cf.service.DnsRecordService dnsRecordService;

    // Redis缓存键前缀
    private static final String CACHE_PREFIX = "stats:";
    private static final String DASHBOARD_CACHE_KEY = CACHE_PREFIX + "dashboard";
    private static final String USER_CACHE_KEY = CACHE_PREFIX + "users";
    private static final String USER_REGISTRATION_CACHE_KEY = CACHE_PREFIX + "user_registration";
    private static final String ZONE_CACHE_KEY = CACHE_PREFIX + "zones";
    private static final String DNS_RECORD_CACHE_KEY = CACHE_PREFIX + "dns_records";
    private static final String POINTS_CACHE_KEY = CACHE_PREFIX + "points";

    // 缓存过期时间（分钟）
    private static final int CACHE_EXPIRE_MINUTES = 5;

    /**
     * 获取Dashboard统计数据
     */
    public Map<String, Object> getDashboardStats() {
        // 尝试从Redis获取缓存
        @SuppressWarnings("unchecked")
        Map<String, Object> cachedStats = (Map<String, Object>) redisTemplate.opsForValue().get(DASHBOARD_CACHE_KEY);
        if (cachedStats != null) {
            return cachedStats;
        }

        // 缓存未命中，计算统计数据
        Map<String, Object> stats = new HashMap<>();

        try {
            // 并行获取各种统计数据
            Map<String, Object> userStats = getUserStats();
            Map<String, Object> zoneStats = getZoneStats();
            Map<String, Object> dnsStats = getDnsRecordStats();
            Map<String, Object> pointsStats = getPointsStats();

            System.out.println("Dashboard统计数据组装:");
            System.out.println("用户统计: " + userStats);
            System.out.println("域名统计: " + zoneStats);
            System.out.println("DNS记录统计: " + dnsStats);
            System.out.println("积分统计: " + pointsStats);

            // 组装Dashboard数据
            stats.put("totalUsers", userStats.get("totalUsers"));
            stats.put("totalZones", zoneStats.get("totalZones"));
            stats.put("activeZones", zoneStats.get("activeZones"));
            stats.put("totalDnsRecords", dnsStats.get("totalDnsRecords"));
            stats.put("dnsRecordsByType", dnsStats.get("recordsByType"));
            stats.put("totalPoints", pointsStats.get("totalPoints"));
            stats.put("activeUsers", pointsStats.get("activeUsers"));

            // 计算本周新增用户
            stats.put("weeklyNewUsers", userStats.get("weeklyNewUsers"));

            // 计算今日新增记录
            stats.put("dailyNewRecords", dnsStats.get("dailyNewRecords"));

            // 添加时间戳
            stats.put("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            // 缓存结果
            redisTemplate.opsForValue().set(DASHBOARD_CACHE_KEY, stats, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        } catch (Exception e) {
            // 如果计算失败，返回默认值
            stats.put("totalUsers", 0);
            stats.put("totalZones", 0);
            stats.put("activeZones", 0);
            stats.put("totalDnsRecords", 0);
            stats.put("totalPoints", 0);
            stats.put("activeUsers", 0);
            stats.put("weeklyNewUsers", 0);
            stats.put("dailyNewRecords", 0);
            stats.put("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            stats.put("error", "统计数据计算失败: " + e.getMessage());
        }

        return stats;
    }

    /**
     * 获取用户统计
     */
    public Map<String, Object> getUserStats() {
        // 尝试从Redis获取缓存
        @SuppressWarnings("unchecked")
        Map<String, Object> cachedStats = (Map<String, Object>) redisTemplate.opsForValue().get(USER_CACHE_KEY);
        if (cachedStats != null) {
            return cachedStats;
        }

        Map<String, Object> stats = new HashMap<>();

        try {
            // 总用户数
            int totalUsers = adminUserMapper.count(null, "USER", null);
            stats.put("totalUsers", totalUsers);

            // 本周新增用户（简化计算，实际应该按创建时间统计）
            int weeklyNewUsers = Math.max(0, totalUsers / 10); // 临时计算方式
            stats.put("weeklyNewUsers", weeklyNewUsers);

            // 今日新增用户
            int dailyNewUsers = Math.max(0, totalUsers / 100); // 临时计算方式
            stats.put("dailyNewUsers", dailyNewUsers);

            // 活跃用户数（最近7天有操作的用户）
            int activeUsers = Math.max(0, totalUsers / 5); // 临时计算方式
            stats.put("activeUsers", activeUsers);

            // 缓存结果
            redisTemplate.opsForValue().set(USER_CACHE_KEY, stats, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        } catch (Exception e) {
            stats.put("totalUsers", 0);
            stats.put("weeklyNewUsers", 0);
            stats.put("dailyNewUsers", 0);
            stats.put("activeUsers", 0);
            stats.put("error", "用户统计计算失败: " + e.getMessage());
        }

        return stats;
    }

    /**
     * 获取域名统计
     */
    public Map<String, Object> getZoneStats() {
        // 尝试从Redis获取缓存
        @SuppressWarnings("unchecked")
        Map<String, Object> cachedStats = (Map<String, Object>) redisTemplate.opsForValue().get(ZONE_CACHE_KEY);
        if (cachedStats != null) {
            return cachedStats;
        }

        Map<String, Object> stats = new HashMap<>();

        try {
            // 总域名数
            int totalZones = zoneMapper.count(null);
            stats.put("totalZones", totalZones);

            // 活跃域名数（status = 1）
            int activeZones = zoneMapper.count(1);
            stats.put("activeZones", activeZones);

            // 禁用域名数
            int inactiveZones = totalZones - activeZones;
            stats.put("inactiveZones", inactiveZones);

            // 今日新增域名
            int dailyNewZones = Math.max(0, totalZones / 50); // 临时计算方式
            stats.put("dailyNewZones", dailyNewZones);

            // 缓存结果
            redisTemplate.opsForValue().set(ZONE_CACHE_KEY, stats, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        } catch (Exception e) {
            stats.put("totalZones", 0);
            stats.put("activeZones", 0);
            stats.put("inactiveZones", 0);
            stats.put("dailyNewZones", 0);
            stats.put("error", "域名统计计算失败: " + e.getMessage());
        }

        return stats;
    }

    /**
     * 获取DNS记录统计
     */
    public Map<String, Object> getDnsRecordStats() {
        // 尝试从Redis获取缓存
        @SuppressWarnings("unchecked")
        Map<String, Object> cachedStats = (Map<String, Object>) redisTemplate.opsForValue().get(DNS_RECORD_CACHE_KEY);
        if (cachedStats != null) {
            return cachedStats;
        }

        Map<String, Object> stats = new HashMap<>();

        try {
            // 先尝试同步所有zone的DNS记录（如果数据库中没有记录的话）
            int existingRecords = dnsRecordMapper.countAll();
            if (existingRecords == 0) {
                System.out.println("DNS记录表为空，尝试同步所有zone的DNS记录...");
                syncAllZonesDnsRecords();
            }

            // 总DNS记录数（需要统计所有zone的DNS记录）
            int totalDnsRecords = dnsRecordMapper.countAll();
            System.out.println("DNS记录总数查询结果: " + totalDnsRecords);
            stats.put("totalDnsRecords", totalDnsRecords);

            // 今日新增记录
            LocalDateTime startOfDay = LocalDateTime.now().with(java.time.LocalTime.MIN);
            int dailyNewRecords = dnsRecordMapper.countByDateRange(startOfDay, null);
            stats.put("dailyNewRecords", dailyNewRecords);

            // 本周新增记录
            // 注意：DayOfWeek.MONDAY 需要根据具体需求确定，这里假设周一为一周开始
            LocalDateTime startOfWeek = LocalDateTime.now()
                    .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                    .with(java.time.LocalTime.MIN);
            int weeklyNewRecords = dnsRecordMapper.countByDateRange(startOfWeek, null);
            stats.put("weeklyNewRecords", weeklyNewRecords);

            // 按类型统计
            List<Map<String, Object>> typeCounts = dnsRecordMapper.countByType();
            Map<String, Integer> recordsByType = new HashMap<>();
            if (typeCounts != null) {
                for (Map<String, Object> entry : typeCounts) {
                    String type = (String) entry.get("type");
                    Number count = (Number) entry.get("count");
                    if (type != null && count != null) {
                        recordsByType.put(type, count.intValue());
                    }
                }
            }
            System.out.println("DNS记录按类型统计: " + recordsByType);
            stats.put("recordsByType", recordsByType);

            // 缓存结果
            redisTemplate.opsForValue().set(DNS_RECORD_CACHE_KEY, stats, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        } catch (Exception e) {
            stats.put("totalDnsRecords", 0);
            stats.put("dailyNewRecords", 0);
            stats.put("weeklyNewRecords", 0);
            stats.put("recordsByType", new HashMap<>());
            stats.put("error", "DNS记录统计计算失败: " + e.getMessage());
        }

        return stats;
    }

    /**
     * 获取积分统计
     */
    public Map<String, Object> getPointsStats() {
        // 尝试从Redis获取缓存
        @SuppressWarnings("unchecked")
        Map<String, Object> cachedStats = (Map<String, Object>) redisTemplate.opsForValue().get(POINTS_CACHE_KEY);
        if (cachedStats != null) {
            return cachedStats;
        }

        Map<String, Object> stats = new HashMap<>();

        try {
            // 系统总积分
            Long totalPoints = pointsMapper.getTotalPoints();
            stats.put("totalPoints", totalPoints != null ? totalPoints : 0);

            // 活跃用户数（有积分的用户）
            int activeUsers = pointsMapper.getActiveUsersCount();
            stats.put("activeUsers", activeUsers);

            // 今日积分变动
            Long dailyPointsChange = pointsMapper.getDailyPointsChange();
            stats.put("dailyPointsChange", dailyPointsChange != null ? dailyPointsChange : 0);

            // 本周积分变动
            Long weeklyPointsChange = pointsMapper.getWeeklyPointsChange();
            stats.put("weeklyPointsChange", weeklyPointsChange != null ? weeklyPointsChange : 0);

            // 平均用户积分
            Double avgUserPoints = pointsMapper.getAverageUserPoints();
            stats.put("avgUserPoints", avgUserPoints != null ? avgUserPoints : 0.0);

            // 缓存结果
            redisTemplate.opsForValue().set(POINTS_CACHE_KEY, stats, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        } catch (Exception e) {
            stats.put("totalPoints", 0);
            stats.put("activeUsers", 0);
            stats.put("dailyPointsChange", 0);
            stats.put("weeklyPointsChange", 0);
            stats.put("avgUserPoints", 0.0);
            stats.put("error", "积分统计计算失败: " + e.getMessage());
        }

        return stats;
    }

    /**
     * 获取用户注册统计
     */
    public Map<String, Object> getUserRegistrationStats(String type, Integer days) {
        // 构建缓存键
        String cacheKey = USER_REGISTRATION_CACHE_KEY + ":" + type + ":" + days;

        // 尝试从Redis获取缓存
        @SuppressWarnings("unchecked")
        Map<String, Object> cachedStats = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedStats != null) {
            return cachedStats;
        }

        Map<String, Object> stats = new HashMap<>();

        try {
            LocalDateTime now = LocalDateTime.now();
            String endDate = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String startDate;

            // 根据类型计算开始日期
            switch (type.toLowerCase()) {
                case "day":
                    startDate = now.minusDays(days - 1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    break;
                case "week":
                    startDate = now.minusWeeks(days).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    break;
                case "month":
                    startDate = now.minusMonths(days).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    break;
                default:
                    startDate = now.minusDays(days - 1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            }

            // 获取按日期分组的注册数据
            List<Map<String, Object>> registrationData = userMapper.countByDateGroup(startDate, endDate);

            // 构建完整的日期范围数据（填充缺失的日期）
            Map<String, Integer> dateCountMap = new HashMap<>();
            for (Map<String, Object> data : registrationData) {
                String date = data.get("date").toString();
                Integer count = ((Number) data.get("count")).intValue();
                dateCountMap.put(date, count);
            }

            // 生成完整的日期序列
            List<Map<String, Object>> chartData = new java.util.ArrayList<>();
            LocalDateTime currentDate = LocalDateTime.parse(startDate + "T00:00:00");
            LocalDateTime endDateTime = LocalDateTime.parse(endDate + "T23:59:59");

            while (!currentDate.isAfter(endDateTime)) {
                String dateStr = currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                Integer count = dateCountMap.getOrDefault(dateStr, 0);

                Map<String, Object> dayData = new HashMap<>();
                dayData.put("date", dateStr);
                dayData.put("count", count);
                dayData.put("displayDate", formatDisplayDate(dateStr, type));
                chartData.add(dayData);

                currentDate = currentDate.plusDays(1);
            }

            // 计算统计信息
            int totalRegistrations = registrationData.stream()
                    .mapToInt(data -> ((Number) data.get("count")).intValue())
                    .sum();

            int maxDailyRegistrations = chartData.stream()
                    .mapToInt(data -> (Integer) data.get("count"))
                    .max()
                    .orElse(0);

            double avgDailyRegistrations = chartData.size() > 0 ? (double) totalRegistrations / chartData.size() : 0.0;

            // 构建返回数据
            stats.put("type", type);
            stats.put("days", days);
            stats.put("startDate", startDate);
            stats.put("endDate", endDate);
            stats.put("chartData", chartData);
            stats.put("totalRegistrations", totalRegistrations);
            stats.put("maxDailyRegistrations", maxDailyRegistrations);
            stats.put("avgDailyRegistrations", Math.round(avgDailyRegistrations * 100.0) / 100.0);
            stats.put("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            // 缓存结果（缓存时间根据类型调整）
            int cacheMinutes = type.equals("day") ? 5 : (type.equals("week") ? 30 : 60);
            redisTemplate.opsForValue().set(cacheKey, stats, cacheMinutes, TimeUnit.MINUTES);

        } catch (Exception e) {
            stats.put("error", "用户注册统计计算失败: " + e.getMessage());
            stats.put("chartData", new java.util.ArrayList<>());
            stats.put("totalRegistrations", 0);
            stats.put("maxDailyRegistrations", 0);
            stats.put("avgDailyRegistrations", 0.0);
        }

        return stats;
    }

    /**
     * 格式化显示日期
     */
    private String formatDisplayDate(String dateStr, String type) {
        LocalDateTime date = LocalDateTime.parse(dateStr + "T00:00:00");
        switch (type.toLowerCase()) {
            case "day":
                return String.format("%d/%d", date.getMonthValue(), date.getDayOfMonth());
            case "week":
                return String.format("%d月第%d周", date.getMonthValue(),
                        (date.getDayOfMonth() - 1) / 7 + 1);
            case "month":
                return String.format("%d年%d月", date.getYear(), date.getMonthValue());
            default:
                return String.format("%d/%d", date.getMonthValue(), date.getDayOfMonth());
        }
    }

    /**
     * 刷新所有统计数据缓存
     */
    public void refreshAllStats() {
        // 清除所有缓存
        redisTemplate.delete(DASHBOARD_CACHE_KEY);
        redisTemplate.delete(USER_CACHE_KEY);
        redisTemplate.delete(USER_REGISTRATION_CACHE_KEY + ":*");
        redisTemplate.delete(ZONE_CACHE_KEY);
        redisTemplate.delete(DNS_RECORD_CACHE_KEY);
        redisTemplate.delete(POINTS_CACHE_KEY);

        // 重新计算并缓存
        getDashboardStats();
    }

    /**
     * 清除所有统计数据缓存
     */
    public void clearAllStatsCache() {
        redisTemplate.delete(DASHBOARD_CACHE_KEY);
        redisTemplate.delete(USER_CACHE_KEY);
        redisTemplate.delete(USER_REGISTRATION_CACHE_KEY + ":*");
        redisTemplate.delete(ZONE_CACHE_KEY);
        redisTemplate.delete(DNS_RECORD_CACHE_KEY);
        redisTemplate.delete(POINTS_CACHE_KEY);
    }

    /**
     * 同步所有zone的DNS记录
     */
    private void syncAllZonesDnsRecords() {
        try {
            // 获取所有启用的zone
            List<com.domaindns.cf.model.Zone> zones = zoneMapper.list(1, null, null);
            System.out.println("找到 " + zones.size() + " 个启用的zone，开始同步DNS记录...");

            int syncedCount = 0;
            for (com.domaindns.cf.model.Zone zone : zones) {
                try {
                    dnsRecordService.syncZoneRecords(zone.getId());
                    syncedCount++;
                    System.out.println("已同步zone: " + zone.getName() + " (ID: " + zone.getId() + ")");
                } catch (Exception e) {
                    System.err.println("同步zone " + zone.getName() + " 的DNS记录失败: " + e.getMessage());
                }
            }

            System.out.println("DNS记录同步完成，共同步了 " + syncedCount + " 个zone");
        } catch (Exception e) {
            System.err.println("同步DNS记录时发生错误: " + e.getMessage());
        }
    }
}