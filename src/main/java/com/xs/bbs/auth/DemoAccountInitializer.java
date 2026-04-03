package com.xs.bbs.auth;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xs.bbs.user.UserProfile;
import com.xs.bbs.user.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class DemoAccountInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final UserAccountMapper userAccountMapper;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public DemoAccountInitializer(UserRepository userRepository, UserAccountMapper userAccountMapper) {
        this.userRepository = userRepository;
        this.userAccountMapper = userAccountMapper;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Long count = userAccountMapper.selectCount(Wrappers.emptyWrapper());
        if (count != null && count > 0) {
            return;
        }
        Map<Long, String> usernames = Map.of(
                1L, "linxi",
                2L, "zhouye",
                3L, "shenqing",
                4L, "xuxing"
        );
        for (UserProfile profile : userRepository.findAll()) {
            UserAccountEntity entity = new UserAccountEntity();
            entity.setUserId(profile.id());
            entity.setUsername(usernames.getOrDefault(profile.id(), "user" + profile.id()));
            entity.setPasswordHash(encoder.encode("123456"));
            entity.setCreatedAt(LocalDateTime.now());
            userAccountMapper.insert(entity);
        }
    }
}
