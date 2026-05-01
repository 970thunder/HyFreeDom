package com.domaindns.user.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PointsMapper {
    int adjust(@Param("userId") Long userId, @Param("delta") Integer delta);

    int insertTxn(@Param("userId") Long userId, @Param("change") Integer change,
            @Param("balanceAfter") Integer balanceAfter, @Param("type") String type,
            @Param("remark") String remark,
            @Param("relatedId") Long relatedId);

    java.util.List<com.domaindns.user.model.PointsTransaction> list(@Param("userId") Long userId,
            @Param("offset") Integer offset, @Param("size") Integer size);

    int count(@Param("userId") Long userId);

    Long getTotalPoints();

    int getActiveUsersCount();

    Long getDailyPointsChange();

    Long getWeeklyPointsChange();

    Double getAverageUserPoints();

    java.util.List<com.domaindns.user.dto.InviteRewardDto> getInviteRewards(@Param("userId") Long userId,
            @Param("offset") Integer offset, @Param("size") Integer size);

    int countInviteRewards(@Param("userId") Long userId);

    java.util.List<java.util.Map<String, Object>> listAll(@Param("keyword") String keyword,
            @Param("type") String type,
            @Param("offset") Integer offset,
            @Param("size") Integer size);

    int countAll(@Param("keyword") String keyword, @Param("type") String type);

    int countByUserTypeRelatedId(@Param("userId") Long userId, @Param("type") String type,
            @Param("relatedId") Long relatedId);

    int countUserCardRedemption(@Param("userId") Long userId, @Param("cardId") Long cardId);
}
