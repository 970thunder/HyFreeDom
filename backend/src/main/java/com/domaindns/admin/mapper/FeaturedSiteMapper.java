package com.domaindns.admin.mapper;

import com.domaindns.admin.model.FeaturedSite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FeaturedSiteMapper {
    List<FeaturedSite> findAll();

    int insert(FeaturedSite site);

    int update(FeaturedSite site);

    int deleteById(Long id);

    FeaturedSite findById(Long id);
}
