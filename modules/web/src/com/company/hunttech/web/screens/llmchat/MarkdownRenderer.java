package com.company.hunttech.web.screens.llmchat;

import com.company.hunttech.entity.ai.LlmChatMessage;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure-Java Markdown renderer that safely translates Markdown constructs into styled HTML
 * for display inside CUBA Platform / Vaadin labels with htmlEnabled="true".
 * Guarantees safe HTML rendering: safe tags (formatting, tables, lists, links) are formatted,
 * dangerous tags (scripts, iframes, on* handlers) are strictly sanitized/escaped to prevent XSS.
 */
public class MarkdownRenderer {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

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
    private static final Pattern CUBA_HASH_NAV_PATTERN = Pattern.compile(
            "^(?:https?://[^/\\s]+/hrm/)?#main/[0-9]+/([a-zA-Z0-9_\\.]+)(?:\\?(?:[^#\\s]*&)?id=([0-9a-fA-F\\-]+))?$"
    );
    private static final Pattern BARE_CUBA_URL_PATTERN = Pattern.compile(
            "(?<![\\(\"'])https?://[^/\\s]+/hrm/#main/[0-9]+/([a-zA-Z0-9_\\.]+)\\?id=([0-9a-fA-F\\-]+)(?![\\)\"'])"
    );
    private static final Pattern ACTION_PAYLOAD_PATTERN = Pattern.compile(
            "^(create-interaction\\?(?:candidateId|candId|candidateName)=[0-9a-zA-Zа-яА-ЯёЁ\\-_\\s\\.%+]+" +
            "|open-position\\?(?:number|num|id)=[0-9a-zA-Z\\-_\\^]+" +
            "|open-vacancy\\?(?:number|num|id)=[0-9a-zA-Z\\-_\\^]+" +
            "|open-cv\\?(?:id|candId|candidateName)=[0-9a-zA-Zа-яА-ЯёЁ\\-_\\s\\.%+]+" +
            "|open-browse\\?(?:role|pos)=[a-zA-Z0-9а-яА-ЯёЁ\\s\\-\\.\\+_%]+" +
            "|browse-vacancies\\?(?:role|pos)=[a-zA-Z0-9а-яА-ЯёЁ\\s\\-\\.\\+_%]+)$"
    );

    private static final Set<String> ALLOWED_HTML_TAGS = new HashSet<>(Arrays.asList(
            "b", "strong", "i", "em", "u", "s", "strike", "del", "ins", "mark", "small", "sub", "sup", "span", "font",
            "p", "br", "hr", "div", "blockquote", "h1", "h2", "h3", "h4", "h5", "h6",
            "ul", "ol", "li", "dl", "dt", "dd",
            "table", "thead", "tbody", "tfoot", "tr", "th", "td",
            "a", "code", "pre", "kbd", "samp"
    ));

    private static final Set<String> BLOCK_HTML_TAGS = new HashSet<>(Arrays.asList(
            "p", "hr", "div", "blockquote", "h1", "h2", "h3", "h4", "h5", "h6",
            "ul", "ol", "li", "dl", "dt", "dd",
            "table", "thead", "tbody", "tfoot", "tr", "th", "td", "pre"
    ));

    private static final Set<String> ALLOWED_HTML_ATTRS = new HashSet<>(Arrays.asList(
            "href", "title", "target", "class", "style", "color", "align", "valign",
            "border", "colspan", "rowspan", "width", "height", "data-entity", "data-id", "data-screen", "onclick"
    ));

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("(?i)<(/)?([a-zA-Z1-6]+)((?:\\s+[^<>]*)?)(\\s*/?)>");
    private static final Pattern HTML_ATTR_PATTERN = Pattern.compile("([a-zA-Z0-9_-]+)(?:\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+)))?");

    /**
     * Renders complete chat history and optional live streaming text into a safe HTML container.
     */
    public static String renderChatHistory(List<LlmChatMessage> messages, String liveText) {
        int count = messages != null ? messages.size() : 0;
        return renderChatHistory(messages, liveText, count, count);
    }

