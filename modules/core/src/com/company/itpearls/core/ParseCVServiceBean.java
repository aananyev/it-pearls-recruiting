package com.company.itpearls.core;

import com.company.itpearls.entity.Company;
import com.company.itpearls.entity.OpenPosition;
import com.company.itpearls.entity.Position;
import com.company.itpearls.entity.SkillTree;
import com.haulmont.cuba.core.global.DataManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service(ParseCVService.NAME)
public class ParseCVServiceBean implements ParseCVService {

    static String emailPtrn = "\\w+([\\.-]?\\w+)*@\\w+([\\.-]?\\w+)*\\.\\w{2,4}";
    static String phonePtrn = "[7|8][ (-]?[\\d]{3}[ )-]?[\\d]{3}[ -]?[\\d]{2}[ -]?[\\d]{2}[\\D]";
    @Inject
    private DataManager dataManager;
    @Inject
    private PdfParserService pdfParserService;

    @Override
    public String parseEmail(String cv) {
        if (cv != null) {
            Pattern emailPattern = Pattern.compile(emailPtrn);
            Matcher emailMatcher = emailPattern.matcher(Jsoup.parse(cv).text());

            String retStr = "";

            if (emailMatcher.find()) {
                retStr = emailMatcher.group();
            } else {
                retStr = null;
            }

            return retStr;
        } else {
            return null;
        }
    }

    @Override
    public String parsePhone(String cv) {
        return deleteSystemChar(getDataModel(cv, phonePtrn));
    }

