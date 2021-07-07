package com.company.itpearls.web.screens.jobcandidate;

import com.company.itpearls.entity.JobCandidate;
import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContactInfo {

    static String emailPtrn = "\\w+([\\.-]?\\w+)*@\\w+([\\.-]?\\w+)*\\.\\w{2,4}";
    static String phonePtrn = "[7|8][ (-]?[\\d]{3}[ )-]?[\\d]{3}[ -]?[\\d]{2}[ -]?[\\d]{2}[\\D]";


    private JobCandidate jobCandidate;
    protected String email; // parse
    protected String phone; // parse
    protected String mobilePhone;
    protected String skypeName;
    protected String telegramName;
    protected String telegramGroup;
    protected String viberName;
    protected String whatsAppName;
    protected List<String> urls = new ArrayList<>();


    public JobCandidate getJobCandidate() {
        return jobCandidate;
    }

    public void setJobCandidate(JobCandidate jobCandidate) {
        this.jobCandidate = jobCandidate;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPhone() {
        return phone;
    }

    public void setMobilePhone(String mobilePhone) {
        this.mobilePhone = mobilePhone;
    }

    public String getMobilePhone() {
        return mobilePhone;
    }

    public String getTelegramGroup() {
        return telegramGroup;
    }

    public String getSkypeName() {
        return skypeName;
    }

    public String getTelegramName() {
        return telegramName;
    }

    public String getWhatsAppName() {
        return whatsAppName;
    }

    public String getViberName() {
        return viberName;
    }

    public void setViberName(String viberName) {
        this.viberName = viberName;
    }

    public void setTelegramGroup(String telegramGroup) {
        this.telegramGroup = telegramGroup;
    }

    public void setSkypeName(String skypeName) {
        this.skypeName = skypeName;
    }

    public void setTelegramName(String telegramName) {
        this.telegramName = telegramName;
    }

    public void setWhatsAppName(String whatsAppName) {
        this.whatsAppName = whatsAppName;
    }

    public void setUrls(List<String> urls) {
        this.urls = urls;
    }

    public List<String> getUrls() {
        return urls;
    }

    public String parseEmail(String text) {
        Pattern emailPattern = Pattern.compile(emailPtrn);
        Matcher emailMatcher = emailPattern.matcher(Jsoup.parse(text).text());

        String retStr = "";

        if (emailMatcher.find()) {
            retStr = emailMatcher.group();
        } else {
            retStr = null;
        }

        setEmail(retStr);
        return retStr;
    }

    public String parsePhone(String text) {
        Pattern patt = Pattern.compile(phonePtrn);
        Matcher matcher = patt.matcher(Jsoup.parse(text).text());

        if (matcher.find()) {
            setPhone(matcher.group());
            return matcher.group();
        } else {
            setPhone(null);
            return null;
        }
    }

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

        setUrls(result);

        return result;
    }

}
