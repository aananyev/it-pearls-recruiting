package com.company.itpearls.core;

import com.company.itpearls.entity.*;
import com.haulmont.cuba.core.global.DataManager;
import com.mdimension.jchronic.Chronic;
import com.mdimension.jchronic.Options;
import com.mdimension.jchronic.tags.Pointer;
import com.mdimension.jchronic.utils.Span;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.context.i18n.LocaleContextHolder.getTimeZone;

@Service(ParseCVService.NAME)
public class ParseCVServiceBean implements ParseCVService {

    static String emailPtrn = "/(\\+7|8)[- _]*\\(?[- _]*(\\d{3}[- _]*\\)?([- _]*\\d){7}|\\d\\d[- _]*\\d\\d[- _]*\\)?([- _]*\\d){6})/g\n";
    static String allCountryRegex =
            "^(\\+\\d{1,3}( )?)?((\\(\\d{1,3}\\))|\\d{1,3})[- .]?\\d{3,4}[- .]?\\d{4}$";
    static String phonePtrn = "[7|8][ (-]?[\\d]{3}[ )-]?[\\d]{3}[ -]?[\\d]{2}[ -]?[\\d]{2}[\\D]";
    static final Integer MIN_NAME_LENGTH = 3;
    static final String QUERY_GET_FIRST_NAMES =
            "select distinct e.firstName " +
                    "from itpearls_JobCandidate e " +
                    "where not e.firstName like ''" +
                    "order by e.firstName";
    static final String QUERY_GET_SECOND_NAMES =
            "select distinct e.secondName " +
                    "from itpearls_JobCandidate e " +
                    "where not e.secondName like ''" +
                    "order by e.secondName";
    static final String QUERY_GET_MIDDLE_NAMES =
            "select distinct e.middleName " +
                    "from itpearls_JobCandidate e " +
                    "where not e.middleName like ''" +
                    "order by e.middleName";

    @Inject
    private DataManager dataManager;
    @Inject
    private PdfParserService pdfParserService;

    @Override
    public Date parseDate(String cv) {
        return getDateFromText(new StringBuffer(cv));
    }

    @Override
    public Date parseDate(StringBuffer cv) {
        return getDateFromText(cv);
    }

    public Calendar detectDate(String input) {
        Options opt = new Options(Pointer.PointerType.PAST);
        opt.setNow(Calendar.getInstance(getTimeZone()));
        Span date = Chronic.parse(input, opt);

        if (date == null) {
            return null;
        } else {
            return date.getBeginCalendar();
        }
    }

    private Date getDateFromText(StringBuffer text) {
        Calendar retCalendar = detectDate(text.toString());
        return retCalendar != null ? retCalendar.getTime() : null;
    }


    @Override
    public List<String> getMiddleNameList(String cv) {

        List<String> middleNameList = dataManager.loadValue(QUERY_GET_MIDDLE_NAMES, String.class)
                .list();

        List<String> retStrList = getListName(middleNameList, cv);

        return retStrList;
    }

    @Override
    public List<String> getSecondNameList(String cv) {

        List<String> secondNameList = dataManager.loadValue(QUERY_GET_SECOND_NAMES, String.class)
                .list();

        List<String> retStrList = getListName(secondNameList, cv);

        return retStrList;
    }

    @Override
    public List<String> getFirstNameList(String cv) {

        List<String> firstNameList = dataManager.loadValue(QUERY_GET_FIRST_NAMES, String.class)
                .list();

        List<String> retStrList = getListName(firstNameList, cv);

        return retStrList;
    }

    @Override
    public List<String> getListName(List<String> firstNameList, String cv) {
        List<String> retStrList = new ArrayList<>();

        for (String fn : firstNameList) {
            if (!fn.equals("")) {
                if (fn.length() >= MIN_NAME_LENGTH) {
                    if (!fn.equals("")) {
                        StringBuffer cvLoverCase = new StringBuffer(Jsoup.parse(cv).text().toLowerCase());

                        if (fn.length() >= MIN_NAME_LENGTH) {
                            // с начала строки и до пробела
                            if (cvLoverCase.toString().contains("\n" + fn.toLowerCase() + " ")) {
                                break;
                            }
                            // с пробела и до пробела
                            if (cvLoverCase.toString().contains(" " + fn.toLowerCase() + " ")) {
                                retStrList.add(fn);
                                break;
                            }
                            // с пробела и до конца строки
                            if (cvLoverCase.toString().contains(" " + fn.toLowerCase() + "\n")) {
                                retStrList.add(fn);
                                break;
                            }
                        }
                    }
                }
            }
        }

        return retStrList;
    }

