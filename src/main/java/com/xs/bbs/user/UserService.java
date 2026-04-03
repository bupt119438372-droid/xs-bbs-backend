package com.xs.bbs.user;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Cacheable(cacheNames = "userProfile", key = "#userId", unless = "#result == null")
    public UserProfile getUser(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }

    public UserProfile getRequiredUser(Long userId) {
        UserProfile userProfile = getUser(userId);
        if (userProfile == null) {
            throw new IllegalArgumentException("user does not exist");
        }
        return userProfile;
    }

    @Cacheable(cacheNames = "userList")
    public Collection<UserProfile> listUsers() {
        return userRepository.findAll();
    }

    @Transactional
    @CacheEvict(cacheNames = "userList", allEntries = true)
    public UserProfile createUser(String nickname, String bio) {
        return userRepository.save(nickname, bio);
    }
}
