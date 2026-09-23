package com.company.hunttech.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Утилитный класс безопасного преобразования Markdown-разметки (таблицы, списки,
 * заголовки, inline-форматирование) в семантический HTML для сохранения в LOB-поля сущностей
 * и отображения в RichTextArea/HtmlBox экранов HRM HuntTech.
 */
public final class MarkdownToHtmlUtils {

    private static final Pattern SCRIPT_TAG_PATTERN = Pattern.compile("(?i)<script[^>]*>[\\s\\S]*?</script>");
    private static final Pattern STYLE_TAG_PATTERN = Pattern.compile("(?i)<style[^>]*>[\\s\\S]*?</style>");
    private static final Pattern DANGEROUS_TAG_SINGLE_PATTERN = Pattern.compile("(?i)<\\s*/?\\s*(script|iframe|object|embed|style|link|meta|applet|base)[^>]*>");
    private static final Pattern DANGEROUS_URI_PATTERN = Pattern.compile("(?i)(href|src)\\s*=\\s*[\"']?\\s*(javascript:|vbscript:|data:text/html)", Pattern.DOTALL);
    private static final Pattern ON_EVENT_HANDLER_PATTERN = Pattern.compile("(?i)\\s+on[a-z0-9_-]+\\s*(=|(?=[\\s>]))[^\\s>]*");

    private static final Pattern MD_DETECT_HEADER_PATTERN = Pattern.compile("(?m)^\\s*#{1,6}\\s+");
    private static final Pattern MD_DETECT_LIST_PATTERN = Pattern.compile("(?m)^(\\s*[-*•]\\s+|\\s*\\d+\\.\\s+)");
    private static final Pattern HTML_TAG_DETECT_PATTERN = Pattern.compile("(?i)<(table|p|ul|ol|li|h[1-6]|div|br|strong|b|em|i|pre|code|span)[^>]*>");

    private static final Pattern MD_HEADER_LINE_PATTERN = Pattern.compile("^\\s*(#{1,6})\\s+(.*)$");
    private static final Pattern MD_UL_LINE_PATTERN = Pattern.compile("^\\s*[-*•]\\s+(.*)$");
    private static final Pattern MD_OL_LINE_PATTERN = Pattern.compile("^\\s*\\d+\\.\\s+(.*)$");
    private static final Pattern MD_TABLE_SPLIT_PATTERN = Pattern.compile("^\\|?\\s*[-:]+[-| :]*\\|?$");

    private MarkdownToHtmlUtils() {
    }

    /**
     * Основная точка входа: трансформирует markdown в валидный HTML с санитизацией.
     */
    public static String toHtml(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }
        String trimmed = sanitizeDangerousHtml(input.trim());

        // Проверяем наличие markdown-структур: таблицы, заголовки, списки
        boolean hasMarkdownTables = trimmed.contains("|") && (trimmed.contains("|---") || trimmed.contains("| ---") || trimmed.contains("|-"));
        boolean hasMarkdownHeaders = MD_DETECT_HEADER_PATTERN.matcher(trimmed).find();
        boolean hasMarkdownLists = MD_DETECT_LIST_PATTERN.matcher(trimmed).find();

        if (hasMarkdownTables || hasMarkdownHeaders || hasMarkdownLists) {
            return convertMarkdownToHtml(trimmed);
        }

        // Если это уже HTML с тегами и без markdown, санитизируем и возвращаем
        boolean hasHtmlTags = HTML_TAG_DETECT_PATTERN.matcher(trimmed).find();
        if (hasHtmlTags) {
            return trimmed;
        }

