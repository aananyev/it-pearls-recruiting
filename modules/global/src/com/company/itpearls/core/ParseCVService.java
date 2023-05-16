package com.company.itpearls.core;

import com.company.itpearls.entity.*;

import java.util.Date;
import java.util.List;

public interface ParseCVService {
    String NAME = "itpearls_ParseCVService";

    Date parseDate(String cv);

    Date parseDate(StringBuffer cv);

    List<String> getFirstNameList(String cv);

    String parseFirstName(String cv);

    String parseMiddleName(String cv);

    String parseSecondName(String cv);

    Integer countMachesSkill(String inputText, SkillTree skillTree);

    String parseEmail(String cv);

    String parsePhone(String cv);

    List<String> extractUrls(String input);

    City parseCity(String textCV);

    Company parseCompany(String textCV);

    String normalizePhoneStr(String phone);

    String colorHighlightingCompetencies(OpenPosition openPosition, String htmlText,
                                         String color, String colorIfExist);

    String colorHighlightingCompetencies(String htmlText, String color);

    String colorHighlingCompany(String htmlText, String color);

    List<Position> parsePositions(String textCV);

    List<Company> parseCompanies(String textCV);

    String colorHighlingPositions(String htmlText, String color);

    String br2nl(String html);

    String nl2br(String html);
}
