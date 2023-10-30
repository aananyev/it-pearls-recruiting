package com.company.itpearls.core;

public interface StrSimpleService {
    String NAME = "itpearls_StrSimpleService";

    public String customReplaceAll(String str, String oldStr, String newStr);
    public String replaceAll(String inputStr, String from, String to);
    public String replaceAll(StringBuilder builder, String from, String to);
    public String deleteExtraCharacters(String inputStr);

}