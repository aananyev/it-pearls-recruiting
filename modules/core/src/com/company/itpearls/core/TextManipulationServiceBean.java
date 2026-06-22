package com.company.itpearls.core;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

@Service(TextManipulationService.NAME)
public class TextManipulationServiceBean implements TextManipulationService {

    @Override
    public String formattedHtml2text(String inputHtml) {
        return String.valueOf(Jsoup.parse(inputHtml
                        .replace("<br>", "\n")
                        .replace("<ol>", "<ul>")
                        .replace("</ol>", "</ul>")
                        .replace("<li>", "- ")
                        .replace("</li>", "\n"))
                .body().wholeText());
    }

    /**
     * Create an Indentationstring of <code>length</code> blanks.
     *
     * @param length Size of indentation
     * @return Indentationstring
     */
    private String createIndentation(int length) {
        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            sb.append(' ');
        }

        return sb.toString();
    }

    public String createList(Element root, int depth) {
        final String indentation = createIndentation(depth); // создать отступ
        StringBuilder sb = new StringBuilder();

        final String typeAttr = root.attr("type"); // Get the character used as bullet (= 'type' attribute)
        char type = typeAttr.isEmpty() ? '1' : typeAttr.charAt(0); // if 'type' attribute: use it, else: use '1' instead

        for (Element sub : root.children()) // Перебор всех дочерних элементов
        {
            // Для Java < 7: использовать if/else if/else
            switch (sub.tagName()) // Проверка: элемент списка или вложенный список
            {
                case "li": // Элемент списка: форматирование и добавление
                    sb.append(indentation).append(type++).append(". ").append(sub.ownText()).append("\n");
                    break;
                case "ol": // Вложенный список
                case "ul":
                    if (!sub.children().isEmpty()) // If sublist is not empty (contains furhter items)
                    {
                        sb.append(createList(sub, depth + 1)); // Рекурсивный вызов для вложенного списка
                    }
                    break;
                default: // "Illegal" tag, do furhter processing if required - output as an example here
                    System.err.println("Нереализованный тег: " + sub.tagName());
            }
        }

        return sb.toString(); // Вернуть отформатированный список
    }

}