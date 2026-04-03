package com.xs.bbs.auth;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xs.bbs.user.UserProfile;
import com.xs.bbs.user.UserService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuthService {

    private final UserAccountMapper userAccountMapper;
    private final UserService userService;
    private final StpInterface stpInterface;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserAccountMapper userAccountMapper, UserService userService, StpInterface stpInterface) {
        this.userAccountMapper = userAccountMapper;
        this.userService = userService;
        this.stpInterface = stpInterface;
    }

    @Transactional
    public LoginResult register(RegisterRequest request) {
        if (findAccountByUsername(request.username()) != null) {
            throw new IllegalArgumentException("username already exists");
        }
        UserProfile profile = userService.createUser(
                request.nickname(),
                request.bio() == null || request.bio().isBlank() ? "这个人还没有留下简介。" : request.bio().trim()
        );
        UserAccountEntity entity = new UserAccountEntity();
        entity.setUserId(profile.id());
        entity.setUsername(request.username().trim());
        entity.setPasswordHash(passwordEncoder.encode(request.password()));
        entity.setCreatedAt(LocalDateTime.now());
        userAccountMapper.insert(entity);
        return loginByUser(profile.id(), entity.getUsername(), profile);
    }

    public LoginResult login(LoginRequest request) {
        UserAccountEntity entity = findAccountByUsername(request.username());
        if (entity == null || !passwordEncoder.matches(request.password(), entity.getPasswordHash())) {
            throw new IllegalArgumentException("username or password is incorrect");
        }
        UserProfile profile = userService.getRequiredUser(entity.getUserId());
        return loginByUser(profile.id(), entity.getUsername(), profile);
    }

    public SessionUserView currentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        UserProfile profile = userService.getRequiredUser(userId);
        UserAccountEntity entity = findAccountByUserId(userId);
        if (entity == null) {
            throw new IllegalArgumentException("account does not exist");
        }
        return toSessionUser(entity, profile);
    }

    public void logout() {
        StpUtil.logout();
    }

    public UserAccountEntity findAccountByUsername(String username) {
        return userAccountMapper.selectOne(
                Wrappers.lambdaQuery(UserAccountEntity.class)
                        .eq(UserAccountEntity::getUsername, username.trim())
                        .last("limit 1")
        );
    }

    public UserAccountEntity findAccountByUserId(Long userId) {
        return userAccountMapper.selectOne(
                Wrappers.lambdaQuery(UserAccountEntity.class)
                        .eq(UserAccountEntity::getUserId, userId)
                        .last("limit 1")
        );
    }

    private LoginResult loginByUser(Long userId, String username, UserProfile profile) {
        StpUtil.login(userId);
        return new LoginResult(
                StpUtil.getTokenName(),
                StpUtil.getTokenValue(),
                StpUtil.getTokenTimeout(),
                toSessionUser(username, profile)
        );
    }

    private SessionUserView toSessionUser(UserAccountEntity entity, UserProfile profile) {
        return toSessionUser(entity.getUsername(), profile);
    }

    private SessionUserView toSessionUser(String username, UserProfile profile) {
        List<String> roles = stpInterface.getRoleList(profile.id(), "login");
        List<String> permissions = stpInterface.getPermissionList(profile.id(), "login");
        return new SessionUserView(profile.id(), username, profile, roles, permissions);
    }
}
