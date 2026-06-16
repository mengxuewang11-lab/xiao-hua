CREATE TABLE system_probe (
    id BIGINT NOT NULL,
    probe_key VARCHAR(64) NOT NULL,
    probe_value VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_system_probe PRIMARY KEY (id),
    CONSTRAINT uk_system_probe_key UNIQUE (probe_key)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO system_probe (id, probe_key, probe_value)
VALUES (1, 'schema_initialized', 'Flyway migration V1 applied');
