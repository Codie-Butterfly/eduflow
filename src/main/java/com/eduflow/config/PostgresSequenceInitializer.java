package com.eduflow.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Initializes PostgreSQL sequences to ensure they are in sync with actual data.
 * This fixes issues where sequences get out of sync after manual data imports
 * or when switching from other databases.
 */
@Slf4j
@Component
public class PostgresSequenceInitializer implements ApplicationRunner {

    @PersistenceContext
    private EntityManager entityManager;

    // Tables that use IDENTITY generation strategy
    private static final List<String> TABLES_WITH_SEQUENCES = List.of(
            "assessment_scores",
            "assessments",
            "attendance",
            "teacher_class_subjects"
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Checking and syncing PostgreSQL sequences...");

        for (String tableName : TABLES_WITH_SEQUENCES) {
            try {
                syncSequence(tableName);
            } catch (Exception e) {
                log.warn("Could not sync sequence for table {}: {}", tableName, e.getMessage());
            }
        }

        log.info("PostgreSQL sequence sync completed");
    }

    private void syncSequence(String tableName) {
        String sequenceName = tableName + "_id_seq";

        // Check if table exists and has data
        String checkTableSql = String.format(
                "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = '%s')",
                tableName
        );

        Boolean tableExists = (Boolean) entityManager
                .createNativeQuery(checkTableSql)
                .getSingleResult();

        if (!tableExists) {
            log.debug("Table {} does not exist yet, skipping sequence sync", tableName);
            return;
        }

        // Get max ID from table
        String maxIdSql = String.format("SELECT COALESCE(MAX(id), 0) FROM %s", tableName);
        Number maxId = (Number) entityManager
                .createNativeQuery(maxIdSql)
                .getSingleResult();

        if (maxId.longValue() > 0) {
            // Set the sequence to max id + 1
            String setValSql = String.format(
                    "SELECT setval('%s', %d, true)",
                    sequenceName,
                    maxId.longValue()
            );

            entityManager.createNativeQuery(setValSql).getSingleResult();
            log.info("Synced sequence {} to {}", sequenceName, maxId.longValue());
        } else {
            log.debug("Table {} is empty, no sequence sync needed", tableName);
        }
    }
}