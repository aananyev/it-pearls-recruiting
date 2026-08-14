package com.company.hunttech.core;

import com.company.hunttech.HunttechTestContainer;
import com.company.hunttech.service.ProjectAiService;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.DevelopmentException;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Проверяет middleware-фасад ProjectEdit без платного внешнего AI-вызова. */
public class ProjectAiServiceTest {
    @ClassRule
    public static HunttechTestContainer cont = HunttechTestContainer.Common.INSTANCE;

    @Test
    public void serviceBeanIsResolvable() {
        assertNotNull(AppBeans.get(ProjectAiService.class));
        assertEquals("PROJECT_DESCRIPTION_GENERATE",
                ProjectAiService.FUNCTION_PROJECT_DESCRIPTION_GENERATE);
        assertEquals("PROJECT_SHORT_DESCRIPTION_GENERATE",
                ProjectAiService.FUNCTION_PROJECT_SHORT_DESCRIPTION_GENERATE);
    }

    @Test
    public void blankUploadedTextIsRejectedBeforeAiCall() {
        // Сервис вызывается через middleware-прокси: ServiceInterceptor оборачивает
        // DevelopmentException в RemoteException — проверяем по цепочке причин.
        assertRejectedBeforeAiCall(() -> AppBeans.get(ProjectAiService.class)
                .processUploadedDescription("Проект", "description.txt", "   "));
    }

    @Test
    public void blankDescriptionTextIsRejectedBeforeAiCall() {
        assertRejectedBeforeAiCall(() -> AppBeans.get(ProjectAiService.class)
                .generateShortDescription("Проект", "   "));
    }

    private void assertRejectedBeforeAiCall(Runnable invocation) {
        try {
            invocation.run();
            fail("Ожидалось отклонение до AI-вызова");
        } catch (Throwable throwable) {
            assertTrue("Ожидался DevelopmentException (в цепочке причин или в сообщении "
                            + "RemoteException middleware), получено: " + throwable,
                    isOrWrapsDevelopmentException(throwable));
        }
    }

    private boolean isOrWrapsDevelopmentException(Throwable throwable) {
        StringBuilder chain = new StringBuilder();
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof DevelopmentException) {
                return true;
            }
            chain.append(current).append('\n');
            current = current.getCause();
        }
        // Middleware-прокси сериализует исходное исключение в текст RemoteException
        // (getMessage()/getCause() на клиенте не сохраняются) — проверяем весь текст.
        return chain.toString().contains("com.haulmont.cuba.core.global.DevelopmentException");
    }
}
