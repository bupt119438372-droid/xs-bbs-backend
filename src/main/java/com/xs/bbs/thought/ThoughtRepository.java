package com.xs.bbs.thought;

import java.util.List;
import java.util.Optional;

public interface ThoughtRepository {

    ThoughtPost save(Long userId, String content, ThoughtDegree degree, boolean allowRecommendation, boolean publicVisible);

    List<ThoughtPost> findAll();

    List<ThoughtPost> findByUserId(Long userId);

    Optional<ThoughtPost> findById(Long thoughtId);
}
