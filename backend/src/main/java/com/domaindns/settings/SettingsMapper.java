package com.domaindns.settings;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SettingsMapper {
    List<SettingsRow> findAll();

    String getValue(String k);

    int upsert(SettingsRow row);
}
