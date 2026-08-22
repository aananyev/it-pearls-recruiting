package com.company.hunttech.service;

import com.company.hunttech.entity.Company;
import com.company.hunttech.entity.Person;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.*;

@Service(CompanySearchAiService.NAME)
public class CompanySearchAiServiceBean implements CompanySearchAiService {
    private static final Logger log = LoggerFactory.getLogger(CompanySearchAiServiceBean.class);

    @Inject
    private AiExecutionService aiExecutionService;
    @Inject
    private CompanyRequisitesIngestService companyRequisitesIngestService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<CompanyRequisitesParsedData> searchCompanyInWeb(String companyName, String inn) {
        List<CompanyRequisitesParsedData> results = new ArrayList<>();
        if ((companyName == null || companyName.trim().isEmpty()) && (inn == null || inn.trim().isEmpty())) {
            return results;
        }

        String searchName = companyName != null ? companyName.trim() : "";
        String searchInn = inn != null ? inn.trim() : "";

        try {
            Map<String, Object> context = new LinkedHashMap<>();
            context.put("companyName", searchName);
            context.put("inn", searchInn);
            context.put("searchQuery", (searchName + " " + searchInn).trim());
            context.put("callerSource", "CompanySearchAiService (searchCompanyInWeb)");
            context.put("sourceText", "Поиск в интернете организации: " + searchName + (searchInn.isEmpty() ? "" : " ИНН " + searchInn));

            AiExecutionResult aiResult = aiExecutionService.executeText(FUNCTION_COMPANY_WEB_SEARCH_PARSE_JSON, context);
            if (aiResult == null || aiResult.getText() == null || aiResult.getText().trim().isEmpty()) {
                // Fallback на универсальный парсер реквизитов
                aiResult = aiExecutionService.executeText(CompanyRequisitesIngestService.FUNCTION_COMPANY_REQUISITES_PARSE_JSON, context);
            }

            if (aiResult != null && aiResult.getText() != null && !aiResult.getText().trim().isEmpty()) {
                String json = cleanJson(aiResult.getText().trim());
                JsonNode root = objectMapper.readTree(json);

                if (root.isArray()) {
                    for (JsonNode item : root) {
                        CompanyRequisitesParsedData data = parseJsonNode(item);
                        if (isValidCandidate(data)) {
                            results.add(data);
                        }
                    }
                } else if (root.isObject()) {
                    if (root.has("candidates") && root.get("candidates").isArray()) {
                        for (JsonNode item : root.get("candidates")) {
                            CompanyRequisitesParsedData data = parseJsonNode(item);
                            if (isValidCandidate(data)) {
                                results.add(data);
                            }
                        }
                    } else if (root.has("items") && root.get("items").isArray()) {
                        for (JsonNode item : root.get("items")) {
                            CompanyRequisitesParsedData data = parseJsonNode(item);
                            if (isValidCandidate(data)) {
                                results.add(data);
                            }
                        }
                    } else {
                        CompanyRequisitesParsedData data = parseJsonNode(root);
                        if (isValidCandidate(data)) {
                            results.add(data);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("AI-поиск компании в интернете завершился с предупреждением: {}", e.getMessage());
        }

        // Если AI не нашел вариантов или вернул пустоту — создаем базовый кандидат из входных параметров
        if (results.isEmpty()) {
            CompanyRequisitesParsedData fallbackData = new CompanyRequisitesParsedData();
            fallbackData.setCompanyName(searchName.isEmpty() ? "Организация ИНН " + searchInn : searchName);
            fallbackData.setCompanyShortName(searchName);
            fallbackData.setInn(searchInn);
            fallbackData.setRawFoundSnippet("Черновой кандидат на основе введенных параметров поиска.");
            results.add(fallbackData);
        }

        return results;
    }

    private boolean isValidCandidate(CompanyRequisitesParsedData data) {
        if (data == null) return false;
        return (data.getCompanyName() != null && !data.getCompanyName().trim().isEmpty())
                || (data.getLegalEntityName() != null && !data.getLegalEntityName().trim().isEmpty())
                || (data.getInn() != null && !data.getInn().trim().isEmpty());
    }

    private CompanyRequisitesParsedData parseJsonNode(JsonNode node) {
        CompanyRequisitesParsedData data = new CompanyRequisitesParsedData();
        data.setCompanyName(textOrNull(node, "companyName"));
        data.setCompanyShortName(textOrNull(node, "companyShortName"));
        data.setLegalEntityName(textOrNull(node, "legalEntityName"));
        data.setOwnership(textOrNull(node, "ownership"));
        data.setInn(textOrNull(node, "inn"));
        data.setKpp(textOrNull(node, "kpp"));
        data.setOgrn(textOrNull(node, "ogrn"));
        data.setOkpo(textOrNull(node, "okpo"));
        data.setOktmo(textOrNull(node, "oktmo"));
        data.setOkved(textOrNull(node, "okved"));
        data.setCountry(textOrNull(node, "country"));
        data.setRegion(textOrNull(node, "region"));
        data.setCity(textOrNull(node, "city"));
        data.setStreetAddress(textOrNull(node, "streetAddress"));
        data.setLegalAddress(textOrNull(node, "legalAddress"));
        data.setActualAddress(textOrNull(node, "actualAddress"));
        data.setPostalAddress(textOrNull(node, "postalAddress"));
        data.setBik(textOrNull(node, "bik"));
        data.setBankName(textOrNull(node, "bankName"));
        data.setSettlementAccount(textOrNull(node, "settlementAccount"));
        data.setCorrespondentAccount(textOrNull(node, "correspondentAccount"));
        data.setPhone(textOrNull(node, "phone"));
        data.setEmail(textOrNull(node, "email"));
        data.setWebsite(textOrNull(node, "website"));

        data.setDirectorLastName(textOrNull(node, "directorLastName"));
        data.setDirectorFirstName(textOrNull(node, "directorFirstName"));
        data.setDirectorMiddleName(textOrNull(node, "directorMiddleName"));
        data.setDirectorPosition(textOrNull(node, "directorPosition"));
        data.setDirectorPhone(textOrNull(node, "directorPhone"));
        data.setDirectorEmail(textOrNull(node, "directorEmail"));

        data.setCompanyDescription(textOrNull(node, "companyDescription"));
        data.setWorkingConditions(textOrNull(node, "workingConditions"));
        data.setRawFoundSnippet(textOrNull(node, "rawFoundSnippet"));

        if (data.getCompanyName() == null && data.getLegalEntityName() != null) {
            data.setCompanyName(data.getLegalEntityName());
        }
        return data;
    }

    private String textOrNull(JsonNode node, String fieldName) {
        if (node.has(fieldName) && !node.get(fieldName).isNull()) {
            String val = node.get(fieldName).asText();
            return (val != null && !val.trim().isEmpty() && !"null".equalsIgnoreCase(val.trim())) ? val.trim() : null;
        }
        return null;
    }

    private String cleanJson(String raw) {
        String text = raw.trim();
        if (text.startsWith("```json")) {
            text = text.substring(7);
        } else if (text.startsWith("```")) {
            text = text.substring(3);
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        text = text.trim();
        int firstBracket = -1;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{' || c == '[') {
                firstBracket = i;
                break;
            }
        }
        if (firstBracket >= 0) {
            text = text.substring(firstBracket);
        }
        int lastBracket = -1;
        for (int i = text.length() - 1; i >= 0; i--) {
            char c = text.charAt(i);
            if (c == '}' || c == ']') {
                lastBracket = i;
                break;
            }
        }
        if (lastBracket >= 0) {
            text = text.substring(0, lastBracket + 1);
        }
        return text.trim();
    }

    @Override
    public CompanyRequisitesParsedData parseCompanyData(String rawText) {
        return companyRequisitesIngestService.parseRequisites(rawText);
    }

    @Override
    public Person resolveOrCreateDirector(CompanyRequisitesParsedData data) {
        return companyRequisitesIngestService.resolveOrCreateDirector(data);
    }

    @Override
    public Company applyCompanyData(Company company, CompanyRequisitesParsedData data) {
        Company result = companyRequisitesIngestService.applyRequisitesToCompany(company, data);
        if (data != null) {
            if (data.getCompanyDescription() != null && !data.getCompanyDescription().trim().isEmpty()) {
                result.setCompanyDescription(data.getCompanyDescription().trim());
            }
            if (data.getWorkingConditions() != null && !data.getWorkingConditions().trim().isEmpty()) {
                result.setWorkingConditions(data.getWorkingConditions().trim());
            }
        }
        return result;
    }
}
