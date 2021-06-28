package com.company.itpearls.core;

public interface ParseCVService {
    String NAME = "itpearls_ParseCVService";

    public String parseEmail(String cv);
    public String parsePhone(String cv);
}