package com.xs.bbs.thought;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class DatabaseThoughtRepository implements ThoughtRepository {

    private final ThoughtPostMapper thoughtPostMapper;

    public DatabaseThoughtRepository(ThoughtPostMapper thoughtPostMapper) {
        this.thoughtPostMapper = thoughtPostMapper;
    }

    @Override
    public ThoughtPost save(Long userId, String content, ThoughtDegree degree, boolean allowRecommendation, boolean publicVisible) {
        ThoughtPostEntity entity = new ThoughtPostEntity();
        entity.setUserId(userId);
        entity.setContent(content);
        entity.setDegreeCode(degree.name());
        entity.setAllowRecommendation(allowRecommendation);
        entity.setPublicVisible(publicVisible);
        entity.setCreatedAt(LocalDateTime.now());
        thoughtPostMapper.insert(entity);
        return toDomain(entity);
    }

    @Override
    public List<ThoughtPost> findAll() {
        return thoughtPostMapper.selectList(
                        Wrappers.lambdaQuery(ThoughtPostEntity.class)
                                .orderByDesc(ThoughtPostEntity::getCreatedAt)
                ).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<ThoughtPost> findByUserId(Long userId) {
        return thoughtPostMapper.selectList(
                        Wrappers.lambdaQuery(ThoughtPostEntity.class)
                                .eq(ThoughtPostEntity::getUserId, userId)
                                .orderByDesc(ThoughtPostEntity::getCreatedAt)
                ).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<ThoughtPost> findById(Long thoughtId) {
        return Optional.ofNullable(thoughtPostMapper.selectById(thoughtId))
                .map(this::toDomain);
    }

    private ThoughtPost toDomain(ThoughtPostEntity entity) {
        return new ThoughtPost(
                entity.getId(),
                entity.getUserId(),
                entity.getContent(),
                ThoughtDegree.valueOf(entity.getDegreeCode()),
                Boolean.TRUE.equals(entity.getAllowRecommendation()),
                Boolean.TRUE.equals(entity.getPublicVisible()),
                entity.getCreatedAt()
        );
    }
}
