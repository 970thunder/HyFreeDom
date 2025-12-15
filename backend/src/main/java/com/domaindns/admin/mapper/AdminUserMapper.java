package com.domaindns.admin.mapper;

import com.domaindns.admin.model.AdminUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminUserMapper {
        List<AdminUser> list(@Param("status") Integer status, @Param("role") String role,
                        @Param("isVerified") Boolean isVerified,
                        @Param("offset") Integer offset,
                        @Param("size") Integer size);

        int count(@Param("status") Integer status, @Param("role") String role, @Param("isVerified") Boolean isVerified);

        int adjustPoints(@Param("id") Long id, @Param("delta") Integer delta);

        AdminUser findById(@Param("id") Long id);

        int updateStatus(@Param("id") Long id, @Param("status") Integer status);

        int updateRole(@Param("id") Long id, @Param("role") String role);
}