    /**
     * Renders chat history with pagination awareness (load earlier messages button).
     */
    public static String renderChatHistory(List<LlmChatMessage> messages, String liveText, int totalCount, int visibleCount) {
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

        if (totalCount > visibleCount) {
            int remaining = totalCount - visibleCount;
            sb.append("<div class=\"llm-chat-load-earlier-container\" style=\"text-align: center; margin: 4px 0 10px 0;\">")
              .append("<button type=\"button\" class=\"llm-chat-load-earlier-btn\" onclick=\"if(window.hunttechLoadEarlierMessages) window.hunttechLoadEarlierMessages();\" style=\"cursor: pointer; padding: 5px 14px; font-size: 12px; font-weight: 600; border-radius: 14px; border: 1px solid rgba(120, 140, 160, 0.35); background: rgba(120, 140, 160, 0.12); color: inherit;\">")
              .append("↑ Загрузить предыдущие сообщения (ещё ").append(remaining).append(")")
              .append("</button></div>");
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
                sb.append("<div class=\"llm-chat-msg-footer\">");
                sb.append("<span class=\"llm-chat-msg-time\" title=\"Штамп даты и времени\">")
                  .append(formatMessageTimestamp(message.getCreateTs()))
                  .append("</span>");
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
            sb.append("<div class=\"llm-chat-msg-footer\">");
            sb.append("<span class=\"llm-chat-msg-time\">")
              .append(formatMessageTimestamp(new Date()))
              .append("</span>");
            sb.append("</div>");
            sb.append("</div>");
        }

        sb.append("</div>");
        return sb.toString();
    }

    /**
     * Renders Hermes chat history and optional live status text.
     */
    public static String renderHermesChatHistory(List<com.company.hunttech.service.dto.HermesChatMessage> messages, String liveText, String emptyHint) {
        int count = messages != null ? messages.size() : 0;
        return renderHermesChatHistory(messages, liveText, emptyHint, count, count);
    }

    /**
     * Renders Hermes chat history with pagination awareness.
     */
    public static String renderHermesChatHistory(List<com.company.hunttech.service.dto.HermesChatMessage> messages, String liveText, String emptyHint, int totalCount, int visibleCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"llm-chat-messages-container\">");

        boolean hasMessages = messages != null && !messages.isEmpty();
        boolean hasLiveText = liveText != null && !liveText.trim().isEmpty();

        if (!hasMessages && !hasLiveText) {
            String hint = (emptyHint != null && !emptyHint.trim().isEmpty())
                    ? emptyHint
                    : "Задайте вопрос Hermes Agent (профиль hrm-viewer). Агент подключен к базе данных HRM в режиме чтения.";
            sb.append("<div class=\"llm-chat-empty-hint\">")
              .append(escapeHtml(hint))
              .append("</div>");
            sb.append("</div>");
            return sb.toString();
        }

        if (totalCount > visibleCount) {
            int remaining = totalCount - visibleCount;
            sb.append("<div class=\"llm-chat-load-earlier-container\" style=\"text-align: center; margin: 4px 0 10px 0;\">")
              .append("<button type=\"button\" class=\"llm-chat-load-earlier-btn\" onclick=\"if(window.hunttechLoadEarlierHermesMessages) window.hunttechLoadEarlierHermesMessages();\" style=\"cursor: pointer; padding: 5px 14px; font-size: 12px; font-weight: 600; border-radius: 14px; border: 1px solid rgba(120, 140, 160, 0.35); background: rgba(120, 140, 160, 0.12); color: inherit;\">")
              .append("↑ Загрузить предыдущие сообщения (ещё ").append(remaining).append(")")
              .append("</button></div>");
        }

        if (hasMessages) {
            for (com.company.hunttech.service.dto.HermesChatMessage message : messages) {
                boolean isUser = "user".equalsIgnoreCase(message.getRole());
                String author = isUser ? "Вы" : "Hermes Agent (hrm-viewer)";
                String roleClass = isUser ? "llm-chat-msg-user" : "llm-chat-msg-ai";
                String content = message.getContent() != null ? message.getContent() : "";

                sb.append("<div class=\"llm-chat-msg ").append(roleClass).append("\">");
                sb.append("<div class=\"llm-chat-msg-header\">");
                sb.append("<span class=\"llm-chat-msg-author\">").append(escapeHtml(author)).append("</span>");
                sb.append("</div>");
                sb.append("<div class=\"llm-chat-msg-body\">");
                sb.append(renderMarkdown(content));
                sb.append("</div>");
                sb.append("<div class=\"llm-chat-msg-footer\">");
                sb.append("<span class=\"llm-chat-msg-time\" title=\"Штамп даты и времени\">")
                  .append(formatMessageTimestamp(message.getCreateTs()))
                  .append("</span>");
                sb.append("</div>");
                sb.append("</div>");
            }
        }

