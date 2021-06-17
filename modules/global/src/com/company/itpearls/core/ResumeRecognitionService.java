package com.company.itpearls.core;

public interface ResumeRecognitionService {
    String NAME = "itpearls_ResumeRecognitionService";

    String languageDetection(String cvText);
    String[] sentenceDetection(String cvText);
    String[] tokenDetection(String cvText);
}