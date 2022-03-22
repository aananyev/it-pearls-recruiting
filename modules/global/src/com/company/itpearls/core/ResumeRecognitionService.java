package com.company.itpearls.core;

import com.company.itpearls.entity.Company;

public interface ResumeRecognitionService {
    String NAME = "itpearls_ResumeRecognitionService";

    String parseFirstName(String cvTect);

    String parseSecondName(String cvText);

    String parseMiddleName(String cvText);
}