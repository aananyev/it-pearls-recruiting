package com.company.hunttech.core;

import com.company.hunttech.HunttechTestContainer;
import com.company.hunttech.service.ProjectAiService;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.DevelopmentException;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/** Проверяет middleware-фасад ProjectEdit без платного внешнего AI-вызова. */
public class ProjectAiServiceTest {
    @ClassRule
    public static HunttechTestContainer cont = HunttechTestContainer.Common.INSTANCE;

    @Test
    public void serviceBeanIsResolvable() {
        assertNotNull(AppBeans.get(ProjectAiService.class));
        assertEquals("PROJECT_DESCRIPTION_GENERATE",
                ProjectAiService.FUNCTION_PROJECT_DESCRIPTION_GENERATE);
    }

    @Test(expected = DevelopmentException.class)
    public void blankUploadedTextIsRejectedBeforeAiCall() {
        AppBeans.get(ProjectAiService.class)
                .processUploadedDescription("Проект", "description.txt", "   ");
    }
}
