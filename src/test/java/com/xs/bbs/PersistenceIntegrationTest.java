package com.xs.bbs;

import com.xs.bbs.social.FollowStatusView;
import com.xs.bbs.social.FollowRepository;
import com.xs.bbs.social.SocialGraphService;
import com.xs.bbs.thought.ThoughtDegree;
import com.xs.bbs.thought.ThoughtPost;
import com.xs.bbs.thought.ThoughtRepository;
import com.xs.bbs.user.UserProfile;
import com.xs.bbs.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PersistenceIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ThoughtRepository thoughtRepository;

    @Autowired
    private SocialGraphService socialGraphService;

    @Autowired
    private FollowRepository followRepository;

    @Test
    void shouldLoadSeedUsersFromDatabase() {
        assertThat(userRepository.findAll())
                .extracting(UserProfile::id)
                .contains(1L, 2L, 3L, 4L);
    }

    @Test
    void shouldPersistThoughtIntoDatabaseRepository() {
        ThoughtPost saved = thoughtRepository.save(
                1L,
                "把长期想做的事写下来，焦虑会变得具体一些。",
                ThoughtDegree.FOCUSED,
                true,
                true
        );

        assertThat(saved.id()).isNotNull();
        assertThat(saved.allowRecommendation()).isTrue();
        assertThat(saved.publicVisible()).isTrue();
        assertThat(thoughtRepository.findByUserId(1L))
                .extracting(ThoughtPost::id)
                .contains(saved.id());
    }

    @Test
    void shouldPersistFollowRelationship() {
        Long followerId = userRepository.save("关系测试A", "用于测试关注持久化").id();
        Long targetId = userRepository.save("关系测试B", "用于测试关注持久化").id();
        followRepository.saveIfAbsent(followerId, targetId);
        FollowStatusView status = socialGraphService.status(followerId, targetId);

        assertThat(status.followed()).isTrue();
        assertThat(status.mutualFollow()).isFalse();
    }
}
