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
    private static final String CHANGELOG_MASTER =
            "modules/core/db/changelog/db.changelog-master.xml";
    private static final String CUBA_UPDATE_SQL =
            "modules/core/db/update/postgres/26/260727-2-reconcileProductionSchema.sql";
    private static final String USER_SETTINGS =
            "modules/global/src/com/company/hunttech/entity/UserSettings.java";
    private static final String USER_AI_PROFILE =
            "modules/global/src/com/company/hunttech/entity/UserAiProfile.java";
    private static final String SETTINGS_GRADLE = "settings.gradle";
    private static final String CUBA_BASELINE_GRADLE = "gradle/cuba-db-baseline.gradle";

    @Test
    public void masterUsesOrderedReconciliationChangelogChain() throws IOException {
        String master = readProjectFile(CHANGELOG_MASTER);

        // Старые AI-файлы не возвращаются в активную цепочку; follow-up идёт после основной сверки.
        assertEquals(2, countOccurrences(master, "<include file="));
        int reconciliationIndex = master.indexOf("260727-1-reconcileProductionSchema.xml");
        int completionIndex = master.indexOf("260727-2-completeUserAiProfileColumns.xml");
        assertTrue(reconciliationIndex >= 0);
        assertTrue(completionIndex > reconciliationIndex);
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
                + readProjectFile(PROFILE_COMPLETION_CHANGELOG);
        String cubaSql = readProjectFile(CUBA_UPDATE_SQL);
        Set<String> entityColumns = extractEntityColumnNames(entity);

        // Контракт фиксирует 33 @Column и один @JoinColumn текущей entity-модели.
        assertEquals(34, entityColumns.size());
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
        assertTrue(cubaSql.trim().endsWith("^"));
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
                + readProjectFile(CUBA_UPDATE_SQL)).toLowerCase(Locale.ROOT);

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
