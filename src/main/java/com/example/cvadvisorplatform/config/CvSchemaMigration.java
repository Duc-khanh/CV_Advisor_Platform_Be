package com.example.cvadvisorplatform.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CvSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        String dataType = jdbcTemplate.queryForObject(
                "SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cv' AND COLUMN_NAME = 'cv_text'",
                String.class
        );

        if (!"longtext".equalsIgnoreCase(dataType)) {
            jdbcTemplate.execute("ALTER TABLE cv MODIFY COLUMN cv_text LONGTEXT NOT NULL");
            log.info("Migrated cv.cv_text from {} to LONGTEXT", dataType);
        } else {
            log.debug("cv.cv_text already uses LONGTEXT");
        }
    }
}