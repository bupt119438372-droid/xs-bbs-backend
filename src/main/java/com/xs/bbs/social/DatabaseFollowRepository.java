package com.xs.bbs.social;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class DatabaseFollowRepository implements FollowRepository {

    private final FollowRelationMapper followRelationMapper;

    public DatabaseFollowRepository(FollowRelationMapper followRelationMapper) {
        this.followRelationMapper = followRelationMapper;
    }

    @Override
    public void saveIfAbsent(Long followerId, Long targetId) {
        if (exists(followerId, targetId)) {
            return;
        }
        FollowRelationEntity entity = new FollowRelationEntity();
        entity.setFollowerId(followerId);
        entity.setTargetId(targetId);
        entity.setCreatedAt(LocalDateTime.now());
        followRelationMapper.insert(entity);
    }

    @Override
    public boolean exists(Long followerId, Long targetId) {
        Long count = followRelationMapper.selectCount(
                Wrappers.lambdaQuery(FollowRelationEntity.class)
                        .eq(FollowRelationEntity::getFollowerId, followerId)
                        .eq(FollowRelationEntity::getTargetId, targetId)
        );
        return count != null && count > 0;
    }

    @Override
    public List<Long> findTargetIdsByFollowerId(Long followerId) {
        return followRelationMapper.selectList(
                        Wrappers.lambdaQuery(FollowRelationEntity.class)
                                .eq(FollowRelationEntity::getFollowerId, followerId)
                                .orderByDesc(FollowRelationEntity::getCreatedAt)
                ).stream()
                .map(FollowRelationEntity::getTargetId)
                .toList();
    }

    @Override
    public List<Long> findFollowerIdsByTargetId(Long targetId) {
        return followRelationMapper.selectList(
                        Wrappers.lambdaQuery(FollowRelationEntity.class)
                                .eq(FollowRelationEntity::getTargetId, targetId)
                                .orderByDesc(FollowRelationEntity::getCreatedAt)
                ).stream()
                .map(FollowRelationEntity::getFollowerId)
                .toList();
    }
}