    private String getDataModel(String onStr, String pattern) {
        if (onStr != null) {
            Pattern patt = Pattern.compile(pattern);
            Matcher matcher = patt.matcher(Jsoup.parse(onStr).text());

            if (matcher.find()) {
                return matcher.group();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public String normalizePhoneStr(String phone) {
        return deleteSystemChar(phone);
    }

    private String deleteSystemChar(String input) {
        String taboo = " ,.{}()[]-";
        String newStr = "";

        if (input != null) {
            for (int i = 0; i < input.length(); i++) {
                Boolean flagTaboo = true;

                for (char c : taboo.toCharArray()) {
                    if (input.charAt(i) == c) {
                        flagTaboo = false;
                    }
                }

                if (flagTaboo) {
                    newStr += input.charAt(i);
                }
            }

            return newStr;
        } else {
            return null;
        }
    }

    @Override
    public List<String> extractUrls(String input) {
        List<String> result = new ArrayList<String>();

        Pattern pattern = Pattern.compile(
                "\\b(((ht|f)tp(s?)\\:\\/\\/|~\\/|\\/)|www.)" +
                        "(\\w+:\\w+@)?(([-\\w]+\\.)+(com|org|net|gov" +
                        "|mil|biz|info|mobi|name|aero|jobs|museum" +
                        "|travel|[a-z]{2}))(:[\\d]{1,5})?" +
                        "(((\\/([-\\w~!$+|.,=]|%[a-f\\d]{2})+)+|\\/)+|\\?|#)?" +
                        "((\\?([-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?" +
                        "([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)" +
                        "(&(?:[-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?" +
                        "([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)*)*" +
                        "(#([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)?\\b");

        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            result.add(matcher.group());
        }

        return result;
    }

    private List<Company> parseCompaniesPriv(String textCV) {
        List<Company> companies = dataManager.load(Company.class).list();
        List<Company> retCompanies = new ArrayList<>();

        for (Company company : companies) {
            if (company != null) {
                if (company.getComanyName() != null) {
                    if (textCV.contains(company.getComanyName())) {
                        retCompanies.add(company);
                    }
                }
            }
        }

        return retCompanies;
    }

    private List<Position> parsePosition(String textCV) {
        List<Position> positions = dataManager.load(Position.class).list();
        List<Position> retPositions = new ArrayList<>();

        for (Position position : positions) {
            if (position != null) {
                if (position.getPositionEnName() != null) {
                    if (textCV.contains(position.getPositionEnName())) {
                        retPositions.add(position);
                    }
                }
            }
        }

        return retPositions;
    }

    @Override
    public List<Position> parsePositions(String textCV) {
        return parsePositions(textCV);
    }

    @Override
    public List<Company> parseCompanies(String textCV) {
        return parseCompaniesPriv(textCV);
    }


    @Override
    public Company parseCompany(String textCV) {
        List<Company> companies = dataManager.load(Company.class).list();
        Company retCompany = null;

        if(textCV != null) {
            for (Company company : companies) {
                if (company != null) {
                    if (company.getComanyName() != null) {
                        if (textCV.contains(company.getComanyName())) {
                            retCompany = company;
                            break;
                        }
                    }
                }
            }
        }

        return retCompany;
    }

    @Override
    public String colorHighlightingCompetencies(OpenPosition openPosition, String htmlText,
                                                String color, String colorIfExist) {
        String retStr = htmlText;

        List<SkillTree> skillTrees = pdfParserService.parseSkillTree(htmlText);
        List<SkillTree> skillTreesFromOpenPosition = pdfParserService.parseSkillTree(openPosition.getComment());

        for (SkillTree skillTree : skillTrees) {
            String keyWithStyle;

            if (skillContains(skillTreesFromOpenPosition, skillTree)) {
                keyWithStyle = "<b><font color=\""
                        + color
                        + "\" face=\"sans-serif\">"
                        + skillTree.getSkillName()
                        + "</font></b>";
            } else {
                keyWithStyle = "<b><font color=\""
                        + colorIfExist
                        + "\" face=\"sans-serif\">"
                        + skillTree.getSkillName()
                        + "</font></b>";
            }

            if (!retStr.contains(keyWithStyle)) {
                retStr = retStr.replaceAll("" + skillTree.getSkillName(), keyWithStyle);
            }
        }

        return retStr;
    }

    private boolean skillContains(List<SkillTree> skillTreesFromOpenPosition, SkillTree skillTree) {
        for (SkillTree st : skillTreesFromOpenPosition) {
            if (st.getSkillName().equals(skillTree.getSkillName())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String colorHighlightingCompetencies(String htmlText, String color) {
        String retStr = htmlText;

        List<SkillTree> skillTrees = pdfParserService.parseSkillTree(htmlText);

        for (SkillTree skillTree : skillTrees) {
            String keyWithStyle = "<b><font color=\""
                    + color
                    + "\">"
                    + skillTree.getSkillName()
                    + "</font></b>";
            if (!retStr.toLowerCase().contains(keyWithStyle.toLowerCase())) {
                retStr = retStr.replaceAll("" + skillTree.getSkillName(), keyWithStyle);
            }
        }

        return retStr;
    }

    @Override
    public String colorHighlingCompany(String htmlText, String color) {
        String retStr = htmlText;

        List<Company> companies = parseCompaniesPriv(htmlText);

        for (Company company : companies) {
            String keyWithStyle = "<b><font color=\""
                    + color
                    + "\">"
                    + company.getComanyName()
                    + "</font></b>";
            if (!retStr.contains(keyWithStyle)) {
                retStr = retStr.replaceAll("" + company.getComanyName(), keyWithStyle);
            }
        }

        return retStr;
    }

    @Override
    public String colorHighlingPositions(String htmlText, String color) {
        String retStr = htmlText;

        List<Position> positions = parsePositions(htmlText);

        for (Position position : positions) {
            String keyWithStyle = "<b><font color=\""
                    + color
                    + "\">"
                    + position.getPositionEnName()
                    + "</font></b>";
            if (!retStr.contains(keyWithStyle)) {
                retStr = retStr.replaceAll("" + position.getPositionEnName(), keyWithStyle);
            }
        }

        return retStr;
    }

    @Override
    public String br2nl(String html) {
        if (html == null)
            return html;

        Document document = Jsoup.parse(html);
        document.outputSettings(new Document.OutputSettings().prettyPrint(false));//makes html() preserve linebreaks and spacing
        document.select("br").append("\\n");
        document.select("p").prepend("\\n\\n");
        String s = document.html().replaceAll("\\\\n", "\n");

        return Jsoup.clean(s, "", Whitelist.none(), new Document.OutputSettings().prettyPrint(false));
    }

    @Override
    public String nl2br(String text) {
        if (text == null)
            return text;

        String s = text.replaceAll("\n", "<br>");

        return s;
    }
}