package com.company.hunttech.core;

import com.company.hunttech.entity.CandidateCV;
import com.company.hunttech.entity.Company;
import com.company.hunttech.entity.OpenPosition;

import java.util.Set;

public interface ResumeRecognitionService {
    String NAME = "hunttech_ResumeRecognitionService";

    Set<String> scanSocialNetworksFromCVs(CandidateCV candidateCV);

    Set<String> scanSocialNetworksFromCVs(String candidateCV);

    String parseFirstName(String cvTect);

    String parseSecondName(String cvText);

    String parseMiddleName(String cvText);

    String setTemplateLetter(OpenPosition openPosition);
}