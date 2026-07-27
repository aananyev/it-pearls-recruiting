package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DatabaseSchemaReconciliationChangelogTest {

    private static final String CHANGELOG =
            "modules/core/db/changelog/260727-1-reconcileProductionSchema.xml";
    private static final String CHANGELOG_MASTER =
            "modules/core/db/changelog/db.changelog-master.xml";
    private static final String USER_SETTINGS =
            "modules/global/src/com/company/hunttech/entity/UserSettings.java";

    @Test
    public void masterUsesOnlyConsolidatedReconciliationChangelog() throws IOException {
        String master = readProjectFile(CHANGELOG_MASTER);

        // Старые файлы остаются только исторической справкой и не должны выполняться раньше reconciliation.
        assertEquals(1, countOccurrences(master, "<include file="));
        assertTrue(master.contains("<include file=\"260727-1-reconcileProductionSchema.xml\""));
    }

    @Test
    public void everyReconciliationChangeSetMarksExistingObjectsAsRan() throws IOException {
        String changelog = readProjectFile(CHANGELOG);

        assertEquals(countOccurrences(changelog, "<changeSet "),
                countOccurrences(changelog, "onFail=\"MARK_RAN\""));
        assertTrue(changelog.contains("HUNTTECH_USER_AI_CONFIGURATION"));
        assertTrue(changelog.contains("HUNTTECH_VACANCY_PROMPT_TEMPLATE"));
        assertTrue(changelog.contains("HUNTTECH_USER_AI_PROFILE"));
        assertTrue(changelog.contains("PREFER_PERSONAL_AI_API_SETTINGS"));
        assertTrue(changelog.contains("PREFER_PERSONAL_PROMPTS"));
    }

    @Test
    public void imageColumnMigrationPreservesDataAndMatchesEntityMapping() throws IOException {
        String changelog = readProjectFile(CHANGELOG);
        String entity = readProjectFile(USER_SETTINGS);

        assertTrue(changelog.contains("oldColumnName=\"IMAGE_ID\""));
        assertTrue(changelog.contains("newColumnName=\"FILE_IMAGE_FACE\""));
        assertTrue(changelog.contains("SET FILE_IMAGE_FACE = IMAGE_ID"));
        assertTrue(entity.contains("@JoinColumn(name = \"FILE_IMAGE_FACE\")"));
        assertFalse(entity.contains("@JoinColumn(name = \"IMAGE_ID\")"));
    }

    @Test
    public void reconciliationContainsNoDestructiveStatements() throws IOException {
        String changelog = readProjectFile(CHANGELOG).toLowerCase(Locale.ROOT);

        // Миграция допускает только additive DDL, rename и точечное копирование UUID фотографии.
        assertFalse(changelog.contains("<droptable"));
        assertFalse(changelog.contains("<dropcolumn"));
        assertFalse(changelog.contains("drop table"));
        assertFalse(changelog.contains("drop column"));
        assertFalse(changelog.contains("<delete"));
        assertFalse(changelog.contains("delete from"));
        assertFalse(changelog.contains("truncate"));
        assertFalse(changelog.contains("hunttech_user_ai_profile_parameters"));
    }

    private static String readProjectFile(String relativePath) throws IOException {
        Path root = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("build.gradle"))) {
            root = root.getParent();
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
