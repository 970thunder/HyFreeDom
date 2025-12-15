package com.domaindns.auth.mapper;

import com.domaindns.auth.entity.UserProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserProfileMapper {
    UserProfile findByUserId(Long userId);

    UserProfile findByIdCard(@Param("idCard") String idCard);

    int insert(UserProfile profile);

    int update(UserProfile profile);
}
