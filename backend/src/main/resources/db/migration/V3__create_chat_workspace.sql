CREATE TABLE app_user_profile (
    id VARCHAR(36) NOT NULL,
    username VARCHAR(100) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_app_user_profile PRIMARY KEY (id),
    CONSTRAINT uk_app_user_profile_username UNIQUE (username)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE user_ui_preference (
    user_id VARCHAR(36) NOT NULL,
    theme_id VARCHAR(40) NOT NULL DEFAULT 'peach',
    custom_accent_color VARCHAR(20) NOT NULL DEFAULT '#ff6ea8',
    sidebar_collapsed BOOLEAN NOT NULL DEFAULT FALSE,
    default_provider VARCHAR(40) NOT NULL DEFAULT 'zhipu',
    default_model VARCHAR(120) NOT NULL DEFAULT 'glm-4.7-flash',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_user_ui_preference PRIMARY KEY (user_id),
    CONSTRAINT fk_user_ui_preference_user FOREIGN KEY (user_id) REFERENCES app_user_profile (id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE chat_conversation (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    title VARCHAR(200) NOT NULL,
    provider VARCHAR(40) NOT NULL,
    model_name VARCHAR(120) NOT NULL,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_chat_conversation PRIMARY KEY (id),
    CONSTRAINT fk_chat_conversation_user FOREIGN KEY (user_id) REFERENCES app_user_profile (id),
    INDEX idx_chat_conversation_user_updated (user_id, archived, updated_at)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE chat_message (
    id VARCHAR(36) NOT NULL,
    conversation_id VARCHAR(36) NOT NULL,
    role VARCHAR(20) NOT NULL,
    content LONGTEXT NOT NULL,
    provider VARCHAR(40) NULL,
    model_name VARCHAR(120) NULL,
    prompt_tokens BIGINT NOT NULL DEFAULT 0,
    completion_tokens BIGINT NOT NULL DEFAULT 0,
    total_tokens BIGINT NOT NULL DEFAULT 0,
    error_code VARCHAR(100) NULL,
    error_message VARCHAR(1000) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_chat_message PRIMARY KEY (id),
    CONSTRAINT fk_chat_message_conversation FOREIGN KEY (conversation_id) REFERENCES chat_conversation (id),
    CONSTRAINT ck_chat_message_role CHECK (role IN ('system', 'user', 'assistant')),
    CONSTRAINT ck_chat_message_usage CHECK (
        prompt_tokens >= 0 AND completion_tokens >= 0 AND total_tokens >= 0
    ),
    INDEX idx_chat_message_conversation_created (conversation_id, created_at)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE chat_model_call_record (
    id VARCHAR(36) NOT NULL,
    conversation_id VARCHAR(36) NOT NULL,
    request_message_id VARCHAR(36) NOT NULL,
    response_message_id VARCHAR(36) NULL,
    provider VARCHAR(40) NOT NULL,
    model_name VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL,
    prompt_tokens BIGINT NOT NULL DEFAULT 0,
    completion_tokens BIGINT NOT NULL DEFAULT 0,
    total_tokens BIGINT NOT NULL DEFAULT 0,
    duration_ms BIGINT NOT NULL DEFAULT 0,
    error_code VARCHAR(100) NULL,
    error_message VARCHAR(1000) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at DATETIME(6) NULL,
    CONSTRAINT pk_chat_model_call_record PRIMARY KEY (id),
    CONSTRAINT fk_chat_model_call_conversation FOREIGN KEY (conversation_id) REFERENCES chat_conversation (id),
    CONSTRAINT fk_chat_model_call_request FOREIGN KEY (request_message_id) REFERENCES chat_message (id),
    CONSTRAINT fk_chat_model_call_response FOREIGN KEY (response_message_id) REFERENCES chat_message (id),
    CONSTRAINT ck_chat_model_call_usage CHECK (
        prompt_tokens >= 0 AND completion_tokens >= 0 AND total_tokens >= 0 AND duration_ms >= 0
    ),
    INDEX idx_chat_model_call_conversation (conversation_id, created_at)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO app_user_profile (id, username, display_name)
VALUES ('local-user', 'local', '本地创作者');

INSERT INTO user_ui_preference (
    user_id, theme_id, custom_accent_color, sidebar_collapsed, default_provider, default_model
) VALUES (
    'local-user', 'peach', '#ff6ea8', FALSE, 'zhipu', 'glm-4.7-flash'
);
