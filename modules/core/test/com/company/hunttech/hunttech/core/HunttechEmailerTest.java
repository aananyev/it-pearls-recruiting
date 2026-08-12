package com.company.hunttech.hunttech.core;

import com.company.hunttech.HunttechTestContainer;
import com.company.hunttech.app.HunttechEmailer;
import com.haulmont.cuba.core.app.Emailer;
import com.haulmont.cuba.core.app.EmailerAPI;
import com.haulmont.cuba.core.entity.SendingMessage;
import com.haulmont.cuba.core.global.AppBeans;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Тесты переопределения платформенного {@link Emailer} на {@link HunttechEmailer}.
 *
 * <p>ВАЖНО: тестовый контейнер CUBA всегда подменяет {@code cuba_Emailer} на
 * {@code TestEmailer} (см. test-spring.xml из cuba-core-tests) — это штатное поведение,
 * чтобы тесты не отправляли реальные письма. Поэтому проверки делятся на:</p>
 * <ol>
 *   <li>статическую регистрацию бина в spring.xml проекта (что {@code cuba_Emailer}
 *       указывает на {@link HunttechEmailer} в боевом контексте);</li>
 *   <li>корректность наследования и сигнатуры переопределённого метода;</li>
 *   <li>наличие бина в контейнере (как {@code TestEmailer} — ожидаемая подмена).</li>
 * </ol>
 */
public class HunttechEmailerTest {

    @ClassRule
    public static HunttechTestContainer.Common container = HunttechTestContainer.Common.INSTANCE;

    @Test
    public void springXmlRegistersHunttechEmailerAsCubaEmailer() throws Exception {
        String springXmlPath = "/com/company/hunttech/spring.xml";
        try (InputStream is = HunttechEmailerTest.class.getResourceAsStream(springXmlPath)) {
            assertNotNull("spring.xml проекта не найден в classpath: " + springXmlPath, is);
            String xml = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            assertTrue("spring.xml должен содержать бин cuba_Emailer",
                    xml.contains("id=\"cuba_Emailer\""));
            assertTrue("бин cuba_Emailer должен указывать на HunttechEmailer",
                    xml.contains("class=\"com.company.hunttech.app.HunttechEmailer\""));
        }
    }

    @Test
    public void hunttechEmailerExtendsEmailerAndOverridesSendSendingMessage() throws Exception {
        assertTrue("HunttechEmailer должен наследовать платформенный Emailer",
                Emailer.class.isAssignableFrom(HunttechEmailer.class));

        Method sendMethod = HunttechEmailer.class.getDeclaredMethod("sendSendingMessage", SendingMessage.class);
        assertTrue("sendSendingMessage должен быть protected (как в Emailer)",
                Modifier.isProtected(sendMethod.getModifiers()));
        assertEquals("сигнатура sendSendingMessage не должна меняться",
                void.class, sendMethod.getReturnType());
        assertEquals("переопределённый метод не должен быть final",
                false, Modifier.isFinal(sendMethod.getModifiers()));
    }

    @Test
    public void emailerBeanExistsInContainer() {
        Emailer emailer = AppBeans.get(EmailerAPI.NAME, Emailer.class);
        assertNotNull("бин cuba_Emailer должен существовать в контейнере", emailer);
        // В тестовом контейнере ожидается подмена TestEmailer (чтобы не слать реальные письма).
        assertEquals("в тестовом контейнере cuba_Emailer подменяется TestEmailer",
                "com.haulmont.cuba.testsupport.TestEmailer", emailer.getClass().getName());
    }
}
