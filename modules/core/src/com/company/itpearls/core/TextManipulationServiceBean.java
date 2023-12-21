package com.company.itpearls.core;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

@Service(TextManipulationService.NAME)
public class TextManipulationServiceBean implements TextManipulationService {

    @Override
    public String formattedHtml2text(String inputHtml) {
        return String.valueOf(Jsoup.parse(inputHtml.replace("<br>", "\n")
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
        final String indentation = createIndentation(depth); // create indentation
        StringBuilder sb = new StringBuilder();

        final String typeAttr = root.attr("type"); // Get the character used as bullet (= 'type' attribute)
        char type = typeAttr.isEmpty() ? '1' : typeAttr.charAt(0); // if 'type' attribute: use it, else: use '1' instead

        for (Element sub : root.children()) // Iterate over all Childs
        {
            // If Java < 7: use if/else if/else here
            switch (sub.tagName()) // Check if the element is an item or a sublist
            {
                case "li": // Listitem, format and append
                    sb.append(indentation).append(type++).append(". ").append(sub.ownText()).append("\n");
                    break;
                case "ol": // Sublist
                case "ul":
                    if (!sub.children().isEmpty()) // If sublist is not empty (contains furhter items)
                    {
                        sb.append(createList(sub, depth + 1)); // Recursive call for the sublist
                    }
                    break;
                default: // "Illegal" tag, do furhter processing if required - output as an example here
                    System.err.println("Not implemented tag: " + sub.tagName());
            }
        }

        return sb.toString(); // Return the formated List
    }

}