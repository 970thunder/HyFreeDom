package com.domaindns.admin.controller;

import com.domaindns.admin.mapper.FeaturedSiteMapper;
import com.domaindns.admin.model.FeaturedSite;
import com.domaindns.auth.service.JwtService;
import com.domaindns.common.ApiResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/featured-sites")
public class AdminFeaturedSiteController {

    private final FeaturedSiteMapper mapper;
    private final JwtService jwtService;

    public AdminFeaturedSiteController(FeaturedSiteMapper mapper, JwtService jwtService) {
        this.mapper = mapper;
        this.jwtService = jwtService;
    }

    private void validateAdmin(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new RuntimeException("Unauthorized: Invalid token format");
        }
        String token = authorization.substring(7);
        Jws<Claims> claims = jwtService.parse(token);
        String role = claims.getBody().get("role", String.class);
        if (!"ADMIN".equals(role)) {
            throw new RuntimeException("Unauthorized: Admin access required");
        }
    }

    @GetMapping
    public ApiResponse<List<FeaturedSite>> list(@RequestHeader("Authorization") String authorization) {
        validateAdmin(authorization);
        List<FeaturedSite> sites = mapper.findAll();
        if (sites == null) {
            sites = new java.util.ArrayList<>();
        }
        return ApiResponse.ok(sites);
    }

    @PostMapping
    public ApiResponse<FeaturedSite> create(@RequestHeader("Authorization") String authorization,
            @RequestBody FeaturedSite site) {
        validateAdmin(authorization);
        mapper.insert(site);
        return ApiResponse.ok(site);
    }

    @PutMapping("/{id}")
    public ApiResponse<FeaturedSite> update(@RequestHeader("Authorization") String authorization, @PathVariable Long id,
            @RequestBody FeaturedSite site) {
        validateAdmin(authorization);
        site.setId(id);
        mapper.update(site);
        return ApiResponse.ok(site);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@RequestHeader("Authorization") String authorization, @PathVariable Long id) {
        validateAdmin(authorization);
        mapper.deleteById(id);
        return ApiResponse.ok(null);
    }
}
