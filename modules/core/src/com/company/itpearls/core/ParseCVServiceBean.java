package com.company.itpearls.core;

import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service(ParseCVService.NAME)
public class ParseCVServiceBean implements ParseCVService {

    static String emailPtrn = "\\w+([\\.-]?\\w+)*@\\w+([\\.-]?\\w+)*\\.\\w{2,4}";
    static String phonePtrn = "[7|8][ (-]?[\\d]{3}[ )-]?[\\d]{3}[ -]?[\\d]{2}[ -]?[\\d]{2}[\\D]";

    @Override
    public String parseEmail(String cv) {
        Pattern emailPattern = Pattern.compile(emailPtrn);
        Matcher emailMatcher = emailPattern.matcher(Jsoup.parse(cv).text());

        String retStr = "";

        if(emailMatcher.find()) {
            retStr = emailMatcher.group();
        } else {
            retStr = null;
        }

        return retStr;
    }

    @Override
    public String parsePhone(String cv) {
        return getDataModel(cv, phonePtrn);
    }

    private String getDataModel(String onStr, String pattern) {
        Pattern patt = Pattern.compile(pattern);
        Matcher matcher = patt.matcher(Jsoup.parse(onStr).text());

        if(matcher.find()) {
            return matcher.group();
        } else {
            return null;
        }
    }
}