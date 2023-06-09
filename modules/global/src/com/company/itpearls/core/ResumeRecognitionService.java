package com.company.itpearls.core;

import com.company.itpearls.entity.CandidateCV;
import com.company.itpearls.entity.Company;

import java.util.Set;

public interface ResumeRecognitionService {
    String NAME = "itpearls_ResumeRecognitionService";

    Set<String> scanSocialNetworksFromCVs(CandidateCV candidateCV);

    Set<String> scanSocialNetworksFromCVs(String candidateCV);

    String parseFirstName(String cvTect);

    String parseSecondName(String cvText);

    String parseMiddleName(String cvText);
}