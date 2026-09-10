package com.company.hunttech.web.screens.llmchat;

import com.company.hunttech.entity.ai.LlmChatMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure-Java Markdown renderer that safely translates Markdown constructs into styled HTML
 * for display inside CUBA Platform / Vaadin labels with htmlEnabled="true".
 * Guarantees strict HTML escaping before rendering to prevent XSS.
 */
public class MarkdownRenderer {

    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("(?s)```([a-zA-Z0-9_-]*)\\r?\\n(.*?)(?:```|$)");
    private static final Pattern INLINE_CODE_PATTERN = Pattern.compile("`([^`]+)`");
    private static final Pattern BOLD_PATTERN = Pattern.compile("(\\*\\*|__)(.+?)\\1");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("(?<![*_])([*_])([^*_\\n]+?)\\1(?![*_])");
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^\\]]+)\\]\\(((?:https?://|hrm://|#)[^\\s\\)\"]+)\\)");
    private static final Pattern HEADER_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$");
    private static final Pattern BLOCKQUOTE_PATTERN = Pattern.compile("^>\\s?(.*)$");
    private static final Pattern UNORDERED_LIST_PATTERN = Pattern.compile("^[-*+]\\s+(.+)$");
    private static final Pattern ORDERED_LIST_PATTERN = Pattern.compile("^(\\d+)\\.\\s+(.+)$");
    private static final Pattern HORIZONTAL_RULE_PATTERN = Pattern.compile("^(?:---|_{3,}|\\*{3,})$");
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final Pattern INTERNAL_HASH_PATTERN = Pattern.compile("^#([a-zA-Z0-9_\\-\\./\\?=&]+)$");

    /**
     * Renders complete chat history and optional live streaming text into a safe HTML container.
     */
    public static String renderChatHistory(List<LlmChatMessage> messages, String liveText) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"llm-chat-messages-container\">");

        boolean hasMessages = messages != null && !messages.isEmpty();
        boolean hasLiveText = liveText != null && !liveText.trim().isEmpty();

        if (!hasMessages && !hasLiveText) {
            sb.append("<div class=\"llm-chat-empty-hint\">")
              .append("Задайте вопрос ИИ-ассистенту HRM. Ответы поддерживают списки, блоки кода, таблицы и форматирование.")
              .append("</div>");
            sb.append("</div>");
            return sb.toString();
        }

        if (hasMessages) {
            for (LlmChatMessage message : messages) {
                boolean isUser = "USER".equalsIgnoreCase(message.getRole());
                String author = isUser ? "Вы" : "ИИ";
                String roleClass = isUser ? "llm-chat-msg-user" : "llm-chat-msg-ai";
                String content = message.getContent() != null ? message.getContent() : "";

                sb.append("<div class=\"llm-chat-msg ").append(roleClass).append("\">");
                sb.append("<div class=\"llm-chat-msg-header\">");
                sb.append("<span class=\"llm-chat-msg-author\">").append(escapeHtml(author)).append("</span>");
                sb.append("</div>");
                sb.append("<div class=\"llm-chat-msg-body\">");
                sb.append(renderMarkdown(content));
                sb.append("</div>");
                sb.append("</div>");
            }
        }

        if (hasLiveText) {
            sb.append("<div class=\"llm-chat-msg llm-chat-msg-ai llm-chat-msg-live\">");
            sb.append("<div class=\"llm-chat-msg-header\">");
            sb.append("<span class=\"llm-chat-msg-author\">ИИ</span>");
            sb.append("<span class=\"llm-chat-live-badge\">Генерация...</span>");
            sb.append("</div>");
            sb.append("<div class=\"llm-chat-msg-body\">");
            sb.append(renderMarkdown(liveText));
            sb.append("<span class=\"llm-chat-cursor\"></span>");
            sb.append("</div>");
            sb.append("</div>");
        }

        sb.append("</div>");
        return sb.toString();
    }

    /**
     * Translates a markdown string into safe, styled HTML.
     */
    public static String renderMarkdown(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // First, preserve and isolate fenced code blocks to avoid unwanted formatting inside them
        List<String> codeBlocks = new ArrayList<>();
        Matcher codeBlockMatcher = CODE_BLOCK_PATTERN.matcher(text);
        StringBuffer placeholderBuffer = new StringBuffer();
        while (codeBlockMatcher.find()) {
            String lang = codeBlockMatcher.group(1);
            String codeContent = codeBlockMatcher.group(2);
            String safeCode = escapeHtml(codeContent != null ? codeContent : "");
            String langClass = (lang != null && !lang.trim().isEmpty())
                    ? " lang-" + escapeHtml(lang.trim().toLowerCase())
                    : "";
            String codeBlockHtml = "<pre class=\"llm-md-pre" + langClass + "\"><code class=\"llm-md-code\">"
                    + safeCode + "</code></pre>";
            codeBlocks.add(codeBlockHtml);
            codeBlockMatcher.appendReplacement(placeholderBuffer, "@@@LLMCODEBLOCK" + (codeBlocks.size() - 1) + "@@@");
        }
        codeBlockMatcher.appendTail(placeholderBuffer);
        String textWithoutCodeBlocks = placeholderBuffer.toString();

        // Process lines
        String[] lines = textWithoutCodeBlocks.split("\\r?\\n");
        StringBuilder out = new StringBuilder();

        boolean inUnorderedList = false;
        boolean inOrderedList = false;
        boolean inBlockquote = false;
        boolean inTable = false;
        List<String> tableRows = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            String rawLine = lines[i];
            String trimmed = rawLine.trim();

            // Check if line is part of a Markdown table: "| a | b |"
            if (isTableRow(trimmed)) {
                if (!inTable) {
                    // Close other open block structures
                    if (inUnorderedList) { out.append("</ul>\n"); inUnorderedList = false; }
                    if (inOrderedList) { out.append("</ol>\n"); inOrderedList = false; }
                    if (inBlockquote) { out.append("</blockquote>\n"); inBlockquote = false; }
                    inTable = true;
                    tableRows.clear();
                }
                tableRows.add(trimmed);
                continue;
            } else if (inTable) {
                out.append(renderTable(tableRows));
                inTable = false;
                tableRows.clear();
            }

            // Check if line is code block placeholder
            if (trimmed.startsWith("@@@LLMCODEBLOCK") && trimmed.endsWith("@@@")) {
                if (inUnorderedList) { out.append("</ul>\n"); inUnorderedList = false; }
                if (inOrderedList) { out.append("</ol>\n"); inOrderedList = false; }
                if (inBlockquote) { out.append("</blockquote>\n"); inBlockquote = false; }
                out.append(trimmed).append("\n");
                continue;
            }

            // Horizontal rule
            if (HORIZONTAL_RULE_PATTERN.matcher(trimmed).matches()) {
                if (inUnorderedList) { out.append("</ul>\n"); inUnorderedList = false; }
                if (inOrderedList) { out.append("</ol>\n"); inOrderedList = false; }
                if (inBlockquote) { out.append("</blockquote>\n"); inBlockquote = false; }
                out.append("<hr class=\"llm-md-hr\"/>\n");
                continue;
            }

            // Header
            Matcher headerMatcher = HEADER_PATTERN.matcher(trimmed);
            if (headerMatcher.matches()) {
                if (inUnorderedList) { out.append("</ul>\n"); inUnorderedList = false; }
                if (inOrderedList) { out.append("</ol>\n"); inOrderedList = false; }
                if (inBlockquote) { out.append("</blockquote>\n"); inBlockquote = false; }
                int level = Math.min(6, Math.max(3, headerMatcher.group(1).length() + 2)); // map h1->h3, h2->h4...
                String headerContent = renderInline(headerMatcher.group(2));
                out.append("<h").append(level).append(" class=\"llm-md-h").append(level).append("\">")
                   .append(headerContent).append("</h").append(level).append(">\n");
                continue;
            }

            // Blockquote
            Matcher quoteMatcher = BLOCKQUOTE_PATTERN.matcher(trimmed);
            if (quoteMatcher.matches()) {
                if (inUnorderedList) { out.append("</ul>\n"); inUnorderedList = false; }
                if (inOrderedList) { out.append("</ol>\n"); inOrderedList = false; }
                if (!inBlockquote) {
                    out.append("<blockquote class=\"llm-md-quote\">\n");
                    inBlockquote = true;
                }
                out.append(renderInline(quoteMatcher.group(1))).append("<br/>\n");
                continue;
            } else if (inBlockquote) {
                out.append("</blockquote>\n");
                inBlockquote = false;
            }

            // Unordered list
            Matcher ulMatcher = UNORDERED_LIST_PATTERN.matcher(trimmed);
            if (ulMatcher.matches()) {
                if (inOrderedList) { out.append("</ol>\n"); inOrderedList = false; }
                if (!inUnorderedList) {
                    out.append("<ul class=\"llm-md-ul\">\n");
                    inUnorderedList = true;
                }
                out.append("<li class=\"llm-md-li\">").append(renderInline(ulMatcher.group(1))).append("</li>\n");
                continue;
            } else if (inUnorderedList && trimmed.isEmpty()) {
                out.append("</ul>\n");
                inUnorderedList = false;
            }

            // Ordered list
            Matcher olMatcher = ORDERED_LIST_PATTERN.matcher(trimmed);
            if (olMatcher.matches()) {
                if (inUnorderedList) { out.append("</ul>\n"); inUnorderedList = false; }
                if (!inOrderedList) {
                    out.append("<ol class=\"llm-md-ol\">\n");
                    inOrderedList = true;
                }
                out.append("<li class=\"llm-md-li\">").append(renderInline(olMatcher.group(2))).append("</li>\n");
                continue;
            } else if (inOrderedList && trimmed.isEmpty()) {
                out.append("</ol>\n");
                inOrderedList = false;
            }

            // Paragraph / text line
            if (trimmed.isEmpty()) {
                if (inUnorderedList) { out.append("</ul>\n"); inUnorderedList = false; }
                if (inOrderedList) { out.append("</ol>\n"); inOrderedList = false; }
                out.append("<div class=\"llm-md-p-spacer\"></div>\n");
            } else {
                out.append("<p class=\"llm-md-p\">").append(renderInline(trimmed)).append("</p>\n");
            }
        }

        // Close any remaining structures
        if (inTable) {
            out.append(renderTable(tableRows));
        }
        if (inUnorderedList) {
            out.append("</ul>\n");
        }
        if (inOrderedList) {
            out.append("</ol>\n");
        }
        if (inBlockquote) {
            out.append("</blockquote>\n");
        }

        // Restore code blocks
        String result = out.toString();
        for (int i = 0; i < codeBlocks.size(); i++) {
            result = result.replace("@@@LLMCODEBLOCK" + i + "@@@", codeBlocks.get(i));
        }

        return result;
    }

    /**
     * Renders inline markdown: inline code, bold, italic, links.
     */
    private static String renderInline(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // 1. Isolate inline code
        List<String> inlineCodes = new ArrayList<>();
        Matcher inlineCodeMatcher = INLINE_CODE_PATTERN.matcher(text);
        StringBuffer inlineBuf = new StringBuffer();
        while (inlineCodeMatcher.find()) {
            String code = inlineCodeMatcher.group(1);
            String safeCode = "<code class=\"llm-md-code-inline\">" + escapeHtml(code) + "</code>";
            inlineCodes.add(safeCode);
            inlineCodeMatcher.appendReplacement(inlineBuf, "@@@LLMINLINECODE" + (inlineCodes.size() - 1) + "@@@");
        }
        inlineCodeMatcher.appendTail(inlineBuf);
        String escaped = escapeHtml(inlineBuf.toString());

        // 2. Bold: **text** or __text__
        escaped = BOLD_PATTERN.matcher(escaped).replaceAll("<strong class=\"llm-md-strong\">$2</strong>");

        // 3. Italic: *text* or _text_
        escaped = ITALIC_PATTERN.matcher(escaped).replaceAll("<em class=\"llm-md-em\">$2</em>");

        // 4. Links: [text](http://...)
        Matcher linkMatcher = LINK_PATTERN.matcher(escaped);
        StringBuffer linkBuf = new StringBuffer();
        while (linkMatcher.find()) {
            String label = linkMatcher.group(1);
            String url = linkMatcher.group(2);
            String replacement;
            if (url.startsWith("hrm://candidate/")) {
                String id = url.substring("hrm://candidate/".length()).trim();
                if (UUID_PATTERN.matcher(id).matches()) {
                    String cubaUrl = "#main/0/hunttech_JobCandidate.edit?id=" + escapeHtml(id);
                    replacement = "<a href=\"" + cubaUrl + "\" class=\"llm-md-link llm-hrm-entity-link\" title=\"Открыть кандидата в HRM\">"
                            + "👤 " + label + "</a>";
                } else {
                    replacement = label;
                }
            } else if (url.startsWith("hrm://vacancy/")) {
                String id = url.substring("hrm://vacancy/".length()).trim();
                if (UUID_PATTERN.matcher(id).matches()) {
                    String cubaUrl = "#main/0/hunttech_OpenPosition.edit?id=" + escapeHtml(id);
                    replacement = "<a href=\"" + cubaUrl + "\" class=\"llm-md-link llm-hrm-entity-link\" title=\"Открыть вакансию в HRM\">"
                            + "💼 " + label + "</a>";
                } else {
                    replacement = label;
                }
            } else if (url.startsWith("hrm://interaction/")) {
                String id = url.substring("hrm://interaction/".length()).trim();
                if (UUID_PATTERN.matcher(id).matches()) {
                    String cubaUrl = "#main/0/hunttech_IteractionList.edit?id=" + escapeHtml(id);
                    replacement = "<a href=\"" + cubaUrl + "\" class=\"llm-md-link llm-hrm-entity-link\" title=\"Открыть взаимодействие в HRM\">"
                            + "📋 " + label + "</a>";
                } else {
                    replacement = label;
                }
            } else if (url.startsWith("hrm://")) {
                replacement = label;
            } else if (url.startsWith("#")) {
                if (INTERNAL_HASH_PATTERN.matcher(url).matches()) {
                    replacement = "<a href=\"" + escapeHtml(url) + "\" class=\"llm-md-link\" title=\"Перейти\">"
                            + label + "</a>";
                } else {
                    replacement = label;
                }
            } else {
                replacement = "<a href=\"" + escapeHtml(url) + "\" target=\"_blank\" rel=\"noopener noreferrer\" class=\"llm-md-link\">"
                        + label + "</a>";
            }
            linkMatcher.appendReplacement(linkBuf, Matcher.quoteReplacement(replacement));
        }
        linkMatcher.appendTail(linkBuf);
        escaped = linkBuf.toString();

        // 5. Restore inline code
        for (int i = 0; i < inlineCodes.size(); i++) {
            escaped = escaped.replace("@@@LLMINLINECODE" + i + "@@@", inlineCodes.get(i));
        }

        return escaped;
    }

    private static boolean isTableRow(String line) {
        return line.startsWith("|") && line.endsWith("|") && line.length() > 2;
    }

    private static String renderTable(List<String> rows) {
        if (rows == null || rows.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"llm-md-table-wrap\"><table class=\"llm-md-table\">");

        boolean hasHeader = false;
        int dataStartIndex = 0;

        // Check if row 1 is a delimiter row: | --- | --- |
        if (rows.size() >= 2 && isTableDelimiterRow(rows.get(1))) {
            hasHeader = true;
            dataStartIndex = 2;
            sb.append("<thead><tr>");
            for (String cell : splitCells(rows.get(0))) {
                sb.append("<th class=\"llm-md-th\">").append(renderInline(cell)).append("</th>");
            }
            sb.append("</tr></thead>");
        }

        sb.append("<tbody>");
        for (int i = dataStartIndex; i < rows.size(); i++) {
            String row = rows.get(i);
            if (isTableDelimiterRow(row)) {
                continue;
            }
            sb.append("<tr>");
            for (String cell : splitCells(row)) {
                sb.append("<td class=\"llm-md-td\">").append(renderInline(cell)).append("</td>");
            }
            sb.append("</tr>");
        }
        sb.append("</tbody></table></div>\n");
        return sb.toString();
    }

    private static boolean isTableDelimiterRow(String row) {
        if (row == null) {
            return false;
        }
        return row.trim().matches("^\\|(\\s*:?-+:?\\s*\\|)+$");
    }

    private static List<String> splitCells(String row) {
        List<String> cells = new ArrayList<>();
        String[] parts = row.split("\\|");
        for (int i = 1; i < parts.length; i++) {
            if (i == parts.length - 1 && parts[i].trim().isEmpty() && row.endsWith("|")) {
                continue;
            }
            cells.add(parts[i].trim());
        }
        return cells;
    }

    public static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(Math.max(16, s.length()));
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&':
                    out.append("&amp;");
                    break;
                case '<':
                    out.append("&lt;");
                    break;
                case '>':
                    out.append("&gt;");
                    break;
                case '"':
                    out.append("&quot;");
                    break;
                case '\'':
                    out.append("&#39;");
                    break;
                default:
                    out.append(c);
            }
        }
        return out.toString();
    }
}
