package com.domaindns.auth.mapper;

import com.domaindns.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    User findByUsername(@Param("username") String username);

    User findByEmail(@Param("email") String email);

    User findById(@Param("id") Long id);

    int insert(User user);

    int countByRole(@Param("role") String role);

    int updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    int updateInviteCode(@Param("id") Long id, @Param("inviteCode") String inviteCode);

    int updateInviterId(@Param("id") Long id, @Param("inviterId") Long inviterId);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int updatePoints(@Param("id") Long id, @Param("points") Integer points);

    int updateIp(@Param("id") Long id, @Param("ipAddress") String ipAddress);

    /**
     * 统计指定时间范围内的用户注册数量
     */
    int countByDateRange(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 按日期统计用户注册数量（用于图表）
     */
    java.util.List<java.util.Map<String, Object>> countByDateGroup(@Param("startDate") String startDate,
            @Param("endDate") String endDate);
}
