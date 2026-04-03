package com.xs.bbs.user;

import java.util.Collection;
import java.util.Optional;

public interface UserRepository {

    Optional<UserProfile> findById(Long userId);

    Collection<UserProfile> findAll();

    UserProfile save(String nickname, String bio);
}
