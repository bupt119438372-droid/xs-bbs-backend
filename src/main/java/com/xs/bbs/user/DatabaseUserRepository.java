package com.xs.bbs.user;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public class DatabaseUserRepository implements UserRepository {

    private final UserProfileMapper userProfileMapper;

    public DatabaseUserRepository(UserProfileMapper userProfileMapper) {
        this.userProfileMapper = userProfileMapper;
    }

    @Override
    public Optional<UserProfile> findById(Long userId) {
        return Optional.ofNullable(userProfileMapper.selectById(userId))
                .map(this::toDomain);
    }

    @Override
    public Collection<UserProfile> findAll() {
        return userProfileMapper.selectList(
                        Wrappers.lambdaQuery(UserProfileEntity.class)
                                .orderByAsc(UserProfileEntity::getId)
                ).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public UserProfile save(String nickname, String bio) {
        UserProfileEntity entity = new UserProfileEntity();
        entity.setNickname(nickname.trim());
        entity.setBio(bio.trim());
        userProfileMapper.insert(entity);
        return toDomain(entity);
    }

    private UserProfile toDomain(UserProfileEntity entity) {
        return new UserProfile(entity.getId(), entity.getNickname(), entity.getBio());
    }
}
