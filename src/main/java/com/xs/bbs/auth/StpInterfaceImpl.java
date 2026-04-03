package com.xs.bbs.auth;

import cn.dev33.satoken.stp.StpInterface;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class StpInterfaceImpl implements StpInterface {

    private final UserAccountMapper userAccountMapper;

    public StpInterfaceImpl(UserAccountMapper userAccountMapper) {
        this.userAccountMapper = userAccountMapper;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        UserAccountEntity entity = userAccountMapper.selectOne(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.lambdaQuery(UserAccountEntity.class)
                        .eq(UserAccountEntity::getUserId, Long.parseLong(String.valueOf(loginId)))
                        .last("limit 1")
        );
        List<String> permissions = new ArrayList<>();
        permissions.add("thought:publish");
        permissions.add("match:view");
        permissions.add("social:follow");
        permissions.add("insight:view");
        if (isAdmin(entity)) {
            permissions.add("audit:review");
        }
        return permissions;
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        UserAccountEntity entity = userAccountMapper.selectOne(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.lambdaQuery(UserAccountEntity.class)
                        .eq(UserAccountEntity::getUserId, Long.parseLong(String.valueOf(loginId)))
                        .last("limit 1")
        );
        List<String> roles = new ArrayList<>();
        roles.add("user");
        if (isAdmin(entity)) {
            roles.add("admin");
        }
        return roles;
    }

    private boolean isAdmin(UserAccountEntity entity) {
        if (entity == null || entity.getUsername() == null) {
            return false;
        }
        return "admin".equalsIgnoreCase(entity.getUsername())
                || "linxi".equalsIgnoreCase(entity.getUsername());
    }
}
