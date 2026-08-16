package com.company.hunttech.core;

import com.company.hunttech.HunttechTestContainer;
import com.company.hunttech.entity.ai.AiCapability;
import com.company.hunttech.service.AiCredentialOwner;
import com.company.hunttech.service.AiExecutionResult;
import com.company.hunttech.service.AiExecutionService;
import com.company.hunttech.service.TextProcessingResult;
import com.company.hunttech.service.TextProcessingService;
import com.company.hunttech.service.TextProcessingServiceBean;
import com.haulmont.cuba.core.global.DevelopmentException;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Контейнерный тест {@link TextProcessingServiceBean} со стабом {@link AiExecutionService}
 * и проверкой соблюдения контракта AI-нотификации (USER/ADMIN API ключи, промпты из таблиц).
 */
public class TextProcessingServiceBeanTest {

    @ClassRule
    public static HunttechTestContainer cont = HunttechTestContainer.Common.INSTANCE;

    private TextProcessingServiceBean bean;
    private StubAiExecutionService stub;

    @Before
    public void setUp() throws Exception {
        stub = new StubAiExecutionService();
        bean = new TextProcessingServiceBean();
        injectField("aiExecutionService", stub);
    }

    private void injectField(String name, Object value) throws Exception {
        Field f = TextProcessingServiceBean.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(bean, value);
    }

    @Test
    public void testFormatHtmlWithAi() {
        stub.register(TextProcessingService.FUNCTION_TEXT_SMART_FORMAT_HTML,
                "<b>ОПЫТ РАБОТЫ</b><p>Java разработчик в HuntTech (2020-2024)</p><ul><li>Spring Boot</li><li>PostgreSQL</li></ul>");

        String input = "Опыт работы: Java разработчик в HuntTech (2020-2024)\n* Spring Boot\n* PostgreSQL";
        TextProcessingResult result = bean.formatHtmlWithResult(input);

        assertNotNull(result);
        assertTrue(result.isAiFormatted());
        assertNotNull(result.getAiExecution());
        assertEquals("test-model", result.getAiExecution().getModelName());
        assertEquals(AiCredentialOwner.ADMIN, result.getAiExecution().getCredentialOwner());
        assertTrue(result.getText().contains("ОПЫТ РАБОТЫ"));
        assertTrue(result.getText().contains("Spring Boot"));
        assertTrue(result.getText().contains("PostgreSQL"));
    }

    @Test
    public void testFormatPlainTextWithAi() {
        stub.register(TextProcessingService.FUNCTION_TEXT_SMART_FORMAT_PLAIN,
                "═══ ОПЫТ РАБОТЫ ═══\nJava разработчик в HuntTech (2020-2024)\n  • Spring Boot\n  • PostgreSQL");

        String input = "Опыт работы: Java разработчик в HuntTech (2020-2024)\n* Spring Boot\n* PostgreSQL";
        TextProcessingResult result = bean.formatPlainTextWithResult(input);

        assertNotNull(result);
        assertTrue(result.isAiFormatted());
        assertNotNull(result.getAiExecution());
        assertEquals("test-model", result.getAiExecution().getModelName());
        assertTrue(result.getText().contains("ОПЫТ РАБОТЫ"));
        assertTrue(result.getText().contains("Spring Boot"));
    }

    @Test
    public void testLocalHtmlFallbackOnAiFailure() {
        stub.setThrowException(true);

        String input = "Контакты\nИван Иванов\n+7 999 123-45-67\n\nОпыт работы\nJava разработчик в HuntTech\n- Spring Framework\n- PostgreSQL\n- Docker\n\nОбразование\nМГТУ им. Баумана, 2018";
        TextProcessingResult result = bean.formatHtmlWithResult(input);

        assertNotNull(result);
        assertFalse(result.isAiFormatted());
        assertNull(result.getAiExecution());
        assertTrue(result.getText().contains("КОНТАКТЫ"));
        assertTrue(result.getText().contains("ОПЫТ РАБОТЫ"));
        assertTrue(result.getText().contains("ОБРАЗОВАНИЕ"));
        assertTrue(result.getText().contains("<ul"));
        assertTrue(result.getText().contains("<li"));
        assertTrue(result.getText().contains("Spring Framework"));
        assertTrue(result.getText().contains("МГТУ им. Баумана"));
    }

    @Test
    public void testLocalPlainTextFallbackOnAiFailure() {
        stub.setThrowException(true);

        String input = "Контакты\nИван Иванов\n+7 999 123-45-67\n\nОпыт работы\nJava разработчик в HuntTech\n- Spring Framework\n- PostgreSQL\n\nНавыки\n* Java Core\n* SQL";
        TextProcessingResult result = bean.formatPlainTextWithResult(input);

        assertNotNull(result);
        assertFalse(result.isAiFormatted());
        assertNull(result.getAiExecution());
        assertTrue(result.getText().contains("═══ КОНТАКТЫ ═══"));
        assertTrue(result.getText().contains("═══ ОПЫТ РАБОТЫ ═══"));
        assertTrue(result.getText().contains("═══ НАВЫКИ ═══"));
        assertTrue(result.getText().contains("• Spring Framework"));
        assertTrue(result.getText().contains("• Java Core"));
    }

    @Test
    public void testEmptyInputHandling() {
        assertTrue(bean.formatHtml(null).isEmpty());
        assertTrue(bean.formatHtml("   ").isEmpty());
        assertTrue(bean.formatPlainText(null).isEmpty());
        assertTrue(bean.formatPlainText("   ").isEmpty());
    }

    private static final class StubAiExecutionService implements AiExecutionService {
        private final Map<String, String> responses = new LinkedHashMap<>();
        private boolean throwException = false;

        public void register(String code, String response) {
            responses.put(code, response);
        }

        public void setThrowException(boolean throwException) {
            this.throwException = throwException;
        }

        @Override
        public AiExecutionResult executeText(String functionCode, Map<String, Object> context) {
            if (throwException) {
                throw new DevelopmentException("AI провайдер временно недоступен (тестовая симуляция)");
            }
            String text = responses.get(functionCode);
            return AiExecutionResult.textResult(
                    functionCode, "Умное форматирование",
                    AiCapability.TEXT_GENERATION, "test-model", "test-provider",
                    AiCredentialOwner.ADMIN, text != null ? text : "");
        }

        @Override
        public AiExecutionResult executeImage(String functionCode, Map<String, Object> context,
                                              byte[] sourceImage, String sourceMimeType) {
            throw new UnsupportedOperationException("Не используется в TextProcessingService");
        }
    }
}
