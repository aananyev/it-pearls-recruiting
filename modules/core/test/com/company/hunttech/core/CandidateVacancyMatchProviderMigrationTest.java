package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CandidateVacancyMatchProviderMigrationTest {

    private Path projectRoot() {
        Path root = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("build.gradle"))) {
            root = root.getParent();
        }
        org.junit.Assert.assertNotNull("Не найден корень проекта HRM HuntTech", root);
        return root;
    }

    @Test
    public void testMigrationIsRegisteredAndGuardedForPostgres() throws IOException {
        Path root = projectRoot();
        String master = read(root.resolve("modules/core/db/changelog/db.changelog-master.xml"));
        String changelog = read(root.resolve("modules/core/db/changelog/260921-1-routeCandidateVacancyMatchToDeepSeek.xml"));

        assertTrue(master.contains("260921-1-routeCandidateVacancyMatchToDeepSeek.xml"));
        assertTrue(changelog.contains("dbms=\"postgresql\""));
        assertTrue(changelog.contains("onFail=\"HALT\""));
        assertTrue(changelog.contains("onError=\"HALT\""));
        assertTrue(changelog.contains("IS_ACTIVE = TRUE"));
        assertTrue(changelog.contains("API_KEY_ENCRYPTED IS NOT NULL"));
        assertTrue(changelog.contains("<rollback>"));
        assertTrue(changelog.contains("260921-1-routeCandidateVacancyMatchToDeepSeek-rollback.sql"));
        assertTrue(changelog.contains("f.ADMIN_MODEL_NAME = a.DEFAULT_MODEL_NAME"));
        assertTrue(changelog.contains("to_regclass('public.hunttech_admin_ai_configuration')"));
        assertTrue(changelog.contains("f.IS_ACTIVE = TRUE"));
        assertTrue(changelog.contains("f.VERSION IS NOT NULL"));
        assertTrue(changelog.contains("a.PROVIDER_CODE = 'openrouter'"));
        assertTrue(changelog.contains("nvidia/nemotron-3-ultra-550b-a55b:free"));
        assertTrue(changelog.contains("../update/postgres/26/260921-1-routeCandidateVacancyMatchToDeepSeek.sql"));
        assertTrue(changelog.contains("relativeToChangelogFile=\"true\""));
    }

    @Test
    public void testMigrationRoutesVacancyMatchingToDeepSeekWithoutCopyingSecrets() throws IOException {
        Path root = projectRoot();
        String sql = read(root.resolve("modules/core/db/update/postgres/26/260921-1-routeCandidateVacancyMatchToDeepSeek.sql"));
        String changelog = read(root.resolve("modules/core/db/changelog/260921-1-routeCandidateVacancyMatchToDeepSeek.xml"));
        String rollback = read(root.resolve("modules/core/db/changelog/260921-1-routeCandidateVacancyMatchToDeepSeek-rollback.sql"));

        assertTrue(sql.contains("CODE = 'CANDIDATE_VACANCY_MATCH_ANALYZE'"));
        assertTrue(sql.contains("PROVIDER_CODE = 'deepseek'"));
        assertTrue(sql.contains("PROVIDER_CODE = 'openrouter'"));
        assertTrue(sql.contains("nvidia/nemotron-3-ultra-550b-a55b:free"));
        assertFalse(sql.contains("09d16acf-71ca-460b-ada4-9576a7c21015"));
        assertFalse(sql.contains("HUNTTECH_USER_AI_CONFIGURATION"));
        assertFalse(sql.contains("PROVIDER_CODE = 'bai'"));
        assertTrue(sql.contains("WITH eligible_deepseek AS"));
        assertTrue(sql.contains("v_expected_source_model CONSTANT TEXT"));
        assertTrue(sql.contains("v_deepseek_config_count != 1"));
        assertTrue(sql.contains("v_openrouter_route_count != 0"));
        assertTrue(sql.contains("v_source_route_count > 1"));
        assertTrue(sql.contains("no marker means rollback must leave it unchanged"));
        assertTrue(sql.contains("source.PROVIDER_CODE = 'openrouter'"));
        assertTrue(sql.contains("PREVIOUS_ADMIN_CONFIGURATION_ID"));
        assertTrue(rollback.contains("SUBSTRING(UPDATED_BY FROM LENGTH('mig:CVM:') + 1)::uuid"));
        assertTrue(rollback.contains("UPDATED_BY ~ '^mig:CVM:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'"));
        assertTrue(rollback.contains("WHERE ID = v_previous_admin_id"));
        assertTrue(rollback.contains("SET ADMIN_CONFIGURATION_ID = v_previous_admin_id"));
        assertTrue(sql.contains("mig:CVM:"));
        assertTrue(sql.contains("GET DIAGNOSTICS v_updated_count = ROW_COUNT"));
        assertTrue(sql.contains("no unique eligible source route or valid existing DeepSeek route"));
        assertTrue(sql.contains("UPDATED_BY = 'mig:CVM:' || c.PREVIOUS_ADMIN_CONFIGURATION_ID::text"));
        assertTrue(sql.contains("AND f.ADMIN_CONFIGURATION_ID = c.PREVIOUS_ADMIN_CONFIGURATION_ID"));
        assertTrue(sql.contains("AND f.ADMIN_MODEL_NAME = v_expected_source_model"));
        assertTrue(rollback.contains("v_restored_count INTEGER"));
        assertTrue(rollback.contains("No migration marker found: assuming the route predated this changeset"));
        assertTrue(rollback.contains("GET DIAGNOSTICS v_restored_count = ROW_COUNT"));
        assertTrue(changelog.contains("AND a.IS_ACTIVE = TRUE"));
        assertTrue(changelog.contains("AND a.API_KEY_ENCRYPTED IS NOT NULL"));
        assertFalse(sql.contains("SET API_KEY"));
    }

    private String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
