package com.company.itpearls.core;

import com.haulmont.cuba.core.app.FileStorageService;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.FileStorageException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Base64;
import java.util.Date;

@Service(TextManipulationService.NAME)
public class TextManipulationServiceBean implements TextManipulationService {

    @Inject
    private FileStorageService fileStorageService;
    @Inject
    private TextManipulationService textManipulationService;

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
    @Override
    public String getMailHTMLFooter() {
        return "</body>\n</html>";
    }
    @Override
    public String getMailHTMLHeader() {
        return new StringBuilder()
                .append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n")
                .append("<html>\n")
                .append("<head>\n")
                .append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" >\n")
                .append("<title></title>\n")
                .append("<style type=\"text/css\">\n")
                .append("</style>\n")
                .append("</head>\n")
                .append("<body>")
                .toString();
    }
    @Override
    public String getImage(FileDescriptor fd) {
        //UUID id = UuidProvider.fromString("f5fb2eef-bf8f-af1d-dfed-5b381001579f");
        byte[] image;

        if (fd != null) {
            if (fd.getCreateDate() == null) {
                fd.setCreateDate(new Date());
            }

            try {
                image = fileStorageService.loadFile(fd);
            } catch (FileStorageException e) {
                return "";
            }

            Base64.Encoder encoder = Base64.getEncoder();
            String encodedString = encoder.encodeToString(image);

            return new StringBuilder()
                    .append(textManipulationService.getMailHTMLHeader())
                    .append("\n<img src=\"data:image/")
                    .append(fd.getExtension())
                    .append(";base64, ")
                    .append(encodedString)
                    .append("\"")
                    .append(" width=\"220\" height=\"292\">\n")
                    .append(textManipulationService.getMailHTMLFooter())
                    .toString();
        } else
            return null;
    }
}