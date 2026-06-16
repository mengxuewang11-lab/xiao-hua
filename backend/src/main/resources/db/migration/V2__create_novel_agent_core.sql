CREATE TABLE novel_project (
    id VARCHAR(36) NOT NULL,
    title VARCHAR(200) NULL,
    original_idea TEXT NOT NULL,
    genre VARCHAR(100) NULL,
    status VARCHAR(40) NOT NULL,
    selected_proposal_id VARCHAR(36) NULL,
    current_chapter_no INT NOT NULL DEFAULT 0,
    foundation_version INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_novel_project PRIMARY KEY (id),
    CONSTRAINT ck_novel_project_current_chapter CHECK (current_chapter_no >= 0),
    CONSTRAINT ck_novel_project_foundation_version CHECK (foundation_version >= 0)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE novel_agent_run (
    id VARCHAR(36) NOT NULL,
    project_id VARCHAR(36) NOT NULL,
    state VARCHAR(64) NOT NULL,
    current_step VARCHAR(64) NULL,
    current_chapter_no INT NOT NULL DEFAULT 0,
    target_chapter_no INT NOT NULL DEFAULT 3,
    current_revision_no INT NOT NULL DEFAULT 0,
    pause_requested BOOLEAN NOT NULL DEFAULT FALSE,
    pause_reason VARCHAR(255) NULL,
    token_budget BIGINT NOT NULL DEFAULT 0,
    token_used BIGINT NOT NULL DEFAULT 0,
    cost_budget DECIMAL(14, 6) NOT NULL DEFAULT 0,
    cost_used DECIMAL(14, 6) NOT NULL DEFAULT 0,
    time_budget_seconds INT NOT NULL DEFAULT 3600,
    consecutive_failures INT NOT NULL DEFAULT 0,
    last_checkpoint VARCHAR(100) NULL,
    error_code VARCHAR(100) NULL,
    error_message VARCHAR(1000) NULL,
    started_at DATETIME(6) NULL,
    paused_at DATETIME(6) NULL,
    completed_at DATETIME(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_novel_agent_run PRIMARY KEY (id),
    CONSTRAINT fk_agent_run_project FOREIGN KEY (project_id) REFERENCES novel_project (id),
    CONSTRAINT ck_agent_run_chapters CHECK (
        current_chapter_no >= 0 AND target_chapter_no > 0 AND current_chapter_no <= target_chapter_no
    ),
    CONSTRAINT ck_agent_run_usage CHECK (
        token_budget >= 0 AND token_used >= 0 AND cost_budget >= 0 AND cost_used >= 0
    ),
    INDEX idx_agent_run_project_state (project_id, state),
    INDEX idx_agent_run_state_updated (state, updated_at)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE novel_agent_step (
    id VARCHAR(36) NOT NULL,
    run_id VARCHAR(36) NOT NULL,
    step_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    subject_key VARCHAR(100) NULL,
    revision_no INT NOT NULL DEFAULT 0,
    attempt_no INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 2,
    idempotency_key VARCHAR(255) NOT NULL,
    input_json JSON NULL,
    output_json JSON NULL,
    error_type VARCHAR(64) NULL,
    error_code VARCHAR(100) NULL,
    error_message VARCHAR(1000) NULL,
    lease_owner VARCHAR(100) NULL,
    lease_until DATETIME(6) NULL,
    started_at DATETIME(6) NULL,
    completed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_novel_agent_step PRIMARY KEY (id),
    CONSTRAINT fk_agent_step_run FOREIGN KEY (run_id) REFERENCES novel_agent_run (id),
    CONSTRAINT uk_agent_step_idempotency UNIQUE (idempotency_key),
    CONSTRAINT ck_agent_step_attempt CHECK (
        revision_no >= 0 AND attempt_no >= 0 AND max_attempts > 0
    ),
    INDEX idx_agent_step_run_status (run_id, status),
    INDEX idx_agent_step_lease (status, lease_until)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE novel_creative_proposal (
    id VARCHAR(36) NOT NULL,
    run_id VARCHAR(36) NOT NULL,
    project_id VARCHAR(36) NOT NULL,
    proposal_no TINYINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    premise TEXT NOT NULL,
    target_audience VARCHAR(255) NOT NULL,
    market_rationale TEXT NOT NULL,
    differentiator TEXT NOT NULL,
    risk_summary TEXT NOT NULL,
    proposal_json JSON NOT NULL,
    selected BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    selected_at DATETIME(6) NULL,
    CONSTRAINT pk_novel_creative_proposal PRIMARY KEY (id),
    CONSTRAINT fk_creative_proposal_run FOREIGN KEY (run_id) REFERENCES novel_agent_run (id),
    CONSTRAINT fk_creative_proposal_project FOREIGN KEY (project_id) REFERENCES novel_project (id),
    CONSTRAINT uk_creative_proposal_number UNIQUE (run_id, proposal_no),
    CONSTRAINT ck_creative_proposal_number CHECK (proposal_no BETWEEN 1 AND 3),
    INDEX idx_creative_proposal_project (project_id, created_at)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE novel_foundation (
    id VARCHAR(36) NOT NULL,
    project_id VARCHAR(36) NOT NULL,
    run_id VARCHAR(36) NOT NULL,
    version_no INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    world_json JSON NOT NULL,
    characters_json JSON NOT NULL,
    relationships_json JSON NOT NULL,
    main_plot_json JSON NOT NULL,
    stages_json JSON NOT NULL,
    foreshadowing_json JSON NOT NULL,
    story_bible_json JSON NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    confirmed_at DATETIME(6) NULL,
    CONSTRAINT pk_novel_foundation PRIMARY KEY (id),
    CONSTRAINT fk_novel_foundation_project FOREIGN KEY (project_id) REFERENCES novel_project (id),
    CONSTRAINT fk_novel_foundation_run FOREIGN KEY (run_id) REFERENCES novel_agent_run (id),
    CONSTRAINT uk_novel_foundation_version UNIQUE (project_id, version_no),
    CONSTRAINT ck_novel_foundation_version CHECK (version_no > 0),
    INDEX idx_novel_foundation_status (project_id, status)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE novel_chapter_plan (
    id VARCHAR(36) NOT NULL,
    project_id VARCHAR(36) NOT NULL,
    foundation_id VARCHAR(36) NOT NULL,
    chapter_no INT NOT NULL,
    title VARCHAR(200) NOT NULL,
    objective TEXT NOT NULL,
    conflict_summary TEXT NOT NULL,
    key_events_json JSON NOT NULL,
    character_changes_json JSON NOT NULL,
    foreshadowing_actions_json JSON NOT NULL,
    climax TEXT NOT NULL,
    ending_hook TEXT NOT NULL,
    forbidden_events_json JSON NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_novel_chapter_plan PRIMARY KEY (id),
    CONSTRAINT fk_chapter_plan_project FOREIGN KEY (project_id) REFERENCES novel_project (id),
    CONSTRAINT fk_chapter_plan_foundation FOREIGN KEY (foundation_id) REFERENCES novel_foundation (id),
    CONSTRAINT uk_chapter_plan_number UNIQUE (project_id, chapter_no),
    CONSTRAINT ck_chapter_plan_number CHECK (chapter_no > 0),
    INDEX idx_chapter_plan_status (project_id, status, chapter_no)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE novel_chapter (
    id VARCHAR(36) NOT NULL,
    project_id VARCHAR(36) NOT NULL,
    run_id VARCHAR(36) NOT NULL,
    chapter_plan_id VARCHAR(36) NOT NULL,
    chapter_no INT NOT NULL,
    version_no INT NOT NULL DEFAULT 1,
    title VARCHAR(200) NOT NULL,
    content LONGTEXT NOT NULL,
    summary TEXT NOT NULL,
    metadata_json JSON NOT NULL,
    quality_report_json JSON NOT NULL,
    continuity_report_json JSON NOT NULL,
    word_count INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    confirmed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_novel_chapter PRIMARY KEY (id),
    CONSTRAINT fk_novel_chapter_project FOREIGN KEY (project_id) REFERENCES novel_project (id),
    CONSTRAINT fk_novel_chapter_run FOREIGN KEY (run_id) REFERENCES novel_agent_run (id),
    CONSTRAINT fk_novel_chapter_plan FOREIGN KEY (chapter_plan_id) REFERENCES novel_chapter_plan (id),
    CONSTRAINT uk_novel_chapter_version UNIQUE (project_id, chapter_no, version_no),
    CONSTRAINT ck_novel_chapter_values CHECK (
        chapter_no > 0 AND version_no > 0 AND word_count > 0
    ),
    INDEX idx_novel_chapter_project_number (project_id, chapter_no, status)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE novel_story_fact (
    id VARCHAR(36) NOT NULL,
    project_id VARCHAR(36) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_id VARCHAR(36) NOT NULL,
    fact_type VARCHAR(64) NOT NULL,
    subject_key VARCHAR(200) NOT NULL,
    predicate_key VARCHAR(200) NOT NULL,
    value_json JSON NOT NULL,
    status VARCHAR(32) NOT NULL,
    effective_chapter_no INT NOT NULL DEFAULT 0,
    version_no INT NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    invalidated_at DATETIME(6) NULL,
    CONSTRAINT pk_novel_story_fact PRIMARY KEY (id),
    CONSTRAINT fk_story_fact_project FOREIGN KEY (project_id) REFERENCES novel_project (id),
    CONSTRAINT ck_story_fact_values CHECK (
        effective_chapter_no >= 0 AND version_no > 0
    ),
    INDEX idx_story_fact_lookup (
        project_id, fact_type, subject_key, status, effective_chapter_no
    ),
    INDEX idx_story_fact_source (source_type, source_id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE novel_decision_request (
    id VARCHAR(36) NOT NULL,
    run_id VARCHAR(36) NOT NULL,
    project_id VARCHAR(36) NOT NULL,
    triggering_step_id VARCHAR(36) NULL,
    decision_type VARCHAR(64) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    context_text TEXT NOT NULL,
    options_json JSON NOT NULL,
    recommendation_json JSON NULL,
    impact_json JSON NOT NULL,
    resume_state VARCHAR(64) NOT NULL,
    resume_step VARCHAR(64) NULL,
    answer_json JSON NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    resolved_at DATETIME(6) NULL,
    CONSTRAINT pk_novel_decision_request PRIMARY KEY (id),
    CONSTRAINT fk_decision_request_run FOREIGN KEY (run_id) REFERENCES novel_agent_run (id),
    CONSTRAINT fk_decision_request_project FOREIGN KEY (project_id) REFERENCES novel_project (id),
    CONSTRAINT fk_decision_request_step FOREIGN KEY (triggering_step_id) REFERENCES novel_agent_step (id),
    INDEX idx_decision_request_open (run_id, status, priority, created_at)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE novel_checkpoint (
    id VARCHAR(36) NOT NULL,
    run_id VARCHAR(36) NOT NULL,
    project_id VARCHAR(36) NOT NULL,
    checkpoint_type VARCHAR(64) NOT NULL,
    chapter_no INT NOT NULL DEFAULT 0,
    state VARCHAR(64) NOT NULL,
    snapshot_json JSON NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_novel_checkpoint PRIMARY KEY (id),
    CONSTRAINT fk_checkpoint_run FOREIGN KEY (run_id) REFERENCES novel_agent_run (id),
    CONSTRAINT fk_checkpoint_project FOREIGN KEY (project_id) REFERENCES novel_project (id),
    CONSTRAINT ck_checkpoint_chapter CHECK (chapter_no >= 0),
    INDEX idx_checkpoint_latest (run_id, created_at)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE model_call_record (
    id VARCHAR(36) NOT NULL,
    run_id VARCHAR(36) NOT NULL,
    step_id VARCHAR(36) NOT NULL,
    provider VARCHAR(100) NOT NULL,
    model_name VARCHAR(150) NOT NULL,
    task_type VARCHAR(64) NOT NULL,
    prompt_template_version VARCHAR(64) NOT NULL,
    request_hash VARCHAR(128) NULL,
    input_tokens BIGINT NOT NULL DEFAULT 0,
    output_tokens BIGINT NOT NULL DEFAULT 0,
    cost_amount DECIMAL(14, 6) NOT NULL DEFAULT 0,
    duration_ms BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    error_code VARCHAR(100) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at DATETIME(6) NULL,
    CONSTRAINT pk_model_call_record PRIMARY KEY (id),
    CONSTRAINT fk_model_call_run FOREIGN KEY (run_id) REFERENCES novel_agent_run (id),
    CONSTRAINT fk_model_call_step FOREIGN KEY (step_id) REFERENCES novel_agent_step (id),
    CONSTRAINT ck_model_call_usage CHECK (
        input_tokens >= 0 AND output_tokens >= 0 AND cost_amount >= 0 AND duration_ms >= 0
    ),
    INDEX idx_model_call_budget (run_id, created_at),
    INDEX idx_model_call_step (step_id, status)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
