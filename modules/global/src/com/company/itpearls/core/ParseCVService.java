package com.company.itpearls.core;

import com.company.itpearls.entity.Company;
import com.company.itpearls.entity.Position;

import java.util.List;

public interface ParseCVService {
    String NAME = "itpearls_ParseCVService";

    String parseEmail(String cv);

    String parsePhone(String cv);

    List<String> extractUrls(String input);

    Company parseCompany(String textCV);

    String normalizePhoneStr(String phone);

    String colorHighlightingCompetencies(String htmlText, String color);

    String colorHighlingCompany(String htmlText, String color);

    List<Position> parsePositions(String textCV);

    List<Company> parseCompanies(String textCV);

    String colorHighlingPositions(String htmlText, String color);
}