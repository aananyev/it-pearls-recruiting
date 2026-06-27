package com.company.itpearls.service;

public interface HrmAiService {
    String NAME = "itpearls_HrmAiService";

    String standardizeVacancyDescription(String rawText, String providerCode);

    String generateVacancyArtifact(String standardizedDescription, String templateCode, String providerCode);
}
