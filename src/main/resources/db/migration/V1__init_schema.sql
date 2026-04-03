CREATE TABLE IF NOT EXISTS user_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nickname VARCHAR(32) NOT NULL,
    bio VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS thought_post (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    content VARCHAR(500) NOT NULL,
    degree_code VARCHAR(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_thought_user FOREIGN KEY (user_id) REFERENCES user_profile (id)
);

CREATE INDEX idx_thought_user_created ON thought_post (user_id, created_at DESC);
CREATE INDEX idx_thought_created ON thought_post (created_at DESC);

CREATE TABLE IF NOT EXISTS user_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(128) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_user_account_user UNIQUE (user_id),
    CONSTRAINT uk_user_account_username UNIQUE (username),
    CONSTRAINT fk_user_account_user FOREIGN KEY (user_id) REFERENCES user_profile (id)
);

CREATE INDEX idx_user_account_username ON user_account (username);

CREATE TABLE IF NOT EXISTS outbox_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_type VARCHAR(64) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    payload_json TEXT NOT NULL,
    status VARCHAR(16) NOT NULL,
    retry_count INT NOT NULL,
    result_summary VARCHAR(255),
    last_error VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    next_retry_at TIMESTAMP NULL,
    processed_at TIMESTAMP NULL,
    dead_letter_reason VARCHAR(500),
    dead_lettered_at TIMESTAMP NULL
);

CREATE INDEX idx_outbox_status_retry_created ON outbox_event (status, next_retry_at, created_at);

CREATE TABLE IF NOT EXISTS notification_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    receiver_id BIGINT NOT NULL,
    sender_id BIGINT NULL,
    related_thought_id BIGINT NULL,
    title VARCHAR(100) NOT NULL,
    content VARCHAR(255) NOT NULL,
    read_flag BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    read_at TIMESTAMP NULL,
    CONSTRAINT fk_notification_receiver FOREIGN KEY (receiver_id) REFERENCES user_profile (id),
    CONSTRAINT fk_notification_sender FOREIGN KEY (sender_id) REFERENCES user_profile (id),
    CONSTRAINT fk_notification_thought FOREIGN KEY (related_thought_id) REFERENCES thought_post (id)
);

CREATE INDEX idx_notification_receiver_created ON notification_message (receiver_id, created_at DESC);
CREATE INDEX idx_notification_receiver_read ON notification_message (receiver_id, read_flag);

CREATE TABLE IF NOT EXISTS user_follow (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    follower_id BIGINT NOT NULL,
    target_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_follow UNIQUE (follower_id, target_id),
    CONSTRAINT fk_follow_follower FOREIGN KEY (follower_id) REFERENCES user_profile (id),
    CONSTRAINT fk_follow_target FOREIGN KEY (target_id) REFERENCES user_profile (id)
);

CREATE INDEX idx_follow_follower_target ON user_follow (follower_id, target_id);
