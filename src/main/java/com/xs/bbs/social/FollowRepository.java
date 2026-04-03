package com.xs.bbs.social;

import java.util.List;

public interface FollowRepository {

    void saveIfAbsent(Long followerId, Long targetId);

    boolean exists(Long followerId, Long targetId);

    List<Long> findTargetIdsByFollowerId(Long followerId);

    List<Long> findFollowerIdsByTargetId(Long targetId);
}
