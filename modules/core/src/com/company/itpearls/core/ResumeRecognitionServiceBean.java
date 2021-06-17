package com.company.itpearls.core;

import org.springframework.stereotype.Service;

@Service(ResumeRecognitionService.NAME)
public class ResumeRecognitionServiceBean implements ResumeRecognitionService {

    @Override
    public String languageDetection(String cvText) {
        return null;
    }

    @Override
    public String[] sentenceDetection(String cvText) {
        return new String[0];
    }

    @Override
    public String[] tokenDetection(String cvText) {
        return new String[0];
    }
}