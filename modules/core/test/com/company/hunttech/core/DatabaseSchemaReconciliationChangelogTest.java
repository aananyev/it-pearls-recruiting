package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DatabaseSchemaReconciliationChangelogTest {

    private static final String CHANGELOG =
            "modules/core/db/changelog/260727-1-reconcileProductionSchema.xml";
    private static final String PROFILE_COMPLETION_CHANGELOG =
            "modules/core/db/changelog/260727-2-completeUserAiProfileColumns.xml";
    private static final String ADMIN_FALLBACK_CONSENT_CHANGELOG =
            "modules/core/db/changelog/260904-1-addAdminFallbackConsent.xml";
    private static final String SECURE_USER_AI_CREDENTIAL_CHANGELOG =
            "modules/core/db/changelog/260904-4-addUserAiEncryptedKey.xml";
    private static final String ACCOUNTING_BOT_CHANGELOG =
            "modules/core/db/changelog/260729-1-addAccountingBotEntities.xml";
    private static final String OUTSTAFFING_RATES_SQL =
            "modules/core/db/update/postgres/26/260731-1-addOutstaffingRatesMarginColumns.sql";
    private static final String CHANGELOG_MASTER =
            "modules/core/db/changelog/db.changelog-master.xml";
    private static final String CUBA_UPDATE_SQL =
            "modules/core/db/update/postgres/26/260727-2-reconcileProductionSchema.sql";
    private static final String ADMIN_FALLBACK_CONSENT_SQL =
            "modules/core/db/update/postgres/26/260904-1-addAdminFallbackConsent.sql";
    private static final String SECURE_USER_AI_CREDENTIAL_SQL =
            "modules/core/db/update/postgres/26/260904-4-addUserAiEncryptedKey.sql";
    private static final String AI_AUDIT_SECURITY_CHANGELOG =
            "modules/core/db/changelog/260905-1-addAiAuditSecuritySnapshots.xml";
    private static final String AI_AUDIT_SECURITY_SQL =
            "modules/core/db/update/postgres/26/260905-1-addAiAuditSecuritySnapshots.sql";
    private static final String USER_SETTINGS =
            "modules/global/src/com/company/hunttech/entity/UserSettings.java";
    private static final String USER_AI_PROFILE =
            "modules/global/src/com/company/hunttech/entity/UserAiProfile.java";
    private static final String SETTINGS_GRADLE = "settings.gradle";
    private static final String CUBA_BASELINE_GRADLE = "gradle/cuba-db-baseline.gradle";

    @Test
    public void masterUsesOrderedReconciliationChangelogChain() throws IOException {
        String master = readProjectFile(CHANGELOG_MASTER);

        // Старые AI-файлы не возвращаются в активную цепочку; новые additive-файлы идут после сверки.
        assertTrue(countOccurrences(master, "<include file=") >= 3);
        int reconciliationIndex = master.indexOf("260727-1-reconcileProductionSchema.xml");
        int completionIndex = master.indexOf("260727-2-completeUserAiProfileColumns.xml");
        int accountingBotIndex = master.indexOf("260729-1-addAccountingBotEntities.xml");
        assertTrue(reconciliationIndex >= 0);
        assertTrue(completionIndex > reconciliationIndex);
        assertTrue(accountingBotIndex > completionIndex);
    }

    @Test
    public void everyReconciliationChangeSetMarksExistingObjectsAsRan() throws IOException {
        String changelog = readProjectFile(CHANGELOG)
                + readProjectFile(PROFILE_COMPLETION_CHANGELOG);

        assertEquals(countOccurrences(changelog, "<changeSet "),
                countOccurrences(changelog, "onFail=\"MARK_RAN\""));
        assertTrue(changelog.contains("HUNTTECH_USER_AI_CONFIGURATION"));
        assertTrue(changelog.contains("HUNTTECH_VACANCY_PROMPT_TEMPLATE"));
        assertTrue(changelog.contains("HUNTTECH_USER_AI_PROFILE"));
        assertTrue(changelog.contains("PREFER_PERSONAL_AI_API_SETTINGS"));
        assertTrue(changelog.contains("PREFER_PERSONAL_PROMPTS"));
    }

    @Test
    public void everyUserAiProfileEntityColumnIsCoveredByBothMigrationPaths() throws IOException {
        String entity = readProjectFile(USER_AI_PROFILE);
        String liquibase = readProjectFile(CHANGELOG)
                + readProjectFile(PROFILE_COMPLETION_CHANGELOG)
                + readProjectFile(ADMIN_FALLBACK_CONSENT_CHANGELOG);
        String cubaSql = readProjectFile(CUBA_UPDATE_SQL)
                + readProjectFile(ADMIN_FALLBACK_CONSENT_SQL);
        Set<String> entityColumns = extractEntityColumnNames(entity);

        // Контракт фиксирует 36 @Column и один @JoinColumn текущей entity-модели.
        assertEquals(37, entityColumns.size());
        for (String column : entityColumns) {
            assertTrue("Liquibase не содержит колонку UserAiProfile: " + column,
                    liquibase.contains(column));
            assertTrue("CUBA update SQL не содержит колонку UserAiProfile: " + column,
                    cubaSql.contains(column));
        }

        assertTrue(readProjectFile(PROFILE_COMPLETION_CHANGELOG)
                .contains("ADD COLUMN IF NOT EXISTS COMMUNICATION_CONSTRAINTS"));
        assertTrue(cubaSql.contains("ADD COLUMN IF NOT EXISTS COMMUNICATION_CONSTRAINTS"));
    }

    @Test
    public void personalCredentialMigrationIsAdditiveAndMasterRegistered() throws IOException {
        String master = readProjectFile(CHANGELOG_MASTER);
        String liquibase = readProjectFile(SECURE_USER_AI_CREDENTIAL_CHANGELOG);
        String cubaSql = readProjectFile(SECURE_USER_AI_CREDENTIAL_SQL);
        assertTrue(master.contains("260904-4-addUserAiEncryptedKey.xml"));
        assertTrue(liquibase.contains("API_KEY_ENCRYPTED"));
        assertTrue(cubaSql.contains("ADD COLUMN IF NOT EXISTS API_KEY_ENCRYPTED"));
        assertFalse(liquibase.contains("API_KEY ="));
        assertFalse(cubaSql.contains("API_KEY ="));
    }

    @Test
    public void aiAuditSecurityMigrationIsAdditiveAndDoesNotDeleteHistoricalPayload() throws IOException {
        String master = readProjectFile(CHANGELOG_MASTER);
        String changelog = readProjectFile(AI_AUDIT_SECURITY_CHANGELOG);
        String sql = readProjectFile(AI_AUDIT_SECURITY_SQL);

        assertTrue(master.contains("260905-1-addAiAuditSecuritySnapshots.xml"));
        assertTrue(changelog.contains("PRIVACY_POLICY_VERSION"));
        assertTrue(changelog.contains("PRIVACY_POLICY_VERSION_SNAPSHOT"));
        assertTrue(changelog.contains("LEGACY_NOT_CAPTURED"));
        assertTrue(sql.contains("ADD COLUMN IF NOT EXISTS PRIVACY_POLICY_VERSION"));
        assertTrue(sql.contains("LEGACY_NOT_CAPTURED"));
        assertFalse(sql.toLowerCase(Locale.ROOT).contains("prompt_text = null"));
        assertFalse(sql.toLowerCase(Locale.ROOT).contains("response_text = null"));
        assertFalse(changelog.toLowerCase(Locale.ROOT).contains("delete from"));
    }

    @Test
    public void imageColumnMigrationPreservesDataAndMatchesEntityMapping() throws IOException {
        String changelog = readProjectFile(CHANGELOG);
        String cubaSql = readProjectFile(CUBA_UPDATE_SQL);
        String entity = readProjectFile(USER_SETTINGS);

        assertTrue(changelog.contains("oldColumnName=\"IMAGE_ID\""));
        assertTrue(changelog.contains("newColumnName=\"FILE_IMAGE_FACE\""));
        assertTrue(changelog.contains("SET FILE_IMAGE_FACE = IMAGE_ID"));
        assertTrue(cubaSql.contains("RENAME COLUMN IMAGE_ID TO FILE_IMAGE_FACE"));
        assertTrue(cubaSql.contains("SET FILE_IMAGE_FACE = IMAGE_ID"));
        assertTrue(entity.contains("@JoinColumn(name = \"FILE_IMAGE_FACE\")"));
        assertFalse(entity.contains("@JoinColumn(name = \"IMAGE_ID\")"));
    }

    @Test
    public void cubaUpdateSqlIsIdempotentAndRegisteredByStandardUpdateDb() throws IOException {
        String cubaSql = readProjectFile(CUBA_UPDATE_SQL);

        assertTrue(cubaSql.contains("CREATE TABLE IF NOT EXISTS HUNTTECH_USER_AI_PROFILE"));
        assertTrue(cubaSql.contains("ADD COLUMN IF NOT EXISTS"));
        assertTrue(cubaSql.contains("CREATE UNIQUE INDEX IF NOT EXISTS"));
        assertTrue(cubaSql.contains("DO $$"));
        assertTrue(cubaSql.trim().endsWith("^") || cubaSql.trim().endsWith("$$;"));
        assertFalse(cubaSql.contains("INSERT INTO SYS_DB_CHANGELOG"));
    }

    @Test
    public void updateDbBaselinesOnlyKnownProductionLegacyAliases() throws IOException {
        String settings = readProjectFile(SETTINGS_GRADLE);
        String baseline = readProjectFile(CUBA_BASELINE_GRADLE);

        assertTrue(settings.contains("gradle/cuba-db-baseline.gradle"));
        assertTrue(baseline.contains("updateDbTask.doFirst"));
        assertTrue(baseline.contains("source_suffix"));
        assertTrue(baseline.contains("LIKE '%/update/postgres/' || lower(aliases.source_suffix)"));
        assertTrue(baseline.contains("260727-2-reconcileProductionSchema.sql"));
        assertTrue(baseline.contains("INSERT INTO SYS_DB_CHANGELOG"));
        assertTrue(baseline.contains("ON CONFLICT (SCRIPT_NAME) DO NOTHING"));
        assertTrue(baseline.contains("public.sec_user"));
        assertTrue(baseline.contains("public.sys_db_changelog"));
    }

    @Test
    public void reconciliationContainsNoDestructiveStatements() throws IOException {
        String migrations = (readProjectFile(CHANGELOG)
                + readProjectFile(PROFILE_COMPLETION_CHANGELOG)
                + readProjectFile(ACCOUNTING_BOT_CHANGELOG)
                + readProjectFile(CUBA_UPDATE_SQL)
                + readProjectFile(OUTSTAFFING_RATES_SQL)).toLowerCase(Locale.ROOT);

        // Допустимы additive DDL, rename и точечное заполнение обязательных значений.
        assertFalse(migrations.contains("<droptable"));
        assertFalse(migrations.contains("<dropcolumn"));
        assertFalse(migrations.contains("drop table"));
        assertFalse(migrations.contains("drop column"));
        assertFalse(migrations.contains("<delete"));
        assertFalse(migrations.contains("delete from"));
        assertFalse(migrations.contains("truncate"));
        assertFalse(migrations.contains("hunttech_user_ai_profile_parameters"));
    }

    @Test
    public void outstaffingRatesMigrationIsIdempotentAndCoversMarginColumnsAuditTriggers()
            throws IOException {
        String sql = readProjectFile(OUTSTAFFING_RATES_SQL);

        // 4 колонки маржинальности добавляются идемпотентно
        for (String column : new String[]{"MARGIN_TK", "MARGIN_IE", "NET_PROFIT_TK", "NET_PROFIT_IE"}) {
            assertTrue("Нет колонки " + column + " в миграции рейтов",
                    sql.contains("ADD COLUMN IF NOT EXISTS " + column));
        }

        // Аудит-таблица + индекс создаются идемпотентно
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS HUNTTECH_OUTSTAFFING_RATES_HISTORY"));
        assertTrue(sql.contains("CREATE INDEX IF NOT EXISTS idx_orr_history_on_rate_id"));

        // Триггеры и функции пересоздаются идемпотентно
        assertTrue(sql.contains("CREATE OR REPLACE FUNCTION fn_outstaffing_margin_recalc"));
        assertTrue(sql.contains("CREATE OR REPLACE FUNCTION fn_outstaffing_rates_audit"));
        for (String trigger : new String[]{"trg_orr_margin_recalc", "trg_orr_audit_insert", "trg_orr_audit_update"}) {
            assertTrue("Нет триггера " + trigger + " в миграции рейтов",
                    sql.contains("DROP TRIGGER IF EXISTS " + trigger)
                            && sql.contains("CREATE TRIGGER " + trigger));
        }

        // Аудит-триггер UPDATE пишет только при реальном изменении бизнес-полей
        assertTrue(sql.contains("IS DISTINCT FROM NEW."));

        // updateDb сам регистрирует скрипт в SYS_DB_CHANGELOG
        assertFalse(sql.contains("INSERT INTO SYS_DB_CHANGELOG"));
    }

    private static Set<String> extractEntityColumnNames(String entity) {
        Pattern pattern = Pattern.compile(
                "@(?:Column|JoinColumn)\\([^)]*name\\s*=\\s*\"([A-Z0-9_]+)\"");
        Matcher matcher = pattern.matcher(entity);
        Set<String> columns = new LinkedHashSet<>();
        while (matcher.find()) {
            columns.add(matcher.group(1));
        }
        return columns;
    }

    private static String readProjectFile(String relativePath) throws IOException {
        Path root = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("build.gradle"))) {
            root = root.getParent();
        }
        if (root == null) {
            throw new IOException("Не найден корень проекта HRM HuntTech для " + relativePath);
        }
        Path path = root.resolve(relativePath);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static int countOccurrences(String source, String token) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }
}
