package com.domaindns.admin.mapper;

import com.domaindns.admin.model.AuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AuditMapper {
    List<AuditLog> list(@Param("action") String action, @Param("offset") Integer offset, @Param("size") Integer size);

    int count(@Param("action") String action);

    int insert(AuditLog auditLog);

    List<AuditLog> listEmailCodeLogs(@Param("email") String email, @Param("ip") String ip,
            @Param("offset") Integer offset, @Param("size") Integer size);

    int countEmailCodeLogs(@Param("email") String email, @Param("ip") String ip);
}
