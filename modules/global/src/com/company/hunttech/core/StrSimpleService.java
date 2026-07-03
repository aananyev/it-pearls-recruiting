package com.company.hunttech.core;

public interface StrSimpleService {
    String NAME = "hunttech_StrSimpleService";

    public String customReplaceAll(String str, String oldStr, String newStr);
    public String replaceAll(String inputStr, String from, String to);
    public String replaceAll(StringBuilder builder, String from, String to);
    public String deleteExtraCharacters(String inputStr);

}