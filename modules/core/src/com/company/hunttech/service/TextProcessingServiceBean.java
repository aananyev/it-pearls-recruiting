package com.company.hunttech.service;

import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Реализация сервиса умной текстовой обработки и типографического AI-форматирования текстов.
 */
@Service(TextProcessingService.NAME)
public class TextProcessingServiceBean implements TextProcessingService {

    private static final Logger log = LoggerFactory.getLogger(TextProcessingServiceBean.class);

    private static final Set<String> KNOWN_SECTION_HEADERS = new HashSet<>(Arrays.asList(
            "контакты", "контактная информация", "личная информация",
            "опыт работы", "опыт", "профессиональный опыт", "трудовая деятельность", "места работы",
            "навыки", "ключевые навыки", "профессиональные навыки", "стек технологий", "hard skills", "soft skills",
            "образование", "высшее образование", "дополнительное образование", "курсы", "сертификаты",
            "о себе", "обо мне", "дополнительные сведения", "дополнительная информация", "информация о себе",
            "языки", "знание языков", "иностранные языки",
            "проекты", "портфолио", "достижения",
            "contacts", "contact information", "personal info",
            "experience", "work experience", "professional experience", "employment history",
            "skills", "key skills", "technical skills", "tech stack",
            "education", "courses", "certificates", "certifications",
            "summary", "about me", "about", "additional info", "languages", "projects"
    ));

    private static final Pattern LIST_ITEM_PATTERN = Pattern.compile("^\\s*([•*\\-]\\s+|\\d+[.)]\\s+)(.*)$");

    @Inject
    private AiExecutionService aiExecutionService;

    @Override
    public String formatHtml(String rawText) {
        return formatHtmlWithResult(rawText).getText();
    }

    @Override
    public TextProcessingResult formatHtmlWithResult(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return TextProcessingResult.localResult("");
        }

        try {
            Map<String, Object> context = new LinkedHashMap<>();
            context.put(PARAM_SOURCE_TEXT, rawText.trim());
            context.put("callerSource", "TextProcessingService (formatHtml)");
            AiExecutionResult result = aiExecutionService.executeText(FUNCTION_TEXT_SMART_FORMAT_HTML, context);
            if (result != null && result.getText() != null && !result.getText().trim().isEmpty()) {
                String cleaned = cleanAiHtmlOutput(result.getText().trim());
                return TextProcessingResult.aiResult(cleaned, result);
            }
        } catch (RuntimeException e) {
            log.warn("AI-форматирование HTML недоступно, используется локальный типографический движок. Причина: {}", e.toString());
        }

