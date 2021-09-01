package com.company.itpearls.core;

import com.company.itpearls.entity.Company;
import com.haulmont.cuba.core.global.DataManager;
import org.jsoup.Jsoup;
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

        if(input != null) {
            for (int i = 0; i < input.length(); i++) {
                Boolean flagTaboo = true;

                for (char c : taboo.toCharArray()) {
                    if(input.charAt(i) == c) {
                        flagTaboo = false;
                    }
                }

                if(flagTaboo) {
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

    @Override
    public Company parseCompany(String textCV) {
        List<Company> companies = dataManager.load(Company.class).list();
        Company retCompany = null;

        for (Company company : companies) {
            if (company.getComanyName() != null) {
                if (textCV.contains(company.getComanyName())) {
                    retCompany = company;
                    break;
                }
            }
        }

        return retCompany;
    }
}