        if (hasLiveText) {
            sb.append("<div class=\"llm-chat-msg llm-chat-msg-ai llm-chat-msg-live\">");
            sb.append("<div class=\"llm-chat-msg-header\">");
            sb.append("<span class=\"llm-chat-msg-author\">Hermes Agent (hrm-viewer)</span>");
            sb.append("<span class=\"llm-chat-live-badge\">Выполняется запрос...</span>");
            sb.append("</div>");
            sb.append("<div class=\"llm-chat-msg-body\">");
            sb.append(renderMarkdown(liveText));
            sb.append("<span class=\"llm-chat-cursor\"></span>");
            sb.append("</div>");
            sb.append("<div class=\"llm-chat-msg-footer\">");
            sb.append("<span class=\"llm-chat-msg-time\">")
              .append(formatMessageTimestamp(new Date()))
              .append("</span>");
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

        // Auto-link bare CUBA navigation URLs so they become clickable interactive entity cards
        Matcher bareUrlMatcher = BARE_CUBA_URL_PATTERN.matcher(text);
        StringBuffer bareUrlBuf = new StringBuffer();
        while (bareUrlMatcher.find()) {
            String fullUrl = bareUrlMatcher.group(0);
            String screen = bareUrlMatcher.group(1);
            String friendlyName = formatFriendlyScreenName(screen);
            bareUrlMatcher.appendReplacement(bareUrlBuf, Matcher.quoteReplacement("[" + friendlyName + "](" + fullUrl + ")"));
        }
        bareUrlMatcher.appendTail(bareUrlBuf);
        text = bareUrlBuf.toString();

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

        // Second, extract and sanitize safe HTML tags (formatting, tables, lists, links, spans)
        List<String> htmlTokens = new ArrayList<>();
        Matcher htmlTagMatcher = HTML_TAG_PATTERN.matcher(textWithoutCodeBlocks);
        StringBuffer htmlTokenBuf = new StringBuffer();
        while (htmlTagMatcher.find()) {
            boolean isClosing = "/".equals(htmlTagMatcher.group(1));
            String tagName = htmlTagMatcher.group(2);
            String rawAttrs = htmlTagMatcher.group(3);
            boolean selfClosing = "/".equals(htmlTagMatcher.group(4).trim());

            String sanitized = sanitizeHtmlTag(isClosing, tagName, rawAttrs, selfClosing);
            if (sanitized != null) {
                htmlTokens.add(sanitized);
                htmlTagMatcher.appendReplacement(htmlTokenBuf, "@@@LLMHTMLTOKEN" + (htmlTokens.size() - 1) + "@@@");
            } else {
                htmlTagMatcher.appendReplacement(htmlTokenBuf, Matcher.quoteReplacement(htmlTagMatcher.group(0)));
            }
        }
        htmlTagMatcher.appendTail(htmlTokenBuf);
        String textWithHtmlTokens = htmlTokenBuf.toString();

        // Process lines
        String[] lines = textWithHtmlTokens.split("\\r?\\n");
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
            } else if (isBlockHtmlLine(trimmed, htmlTokens)) {
                if (inUnorderedList) { out.append("</ul>\n"); inUnorderedList = false; }
                if (inOrderedList) { out.append("</ol>\n"); inOrderedList = false; }
                if (inBlockquote) { out.append("</blockquote>\n"); inBlockquote = false; }
                out.append(renderInline(trimmed)).append("\n");
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

        // Restore HTML tokens
        String result = out.toString();
        for (int i = 0; i < htmlTokens.size(); i++) {
            result = result.replace("@@@LLMHTMLTOKEN" + i + "@@@", htmlTokens.get(i));
        }

        // Restore code blocks
        for (int i = 0; i < codeBlocks.size(); i++) {
            result = result.replace("@@@LLMCODEBLOCK" + i + "@@@", codeBlocks.get(i));
        }

        return result;
    }