        return TextProcessingResult.localResult(formatHtmlLocally(rawText));
    }

    @Override
    public String formatPlainText(String rawText) {
        return formatPlainTextWithResult(rawText).getText();
    }

    @Override
    public TextProcessingResult formatPlainTextWithResult(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return TextProcessingResult.localResult("");
        }

        try {
            Map<String, Object> context = new LinkedHashMap<>();
            context.put(PARAM_SOURCE_TEXT, rawText.trim());
            context.put("callerSource", "TextProcessingService (formatPlainText)");
            AiExecutionResult result = aiExecutionService.executeText(FUNCTION_TEXT_SMART_FORMAT_PLAIN, context);
            if (result != null && result.getText() != null && !result.getText().trim().isEmpty()) {
                String cleaned = cleanAiPlainOutput(result.getText().trim());
                return TextProcessingResult.aiResult(cleaned, result);
            }
        } catch (RuntimeException e) {
            log.warn("AI-форматирование Plain Text недоступно, используется локальный типографический движок. Причина: {}", e.toString());
        }

        return TextProcessingResult.localResult(formatPlainTextLocally(rawText));
    }

    /**
     * Локальный движок структурирования текста в чистый, читаемый HTML с аккуратной типографикой.
     */
    public String formatHtmlLocally(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return "";
        }

        String text = extractPlainTextLines(rawText);
        String[] lines = text.split("\\r?\\n");
        StringBuilder html = new StringBuilder();
        boolean inList = false;

        for (String rawLine : lines) {
            String line = rawLine != null ? rawLine.trim() : "";
            if (line.isEmpty()) {
                if (inList) {
                    html.append("</ul>\n");
                    inList = false;
                }
                continue;
            }

            if (isSectionHeader(line)) {
                if (inList) {
                    html.append("</ul>\n");
                    inList = false;
                }
                html.append("<div style=\"margin-top: 14px; margin-bottom: 6px; font-weight: bold; font-size: 14px; color: #1e3a8a; border-bottom: 1px solid #cbd5e1; padding-bottom: 3px;\">")
                        .append(escapeHtml(line.toUpperCase(Locale.getDefault())))
                        .append("</div>\n");
                continue;
            }

            java.util.regex.Matcher matcher = LIST_ITEM_PATTERN.matcher(line);
            if (matcher.matches()) {
                if (!inList) {
                    html.append("<ul style=\"margin: 4px 0 6px 20px; padding: 0;\">\n");
                    inList = true;
                }
                String itemContent = matcher.group(2).trim();
                html.append("<li style=\"margin-bottom: 3px; line-height: 1.45;\">")
                        .append(escapeHtml(itemContent))
                        .append("</li>\n");
            } else {
                if (inList) {
                    html.append("</ul>\n");
                    inList = false;
                }
                html.append("<p style=\"margin: 4px 0; line-height: 1.45;\">")
                        .append(escapeHtml(line))
                        .append("</p>\n");
            }
        }

        if (inList) {
            html.append("</ul>\n");
        }

        return html.toString().trim();
    }

    /**
     * Локальный движок структурирования текста в аккуратный Plain Text с понятными разделителями.
     */
    public String formatPlainTextLocally(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return "";
        }

        String text = extractPlainTextLines(rawText);
        String[] lines = text.split("\\r?\\n");
        List<String> output = new ArrayList<>();
        boolean prevWasBlank = false;

        for (String rawLine : lines) {
            String line = rawLine != null ? rawLine.trim() : "";
            if (line.isEmpty()) {
                if (!prevWasBlank && !output.isEmpty()) {
                    output.add("");
                    prevWasBlank = true;
                }
                continue;
            }

            if (isSectionHeader(line)) {
                if (!output.isEmpty() && !prevWasBlank) {
                    output.add("");
                }
                output.add("═══ " + line.toUpperCase(Locale.getDefault()) + " ═══");
                prevWasBlank = false;
                continue;
            }

            java.util.regex.Matcher matcher = LIST_ITEM_PATTERN.matcher(line);
            if (matcher.matches()) {
                output.add("  • " + matcher.group(2).trim());
            } else {
                output.add(line);
            }
            prevWasBlank = false;
        }

        return String.join("\n", output).trim();
    }

    private boolean isSectionHeader(String line) {
        if (line == null || line.length() < 3 || line.length() > 60) {
            return false;
        }
        String clean = line.replaceAll("^[#=*_\\-\\s:]+|[#=*_\\-\\s:]+$", "").toLowerCase(Locale.ROOT).trim();
        return KNOWN_SECTION_HEADERS.contains(clean);
    }

    private String cleanAiHtmlOutput(String aiResult) {
        String result = aiResult;
        if (result.startsWith("```html")) {
            result = result.substring(7);
        } else if (result.startsWith("```")) {
            result = result.substring(3);
        }
        if (result.endsWith("```")) {
            result = result.substring(0, result.length() - 3);
        }
        return result.trim();
    }

    private String cleanAiPlainOutput(String aiResult) {
        String result = aiResult;
        if (result.startsWith("```txt") || result.startsWith("```text")) {
            result = result.substring(result.indexOf('\n') + 1);
        } else if (result.startsWith("```")) {
            result = result.substring(3);
        }
        if (result.endsWith("```")) {
            result = result.substring(0, result.length() - 3);
        }
        return result.trim();
    }

    private String extractPlainTextLines(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return "";
        }
        String text = rawText
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n\n")
                .replaceAll("(?i)</div>", "\n")
                .replaceAll("(?i)</li>", "\n")
                .replaceAll("(?i)<tr[^>]*>", "\n")
                .replaceAll("(?i)<hr\\s*/?>", "\n");
        return text.replaceAll("<[^>]+>", "");
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
