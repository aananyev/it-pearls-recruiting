package com.company.hunttech.service;

public interface HrmAiService {
    String NAME = "hunttech_HrmAiService";

    String standardizeVacancyDescription(String rawText, String providerCode);

    String generateVacancyArtifact(String standardizedDescription, String templateCode, String providerCode);
}
