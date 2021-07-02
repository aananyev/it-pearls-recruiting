package com.company.itpearls.core;

import java.util.List;

public interface ParseCVService {
    String NAME = "itpearls_ParseCVService";

    public String parseEmail(String cv);
    public String parsePhone(String cv);

    List<String> extractUrls(String input);
}