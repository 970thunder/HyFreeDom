package com.domaindns.user.controller;

import com.domaindns.admin.mapper.FeaturedSiteMapper;
import com.domaindns.admin.model.FeaturedSite;
import com.domaindns.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/featured-sites")
public class UserFeaturedSiteController {

    private final FeaturedSiteMapper mapper;

    public UserFeaturedSiteController(FeaturedSiteMapper mapper) {
        this.mapper = mapper;
    }

    @GetMapping
    public ApiResponse<List<FeaturedSite>> list() {
        return ApiResponse.ok(mapper.findAll());
    }
}
