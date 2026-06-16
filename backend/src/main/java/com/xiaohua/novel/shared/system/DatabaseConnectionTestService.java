package com.xiaohua.novel.shared.system;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DatabaseConnectionTestService {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseConnectionTestService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DatabaseConnectionInfo inspect() {
        DatabaseConnectionInfo databaseInfo = jdbcTemplate.queryForObject(
                """
                SELECT
                    DATABASE(),
                    CURRENT_USER(),
                    VERSION(),
                    @@character_set_database,
                    @@collation_database,
                    NOW(6)
                """,
                (resultSet, rowNumber) -> new DatabaseConnectionInfo(
                        "UP",
                        resultSet.getString(1),
                        resultSet.getString(2),
                        resultSet.getString(3),
                        resultSet.getString(4),
                        resultSet.getString(5),
                        resultSet.getTimestamp(6).toLocalDateTime(),
                        null,
                        null));

        String flywayVersion = jdbcTemplate.queryForObject(
                "SELECT MAX(version) FROM flyway_schema_history WHERE success = 1",
                String.class);
        String migrationProbe = jdbcTemplate.queryForObject(
                "SELECT probe_value FROM system_probe WHERE probe_key = 'schema_initialized'",
                String.class);

        return new DatabaseConnectionInfo(
                databaseInfo.status(),
                databaseInfo.databaseName(),
                databaseInfo.currentUser(),
                databaseInfo.databaseVersion(),
                databaseInfo.characterSet(),
                databaseInfo.collation(),
                databaseInfo.serverTime(),
                flywayVersion,
                migrationProbe);
    }
}