        // Обычный плоский текст форматируем абзацами
        return convertMarkdownToHtml(trimmed);
    }

    public static String sanitizeDangerousHtml(String html) {
        if (html == null) return "";
        String sanitized = SCRIPT_TAG_PATTERN.matcher(html).replaceAll("");
        sanitized = STYLE_TAG_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = DANGEROUS_TAG_SINGLE_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = DANGEROUS_URI_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = ON_EVENT_HANDLER_PATTERN.matcher(sanitized).replaceAll("");
        return sanitized;
    }

    private static String convertMarkdownToHtml(String md) {
        if (md == null || md.trim().isEmpty()) return "";
        String[] lines = md.replace("\r", "").split("\n");
        StringBuilder html = new StringBuilder();

        boolean inUl = false;
        boolean inOl = false;
        boolean inTable = false;
        boolean inPre = false;
        List<String> tableRows = new ArrayList<>();

        for (String line : lines) {
            String trimmed = line.trim();

            // 1. Блоки кода (```)
            if (trimmed.startsWith("```")) {
                if (inPre) {
                    html.append("</code></pre>\n");
                    inPre = false;
                } else {
                    if (inUl) { html.append("</ul>\n"); inUl = false; }
                    if (inOl) { html.append("</ol>\n"); inOl = false; }
                    html.append("<pre style=\"background-color: #f8f9fa; border: 1px solid #e9ecef; border-radius: 4px; padding: 10px; overflow-x: auto;\"><code>");
                    inPre = true;
                }
                continue;
            }
            if (inPre) {
                html.append(escapeHtml(line)).append("\n");
                continue;
            }

            // 2. Таблицы Markdown (| col1 | col2 |)
            if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
                if (inUl) { html.append("</ul>\n"); inUl = false; }
                if (inOl) { html.append("</ol>\n"); inOl = false; }
                tableRows.add(trimmed);
                inTable = true;
                continue;
            } else if (inTable) {
                html.append(renderMarkdownTable(tableRows));
                tableRows.clear();
                inTable = false;
            }

            // 3. Заголовки (###, ####, ##, #)
            Matcher hMat = MD_HEADER_LINE_PATTERN.matcher(trimmed);
            if (hMat.matches()) {
                if (inUl) { html.append("</ul>\n"); inUl = false; }
                if (inOl) { html.append("</ol>\n"); inOl = false; }
                int level = Math.min(Math.max(hMat.group(1).length(), 2), 5); // h2 - h5
                String title = formatInlineMarkdown(hMat.group(2).trim());
                html.append("<h").append(level).append(" style=\"margin-top: 14px; margin-bottom: 6px; color: #212529;\">")
                        .append(title).append("</h").append(level).append(">\n");
                continue;
            }

            // 4. Маркированные списки (- item, * item, • item)
            Matcher ulMat = MD_UL_LINE_PATTERN.matcher(trimmed);
            if (ulMat.matches()) {
                if (inOl) { html.append("</ol>\n"); inOl = false; }
                if (!inUl) {
                    html.append("<ul style=\"margin-top: 4px; margin-bottom: 8px; padding-left: 20px;\">\n");
                    inUl = true;
                }
                String itemContent = formatInlineMarkdown(ulMat.group(1).trim());
                html.append("  <li>").append(itemContent).append("</li>\n");
                continue;
            }

            // 5. Нумерованные списки (1. item, 2. item)
            Matcher olMat = MD_OL_LINE_PATTERN.matcher(trimmed);
            if (olMat.matches()) {
                if (inUl) { html.append("</ul>\n"); inUl = false; }
                if (!inOl) {
                    html.append("<ol style=\"margin-top: 4px; margin-bottom: 8px; padding-left: 20px;\">\n");
                    inOl = true;
                }
                String itemContent = formatInlineMarkdown(olMat.group(1).trim());
                html.append("  <li>").append(itemContent).append("</li>\n");
                continue;
            }

            // Выход из списков при обычном контенте
            if (inUl) { html.append("</ul>\n"); inUl = false; }
            if (inOl) { html.append("</ol>\n"); inOl = false; }

            // 6. Пустая строка
            if (trimmed.isEmpty()) {
                html.append("<br/>\n");
                continue;
            }

            // 7. Обычный абзац или уже готовый HTML-тег
            if (trimmed.startsWith("<") && trimmed.endsWith(">")) {
                html.append(trimmed).append("\n");
            } else {
                html.append("<p style=\"margin: 4px 0;\">").append(formatInlineMarkdown(trimmed)).append("</p>\n");
            }
        }

        if (inPre) {
            html.append("</code></pre>\n");
        }
        if (inTable) {
            html.append(renderMarkdownTable(tableRows));
        }
        if (inUl) {
            html.append("</ul>\n");
        }
        if (inOl) {
            html.append("</ol>\n");
        }

        return html.toString().trim();
    }

    private static String renderMarkdownTable(List<String> rows) {
        if (rows == null || rows.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("<table border=\"1\" cellpadding=\"6\" cellspacing=\"0\" style=\"border-collapse: collapse; width: 100%; margin: 10px 0; border: 1px solid #dee2e6;\">\n");

        boolean hasHeader = false;
        int headerIndex = -1;

        for (int i = 0; i < rows.size(); i++) {
            String r = rows.get(i);
            if (MD_TABLE_SPLIT_PATTERN.matcher(r).matches() && r.contains("-")) {
                headerIndex = i;
                hasHeader = (i > 0);
                break;
            }
        }

        if (hasHeader) {
            sb.append("  <thead style=\"background-color: #f1f3f5; font-weight: bold;\">\n");
            for (int i = 0; i < headerIndex; i++) {
                sb.append("    <tr>\n");
                String[] cols = parseTableRow(rows.get(i));
                for (String c : cols) {
                    sb.append("      <th style=\"border: 1px solid #dee2e6; padding: 8px; text-align: left;\">")
                            .append(formatInlineMarkdown(c)).append("</th>\n");
                }
                sb.append("    </tr>\n");
            }
            sb.append("  </thead>\n");
            sb.append("  <tbody>\n");
            for (int i = headerIndex + 1; i < rows.size(); i++) {
                sb.append("    <tr>\n");
                String[] cols = parseTableRow(rows.get(i));
                for (String c : cols) {
                    sb.append("      <td style=\"border: 1px solid #dee2e6; padding: 8px;\">")
                            .append(formatInlineMarkdown(c)).append("</td>\n");
                }
                sb.append("    </tr>\n");
            }
            sb.append("  </tbody>\n");
        } else {
            sb.append("  <tbody>\n");
            for (String row : rows) {
                sb.append("    <tr>\n");
                String[] cols = parseTableRow(row);
                for (String c : cols) {
                    sb.append("      <td style=\"border: 1px solid #dee2e6; padding: 8px;\">")
                            .append(formatInlineMarkdown(c)).append("</td>\n");
                }
                sb.append("    </tr>\n");
            }
            sb.append("  </tbody>\n");
        }

        sb.append("</table>\n");
        return sb.toString();
    }

    private static String[] parseTableRow(String row) {
        String s = row.trim();
        if (s.startsWith("|")) s = s.substring(1);
        if (s.endsWith("|")) s = s.substring(0, s.length() - 1);
        String[] parts = s.split("(?<!\\\\)\\|", -1);
        String[] clean = new String[parts.length];
        for (int i = 0; i < parts.length; i++) {
            clean[i] = parts[i].replace("\\|", "|").trim();
        }
        return clean;
    }

    private static String formatInlineMarkdown(String text) {
        if (text == null) return "";
        String s = escapeHtml(text);
        // Код `code`
        s = s.replaceAll("`([^`]+)`", "<code style=\"background-color: #f1f3f5; padding: 2px 4px; border-radius: 3px; font-family: monospace;\">$1</code>");
        // Жирный **bold**
        s = s.replaceAll("\\*\\*([^*]+)\\*\\*", "<b>$1</b>");
        // Курсив *italic*
        s = s.replaceAll("(?<!\\*)\\*([^*]+)\\*(?!\\*)", "<i>$1</i>");
        return s;
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
