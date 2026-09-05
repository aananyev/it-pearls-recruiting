package com.company.hunttech.core;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class OpenPositionRestQueriesContractTest {

    @Test
    public void testOpenPositionPublicExcludesUnderReviewPriority() throws Exception {
        File restQueriesFile = new File("../web/src/com/company/hunttech/rest-queries.xml");
        if (!restQueriesFile.exists()) {
            restQueriesFile = new File("modules/web/src/com/company/hunttech/rest-queries.xml");
        }
        assertTrue("rest-queries.xml должен существовать", restQueriesFile.exists());

        String content = new String(Files.readAllBytes(restQueriesFile.toPath()));
        assertTrue("openPositionPublic должен содержать фильтр исключения приоритета На проверку (-2)",
                content.contains("priority <> -2") || content.contains("priority != -2") || content.contains("e.priority <> -2"));
    }

    @Test
    public void testOpenPositionPublicExcludesDraftPositions() throws Exception {
        File restQueriesFile = new File("../web/src/com/company/hunttech/rest-queries.xml");
        if (!restQueriesFile.exists()) {
            restQueriesFile = new File("modules/web/src/com/company/hunttech/rest-queries.xml");
        }
        assertTrue("rest-queries.xml должен существовать", restQueriesFile.exists());

        String content = new String(Files.readAllBytes(restQueriesFile.toPath()));
        assertTrue("openPositionPublic должен содержать фильтр исключения черновиков (signDraft)",
                content.contains("signDraft is null or e.signDraft = false") || content.contains("signDraft = false"));
    }
}
