package com.company.itpearls.core;

import com.company.itpearls.entity.Company;

import java.util.List;

public interface ParseCVService {
    String NAME = "itpearls_ParseCVService";

    String parseEmail(String cv);

    String parsePhone(String cv);

    List<String> extractUrls(String input);

    Company parseCompany(String textCV);

    String normalizePhoneStr(String phone);
}