    private static boolean isBlockHtmlLine(String line, List<String> htmlTokens) {
        if (line == null || line.isEmpty()) return false;
        if (line.startsWith("@@@LLMHTMLTOKEN")) {
            int endIdx = line.indexOf("@@@", 15);
            if (endIdx > 15) {
                try {
                    int tokenIdx = Integer.parseInt(line.substring(15, endIdx));
                    if (tokenIdx >= 0 && tokenIdx < htmlTokens.size()) {
                        String rawToken = htmlTokens.get(tokenIdx).toLowerCase(Locale.ROOT);
                        for (String blockTag : BLOCK_HTML_TAGS) {
                            if (rawToken.startsWith("<" + blockTag) || rawToken.startsWith("</" + blockTag)) {
                                return true;
                            }
                        }
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return false;
    }

    private static String sanitizeHtmlTag(boolean isClosing, String tagName, String rawAttrs, boolean selfClosing) {
        String lowerTag = tagName.toLowerCase(Locale.ROOT);
        if (!ALLOWED_HTML_TAGS.contains(lowerTag)) {
            return null; // not in whitelist, will be safely escaped as raw text
        }

        if (isClosing) {
            return "</" + lowerTag + ">";
        }

        Map<String, String> attrs = new LinkedHashMap<>();
        if (rawAttrs != null && !rawAttrs.trim().isEmpty()) {
            Matcher attrMatcher = HTML_ATTR_PATTERN.matcher(rawAttrs);
            while (attrMatcher.find()) {
                String attrName = attrMatcher.group(1).toLowerCase(Locale.ROOT);
                String val = attrMatcher.group(2);
                if (val == null) val = attrMatcher.group(3);
                if (val == null) val = attrMatcher.group(4);
                if (val == null) val = "";

                if (attrName.startsWith("on")) {
                    continue; // block inline event handlers from user/LLM input
                }

                if (ALLOWED_HTML_ATTRS.contains(attrName)) {
                    if ("href".equals(attrName) || "src".equals(attrName)) {
                        String lowerVal = val.trim().toLowerCase(Locale.ROOT);
                        if (lowerVal.startsWith("javascript:") || lowerVal.startsWith("vbscript:") || lowerVal.startsWith("data:text/html")) {
                            continue;
                        }
                    }
                    if ("style".equals(attrName)) {
                        String lowerVal = val.toLowerCase(Locale.ROOT);
                        if (lowerVal.contains("expression(") || lowerVal.contains("javascript:") || lowerVal.contains("behavior:")) {
                            continue;
                        }
                    }
                    attrs.put(attrName, val);
                }
            }
        }

        // Auto-transform <a> tags pointing to HRM entities into interactive card links
        if ("a".equals(lowerTag)) {
            String href = attrs.get("href");
            if (href != null) {
                Matcher cubaNav = CUBA_HASH_NAV_PATTERN.matcher(href);
                if (cubaNav.matches()) {
                    String screen = cubaNav.group(1);
                    String id = cubaNav.group(2);
                    String entityType = mapScreenToEntityType(screen);
                    String safeId = id != null ? escapeHtml(id) : "";
                    attrs.put("class", "llm-md-link llm-hrm-entity-link");
                    attrs.put("data-entity", entityType);
                    attrs.put("data-id", safeId);
                    attrs.put("data-screen", screen);
                    attrs.put("title", "Открыть карточку в HRM");
                    attrs.put("onclick", "if(window.hunttechOpenHrmEntity){window.hunttechOpenHrmEntity('" + escapeHtml(entityType) + "','" + safeId + "');return false;}else if(window.parent&&window.parent.hunttechOpenHrmEntity){window.parent.hunttechOpenHrmEntity('" + escapeHtml(entityType) + "','" + safeId + "');return false;}");
                } else if (href.startsWith("hrm://")) {
                    String sub = href.substring("hrm://".length()).trim();
                    int slashIdx = sub.indexOf('/');
                    if (slashIdx > 0) {
                        String entity = sub.substring(0, slashIdx);
                        String id = sub.substring(slashIdx + 1).trim();
                        if (UUID_PATTERN.matcher(id).matches()) {
                            attrs.put("class", "llm-md-link llm-hrm-entity-link");
                            attrs.put("data-entity", entity);
                            attrs.put("data-id", id);
                            attrs.put("title", "Открыть карточку в HRM");
                            attrs.put("onclick", "if(window.hunttechOpenHrmEntity){window.hunttechOpenHrmEntity('" + escapeHtml(entity) + "','" + escapeHtml(id) + "');return false;}else if(window.parent&&window.parent.hunttechOpenHrmEntity){window.parent.hunttechOpenHrmEntity('" + escapeHtml(entity) + "','" + escapeHtml(id) + "');return false;}");
                        }
                    }
                }
            }
        }

        if ("table".equals(lowerTag) && !attrs.containsKey("class")) {
            attrs.put("class", "llm-md-table");
        } else if ("th".equals(lowerTag) && !attrs.containsKey("class")) {
            attrs.put("class", "llm-md-th");
        } else if ("td".equals(lowerTag) && !attrs.containsKey("class")) {
            attrs.put("class", "llm-md-td");
        } else if ("blockquote".equals(lowerTag) && !attrs.containsKey("class")) {
            attrs.put("class", "llm-md-quote");
        }

        StringBuilder out = new StringBuilder();
        out.append("<").append(lowerTag);
        for (Map.Entry<String, String> entry : attrs.entrySet()) {
            out.append(" ").append(entry.getKey()).append("=\"").append(escapeHtml(entry.getValue())).append("\"");
        }
        if (selfClosing || "br".equals(lowerTag) || "hr".equals(lowerTag)) {
            out.append("/>");
        } else {
            out.append(">");
        }
        return out.toString();
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
            Matcher cubaNavMatcher = CUBA_HASH_NAV_PATTERN.matcher(url);
            if (cubaNavMatcher.matches()) {
                String screen = cubaNavMatcher.group(1);
                String id = cubaNavMatcher.group(2);
                String entityType = mapScreenToEntityType(screen);
                String icon = getEntityIcon(entityType);
                String safeId = id != null ? escapeHtml(id) : "";
                String cubaUrl = "#main/0/" + escapeHtml(screen) + (id != null ? "?id=" + safeId : "");
                String clickJs = "if(window.hunttechOpenHrmEntity){window.hunttechOpenHrmEntity('" + escapeHtml(entityType) + "','" + safeId + "');return false;}else if(window.parent&&window.parent.hunttechOpenHrmEntity){window.parent.hunttechOpenHrmEntity('" + escapeHtml(entityType) + "','" + safeId + "');return false;}";
                replacement = "<a href=\"" + cubaUrl + "\" onclick=\"" + clickJs + "\" class=\"llm-md-link llm-hrm-entity-link\" data-entity=\"" + escapeHtml(entityType) + "\" data-screen=\"" + escapeHtml(screen) + "\" data-id=\"" + safeId + "\" title=\"Открыть карточку в HRM\">"
                        + "<span class=\"llm-entity-icon\">" + icon + "</span> " + label + "</a>";
            } else if (url.startsWith("hrm://candidate/")) {
                String id = url.substring("hrm://candidate/".length()).trim();
                if (UUID_PATTERN.matcher(id).matches()) {
                    String safeId = escapeHtml(id);
                    String cubaUrl = "#main/0/hunttech_JobCandidate.edit?id=" + safeId;
                    String clickJs = "if(window.hunttechOpenHrmEntity){window.hunttechOpenHrmEntity('candidate','" + safeId + "');return false;}else if(window.parent&&window.parent.hunttechOpenHrmEntity){window.parent.hunttechOpenHrmEntity('candidate','" + safeId + "');return false;}";
                    replacement = "<a href=\"" + cubaUrl + "\" onclick=\"" + clickJs + "\" class=\"llm-md-link llm-hrm-entity-link\" data-entity=\"candidate\" data-id=\"" + safeId + "\" title=\"Открыть карточку кандидата в HRM\">"
                            + "<span class=\"llm-entity-icon\">👤</span> " + label + "</a>";
                } else {
                    replacement = label;
                }
            } else if (url.startsWith("hrm://vacancy/")) {
                String id = url.substring("hrm://vacancy/".length()).trim();
                if (UUID_PATTERN.matcher(id).matches()) {
                    String safeId = escapeHtml(id);
                    String cubaUrl = "#main/0/hunttech_OpenPosition.edit?id=" + safeId;
                    String clickJs = "if(window.hunttechOpenHrmEntity){window.hunttechOpenHrmEntity('vacancy','" + safeId + "');return false;}else if(window.parent&&window.parent.hunttechOpenHrmEntity){window.parent.hunttechOpenHrmEntity('vacancy','" + safeId + "');return false;}";
                    replacement = "<a href=\"" + cubaUrl + "\" onclick=\"" + clickJs + "\" class=\"llm-md-link llm-hrm-entity-link\" data-entity=\"vacancy\" data-id=\"" + safeId + "\" title=\"Открыть карточку вакансии в HRM\">"
                            + "<span class=\"llm-entity-icon\">💼</span> " + label + "</a>";
                } else {
                    replacement = label;
                }
            } else if (url.startsWith("hrm://interaction/")) {
                String id = url.substring("hrm://interaction/".length()).trim();
                if (UUID_PATTERN.matcher(id).matches()) {
                    String safeId = escapeHtml(id);
                    String cubaUrl = "#main/0/hunttech_IteractionList.edit?id=" + safeId;
                    String clickJs = "if(window.hunttechOpenHrmEntity){window.hunttechOpenHrmEntity('interaction','" + safeId + "');return false;}else if(window.parent&&window.parent.hunttechOpenHrmEntity){window.parent.hunttechOpenHrmEntity('interaction','" + safeId + "');return false;}";
                    replacement = "<a href=\"" + cubaUrl + "\" onclick=\"" + clickJs + "\" class=\"llm-md-link llm-hrm-entity-link\" data-entity=\"interaction\" data-id=\"" + safeId + "\" title=\"Открыть карточку взаимодействия в HRM\">"
                            + "<span class=\"llm-entity-icon\">📋</span> " + label + "</a>";
                } else {
                    replacement = label;
                }
            } else if (url.startsWith("hrm://cv/")) {
                String id = url.substring("hrm://cv/".length()).trim();
                if (UUID_PATTERN.matcher(id).matches()) {
                    String safeId = escapeHtml(id);
                    String cubaUrl = "#main/0/hunttech_CandidateCV.edit?id=" + safeId;
                    String clickJs = "if(window.hunttechOpenHrmEntity){window.hunttechOpenHrmEntity('cv','" + safeId + "');return false;}else if(window.parent&&window.parent.hunttechOpenHrmEntity){window.parent.hunttechOpenHrmEntity('cv','" + safeId + "');return false;}";
                    replacement = "<a href=\"" + cubaUrl + "\" onclick=\"" + clickJs + "\" class=\"llm-md-link llm-hrm-entity-link\" data-entity=\"cv\" data-id=\"" + safeId + "\" title=\"Открыть резюме кандидата в HRM\">"
                            + "<span class=\"llm-entity-icon\">📄</span> " + label + "</a>";
                } else {
                    replacement = label;
                }
            } else if (url.startsWith("hrm://action/")) {
                String actionData = url.substring("hrm://action/".length()).trim();
                if (ACTION_PAYLOAD_PATTERN.matcher(actionData).matches()) {
                    replacement = "<a href=\"#action\" class=\"llm-md-link llm-hrm-action-link\" data-action-url=\"" + escapeHtml(actionData) + "\" title=\"Выполнить действие в HRM\">"
                            + "<span class=\"llm-entity-icon\">⚡</span> " + label + "</a>";
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

    public static String formatMessageTimestamp(Date date) {
        if (date == null) {
            date = new Date();
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).format(TIME_FORMATTER);
    }

    public static String formatFriendlyScreenName(String screen) {
        if (screen == null) return "Открыть карточку в HRM";
        String s = screen.toLowerCase(Locale.ROOT);
        if (s.contains("openposition")) return "Открыть карточку вакансии в HRM";
        if (s.contains("jobcandidate")) return "Открыть карточку кандидата в HRM";
        if (s.contains("candidatecv")) return "Открыть резюме кандидата в HRM";
        if (s.contains("iteractionlist")) return "Открыть взаимодействие в HRM";
        if (s.contains("company")) return "Открыть компанию в HRM";
        return "Открыть карточку в HRM";
    }

    public static String mapScreenToEntityType(String screen) {
        if (screen == null) return "entity";
        String s = screen.toLowerCase(Locale.ROOT);
        if (s.contains("openposition")) return "vacancy";
        if (s.contains("jobcandidate")) return "candidate";
        if (s.contains("candidatecv")) return "cv";
        if (s.contains("iteractionlist")) return "interaction";
        if (s.contains("company")) return "company";
        if (s.contains("extuser")) return "user";
        return screen;
    }

    public static String getEntityIcon(String entityType) {
        if (entityType == null) return "🔗";
        switch (entityType.toLowerCase(Locale.ROOT)) {
            case "vacancy": return "💼";
            case "candidate": return "👤";
            case "cv": return "📄";
            case "interaction": return "📋";
            case "company": return "🏢";
            case "user": return "⚙️";
            default: return "🔗";
        }
    }
}