    @Override
    public String parseFirstName(String cv) {

        List<String> firstName = dataManager.loadValue(QUERY_GET_FIRST_NAMES, String.class)
                .list();
        StringBuffer retSb = new StringBuffer(getNamesFromText(firstName, cv));

        return retSb.equals("") ? null : retSb.toString();
    }

    @Override
    public String parseMiddleName(String cv) {

        List<String> middleName = dataManager.loadValue(QUERY_GET_MIDDLE_NAMES, String.class)
                .list();

        StringBuffer retSb = new StringBuffer(getNamesFromText(middleName, cv));

        return retSb.equals(null) ? null : retSb.toString();
    }

    @Override
    public String parseSecondName(String cv) {

        List<String> secondName = dataManager
                .loadValue(QUERY_GET_SECOND_NAMES, String.class)
                .list();

        StringBuffer retSb = new StringBuffer(getNamesFromText(secondName, cv));


        return retSb.equals(null) ? null : retSb.toString();
    }

    @Override
    public StringBuffer getNamesFromText(List<String> secondName, String cv) {
        StringBuffer retSb = new StringBuffer();

        for (String fn : secondName) {
            if (!fn.equals("")) {
                StringBuffer cvLoverCase = new StringBuffer(Jsoup.parse(cv).text().toLowerCase());

                if (fn.length() >= MIN_NAME_LENGTH) {
                    // с начала строки и до пробела
                    if (cvLoverCase.toString().contains("\n" + fn.toLowerCase() + " ")) {
                        retSb.append(fn);
                        break;
                    }
                    // с пробела и до пробела
                    if (cvLoverCase.toString().contains(" " + fn.toLowerCase() + " ")) {
                        retSb.append(fn);
                        break;
                    }
                    // с пробела и до конца строки
                    if (cvLoverCase.toString().contains(" " + fn.toLowerCase() + "\n")) {
                        retSb.append(fn);
                        break;
                    }
                }
            }
        }

        return retSb;
    }

    @Override
    public Integer countMachesSkill(String inputText, SkillTree skillTree) {
        Integer retInt = StringUtils.countMatches(inputText.toLowerCase(),
                skillTree.getSkillName().toLowerCase());

        return retInt;
    }

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
        return deleteSystemChar(getDataModel(cv, allCountryRegex));
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
        List<Company> companies = dataManager
                .load(Company.class)
                .list();
        List<Company> retCompanies = new ArrayList<>();

        for (Company company : companies) {
            if (company != null) {
                if (company.getComanyName() != null) {
                    if (textCV.toLowerCase().contains(company.getComanyName().toLowerCase())) {
                        retCompanies.add(company);
                    }

                    if (company.getCompanyShortName() != null) {
                        if (textCV.toLowerCase().contains(company.getCompanyShortName().toLowerCase())) {
                            retCompanies.add(company);
                        }
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
    public City parseCity(String textCV) {
        List<City> cities = dataManager.load(City.class).list();
        City retCity = null;

        if (textCV != null) {
            for (City city : cities) {
                if (city != null) {
                    if (city.getCityRuName() != null) {
                        if (textCV.toLowerCase().contains(city.getCityRuName().toLowerCase())) {
                            retCity = city;
                            break;
                        }
                    }
                }
            }
        }

        return retCity;
    }

    @Override
    public Company parseCompany(String textCV) {
        List<Company> companies = dataManager.load(Company.class).list();
        Company retCompany = null;

        if (textCV != null) {
            for (Company company : companies) {
                if (company != null) {
                    if (company.getComanyName() != null) {
                        if (textCV.contains(company.getComanyName())) {
                            retCompany = company;
                            break;
                        }
                    }

                    if (company.getCompanyShortName() != null) {
                        if (textCV.contains(company.getCompanyShortName())) {
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
            if (st.getSkillName().toLowerCase().equals(skillTree.getSkillName().toLowerCase())) {
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