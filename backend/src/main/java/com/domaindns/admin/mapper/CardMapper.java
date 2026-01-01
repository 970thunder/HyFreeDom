package com.domaindns.admin.mapper;

import com.domaindns.admin.model.Card;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CardMapper {
    List<Card> list(@Param("status") String status, @Param("offset") Integer offset, @Param("size") Integer size);

    int count(@Param("status") String status);

    int insert(@Param("code") String code, @Param("points") Integer points,
            @Param("expiredAt") LocalDateTime expiredAt);

    int batchDelete(@Param("ids") List<Long> ids);

    int deleteById(@Param("id") Long id);

    Card findByCode(@Param("code") String code);

    int markAsUsed(@Param("id") Long id, @Param("usedBy") Long usedBy, @Param("usedAt") LocalDateTime usedAt);
}
