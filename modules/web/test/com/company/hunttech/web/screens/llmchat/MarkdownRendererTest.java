package com.company.hunttech.web.screens.llmchat;

import com.company.hunttech.entity.ai.LlmChatMessage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownRendererTest {

    @Test
    void testEmptyHistoryShowsHint() {
        String html = MarkdownRenderer.renderChatHistory(null, null);
        assertTrue(html.contains("llm-chat-empty-hint"));
        assertTrue(html.contains("Задайте вопрос ИИ-ассистенту"));
    }

    @Test
    void testHtmlEscapingPreventsXss() {
        String raw = "<script>alert('xss')</script> <b>bold tag</b> & special";
        String html = MarkdownRenderer.renderMarkdown(raw);

        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&lt;script&gt;"));
        assertFalse(html.contains("<b>bold tag</b>"));
        assertTrue(html.contains("&lt;b&gt;bold tag&lt;/b&gt;"));
        assertTrue(html.contains("&amp;"));
    }

    @Test
    void testBoldAndItalicAndInlineCode() {
        String raw = "Это **жирный** текст и *курсив*, а также `System.out.println()` код.";
        String html = MarkdownRenderer.renderMarkdown(raw);

        assertTrue(html.contains("<strong class=\"llm-md-strong\">жирный</strong>"));
        assertTrue(html.contains("<em class=\"llm-md-em\">курсив</em>"));
        assertTrue(html.contains("<code class=\"llm-md-code-inline\">System.out.println()</code>"));
    }

    @Test
    void testCodeBlocksWithLanguage() {
        String raw = "```python\ndef hello():\n    print(\"Hello <world>\")\n```";
        String html = MarkdownRenderer.renderMarkdown(raw);

        assertTrue(html.contains("<pre class=\"llm-md-pre lang-python\">"));
        assertTrue(html.contains("<code class=\"llm-md-code\">"));
        assertTrue(html.contains("print(&quot;Hello &lt;world&gt;&quot;)"));
        assertFalse(html.contains("<world>"));
    }

    @Test
    void testHeaders() {
        String raw = "# Заголовок 1\n## Заголовок 2\n### Заголовок 3";
        String html = MarkdownRenderer.renderMarkdown(raw);

        assertTrue(html.contains("<h3 class=\"llm-md-h3\">Заголовок 1</h3>"));
        assertTrue(html.contains("<h4 class=\"llm-md-h4\">Заголовок 2</h4>"));
        assertTrue(html.contains("<h5 class=\"llm-md-h5\">Заголовок 3</h5>"));
    }

    @Test
    void testLists() {
        String raw = "- Первый пункт\n- Второй пункт\n\n1. Шаг один\n2. Шаг два";
        String html = MarkdownRenderer.renderMarkdown(raw);

        assertTrue(html.contains("<ul class=\"llm-md-ul\">"));
        assertTrue(html.contains("<li class=\"llm-md-li\">Первый пункт</li>"));
        assertTrue(html.contains("<ol class=\"llm-md-ol\">"));
        assertTrue(html.contains("<li class=\"llm-md-li\">Шаг один</li>"));
    }

    @Test
    void testBlockquote() {
        String raw = "> Важное примечание для рекрутера";
        String html = MarkdownRenderer.renderMarkdown(raw);

        assertTrue(html.contains("<blockquote class=\"llm-md-quote\">"));
        assertTrue(html.contains("Важное примечание для рекрутера"));
    }

    @Test
    void testTable() {
        String raw = "| Навык | Уровень |\n| --- | --- |\n| Java | Senior |\n| PostgreSQL | Middle |";
        String html = MarkdownRenderer.renderMarkdown(raw);

        assertTrue(html.contains("<table class=\"llm-md-table\">"));
        assertTrue(html.contains("<th class=\"llm-md-th\">Навык</th>"));
        assertTrue(html.contains("<td class=\"llm-md-td\">Senior</td>"));
        assertTrue(html.contains("<td class=\"llm-md-td\">PostgreSQL</td>"));
    }

    @Test
    void testLinks() {
        String raw = "Подробнее на [HRM Portal](https://hunttech.internal/portal).";
        String html = MarkdownRenderer.renderMarkdown(raw);

        assertTrue(html.contains("<a href=\"https://hunttech.internal/portal\" target=\"_blank\" rel=\"noopener noreferrer\" class=\"llm-md-link\">HRM Portal</a>"));
    }

    @Test
    void testChatHistoryRenderingWithLiveStreaming() {
        List<LlmChatMessage> messages = new ArrayList<>();
        LlmChatMessage userMsg = new LlmChatMessage();
        userMsg.setRole("USER");
        userMsg.setContent("Привет! Найди Java-разработчиков.");
        messages.add(userMsg);

        LlmChatMessage aiMsg = new LlmChatMessage();
        aiMsg.setRole("ASSISTANT");
        aiMsg.setContent("Вот найденные кандидаты:\n- **Иван Иванов** (Lead)\n- **Петр Петров** (Senior)");
        messages.add(aiMsg);

        String html = MarkdownRenderer.renderChatHistory(messages, "Подбираю дополнительные контакты...");

        assertTrue(html.contains("llm-chat-msg-user"));
        assertTrue(html.contains("Вы"));
        assertTrue(html.contains("Привет! Найди Java-разработчиков."));

        assertTrue(html.contains("llm-chat-msg-ai"));
        assertTrue(html.contains("ИИ"));
        assertTrue(html.contains("<strong class=\"llm-md-strong\">Иван Иванов</strong>"));

        assertTrue(html.contains("llm-chat-msg-live"));
        assertTrue(html.contains("Генерация..."));
        assertTrue(html.contains("Подбираю дополнительные контакты..."));
        assertTrue(html.contains("llm-chat-cursor"));
    }

    @Test
    void testHrmCandidateLink() {
        String raw = "Рекомендую ознакомиться: [Иван Иванов](hrm://candidate/a1b2c3d4-e5f6-7890-abcd-ef1234567890)";
        String html = MarkdownRenderer.renderMarkdown(raw);

        assertTrue(html.contains("class=\"llm-md-link llm-hrm-entity-link\""));
        assertTrue(html.contains("data-entity=\"candidate\""));
        assertTrue(html.contains("data-id=\"a1b2c3d4-e5f6-7890-abcd-ef1234567890\""));
        assertTrue(html.contains("href=\"#main/0/hunttech_JobCandidate.edit?id=a1b2c3d4-e5f6-7890-abcd-ef1234567890\""));
        assertTrue(html.contains("<span class=\"llm-entity-icon\">👤</span>"));
        assertTrue(html.contains("Иван Иванов"));
    }

    @Test
    void testHrmVacancyLink() {
        String raw = "Открытая позиция: [Senior Java Developer](hrm://vacancy/12345678-1234-1234-1234-123456789abc)";
        String html = MarkdownRenderer.renderMarkdown(raw);

        assertTrue(html.contains("class=\"llm-md-link llm-hrm-entity-link\""));
        assertTrue(html.contains("data-entity=\"vacancy\""));
        assertTrue(html.contains("data-id=\"12345678-1234-1234-1234-123456789abc\""));
        assertTrue(html.contains("href=\"#main/0/hunttech_OpenPosition.edit?id=12345678-1234-1234-1234-123456789abc\""));
        assertTrue(html.contains("<span class=\"llm-entity-icon\">💼</span>"));
        assertTrue(html.contains("Senior Java Developer"));
    }

    @Test
    void testHrmInteractionLink() {
        String raw = "Смотри [Интервью от 05.09](hrm://interaction/ffffffff-ffff-ffff-ffff-ffffffffffff)";
        String html = MarkdownRenderer.renderMarkdown(raw);

        assertTrue(html.contains("class=\"llm-md-link llm-hrm-entity-link\""));
        assertTrue(html.contains("data-entity=\"interaction\""));
        assertTrue(html.contains("data-id=\"ffffffff-ffff-ffff-ffff-ffffffffffff\""));
        assertTrue(html.contains("href=\"#main/0/hunttech_IteractionList.edit?id=ffffffff-ffff-ffff-ffff-ffffffffffff\""));
        assertTrue(html.contains("<span class=\"llm-entity-icon\">📋</span>"));
        assertTrue(html.contains("Интервью от 05.09"));
    }

    @Test
    void testHrmCvLink() {
        String raw = "Последнее резюме: [Резюме Java Lead](hrm://cv/11111111-2222-3333-4444-555555555555)";
        String html = MarkdownRenderer.renderMarkdown(raw);

        assertTrue(html.contains("class=\"llm-md-link llm-hrm-entity-link\""));
        assertTrue(html.contains("data-entity=\"cv\""));
        assertTrue(html.contains("data-id=\"11111111-2222-3333-4444-555555555555\""));
        assertTrue(html.contains("href=\"#main/0/hunttech_CandidateCV.edit?id=11111111-2222-3333-4444-555555555555\""));
        assertTrue(html.contains("<span class=\"llm-entity-icon\">📄</span>"));
        assertTrue(html.contains("Резюме Java Lead"));
    }

    @Test
    void testHrmActionLink() {
        String raw = "Выберите действие: [Создать взаимодействие](hrm://action/create-interaction?candId=11111111-2222-3333-4444-555555555555)";
        String html = MarkdownRenderer.renderMarkdown(raw);

        assertTrue(html.contains("class=\"llm-md-link llm-hrm-action-link\""));
        assertTrue(html.contains("data-action-url=\"create-interaction?candId=11111111-2222-3333-4444-555555555555\""));
        assertTrue(html.contains("<span class=\"llm-entity-icon\">⚡</span>"));
        assertTrue(html.contains("Создать взаимодействие"));
    }

    @Test
    void testInvalidHrmActionLinkRejected() {
        String raw = "Подозрительное действие: [Выполнить скрипт](hrm://action/evil-action?cmd=dropDatabase)";
        String html = MarkdownRenderer.renderMarkdown(raw);

        assertFalse(html.contains("llm-hrm-action-link"));
        assertFalse(html.contains("data-action-url"));
        assertTrue(html.contains("Выполнить скрипт"));
    }

    @Test
    void testChatHistoryPaginationBanner() {
        List<LlmChatMessage> messages = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            LlmChatMessage msg = new LlmChatMessage();
            msg.setRole(i % 2 == 0 ? "ASSISTANT" : "USER");
            msg.setContent("Сообщение " + i);
            messages.add(msg);
        }

        // totalCount = 45, visibleCount = 20 -> remaining = 25
        String html = MarkdownRenderer.renderChatHistory(messages, null, 45, 20);

        assertTrue(html.contains("llm-chat-load-earlier-container"));
        assertTrue(html.contains("Загрузить предыдущие сообщения (ещё 25)"));
        assertTrue(html.contains("hunttechLoadEarlierMessages"));
        assertTrue(html.contains("Сообщение 1"));
        assertTrue(html.contains("Сообщение 20"));
    }

    @Test
    void testChatHistoryWithoutPaginationBannerWhenAllLoaded() {
        List<LlmChatMessage> messages = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            LlmChatMessage msg = new LlmChatMessage();
            msg.setRole("USER");
            msg.setContent("Вопрос " + i);
            messages.add(msg);
        }

        // totalCount = 15, visibleCount = 20 -> no banner
        String html = MarkdownRenderer.renderChatHistory(messages, null, 15, 20);

        assertFalse(html.contains("llm-chat-load-earlier-container"));
        assertTrue(html.contains("Вопрос 1"));
    }
}

