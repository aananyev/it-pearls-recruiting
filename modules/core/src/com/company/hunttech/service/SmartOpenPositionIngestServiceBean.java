package com.company.hunttech.service;

import com.company.hunttech.entity.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.core.global.Metadata;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service(SmartOpenPositionIngestService.NAME)
public class SmartOpenPositionIngestServiceBean implements SmartOpenPositionIngestService {
    private static final Logger log = LoggerFactory.getLogger(SmartOpenPositionIngestServiceBean.class);

    private static final String FUNCTION_VACANCY_SMART_PARSE_JSON = "VACANCY_SMART_PARSE_JSON";

    @Inject
    private DataManager dataManager;
    @Inject
    private Metadata metadata;
    @Inject
    private FileLoader fileLoader;
    @Inject
    private AiExecutionService aiExecutionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String extractTextFromFile(FileDescriptor fileDescriptor, byte[] fileBytes) {
        String fileName = fileDescriptor != null ? fileDescriptor.getName() : "noname";
        int size = fileBytes != null ? fileBytes.length : 0;
        String ext = fileDescriptor != null && fileDescriptor.getExtension() != null
                ? fileDescriptor.getExtension().toLowerCase() : "";
        log.info("[SMART_VACANCY_OPENING] Извлечение текста из файла: '{}', размер: {} байт, расширение: '{}'", fileName, size, ext);

        if (fileBytes == null || fileBytes.length == 0) {
            if (fileDescriptor != null) {
                try (InputStream is = fileLoader.openStream(fileDescriptor)) {
                    fileBytes = is.readAllBytes();
                    log.info("[SMART_VACANCY_OPENING] Файл успешно загружен из FileLoader, прочитано {} байт", fileBytes.length);
                } catch (Exception e) {
                    log.error("[SMART_VACANCY_OPENING] ✘ Не удалось прочитать файл вакансии из хранилища: " + fileName, e);
                    return "";
                }
            } else {
                log.warn("[SMART_VACANCY_OPENING] Пустые байты и отсутствует FileDescriptor при извлечении текста файла");
                return "";
            }
        }

        try {
            String extracted;
            if ("pdf".equals(ext)) {
                try (PDDocument document = Loader.loadPDF(fileBytes)) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    extracted = stripper.getText(document);
                }
            } else if ("docx".equals(ext)) {
                try (InputStream is = new ByteArrayInputStream(fileBytes);
                     XWPFDocument doc = new XWPFDocument(is);
                     XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
                    extracted = extractor.getText();
                }
            } else if ("doc".equals(ext)) {
                try (InputStream is = new ByteArrayInputStream(fileBytes);
                     WordExtractor extractor = new WordExtractor(is)) {
                    extracted = extractor.getText();
                }
            } else {
                extracted = new String(fileBytes, StandardCharsets.UTF_8);
            }
            log.info("[SMART_VACANCY_OPENING] ✓ Текст из файла '{}' успешно извлечен (длина: {} символов). Превью: [{}]",
                    fileName, extracted != null ? extracted.length() : 0, preview(extracted, 120));
            return extracted != null ? extracted : "";
        } catch (Exception e) {
            log.error("[SMART_VACANCY_OPENING] Ошибка при парсинге документа " + fileName + ", fallback на UTF-8 строку", e);
            try {
                return new String(fileBytes, StandardCharsets.UTF_8);
            } catch (Exception ignored) {
                return "";
            }
        }
    }

    @Override
    public SmartOpenPositionParsedData parseVacancyText(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            log.warn("[SMART_VACANCY_OPENING] Запрос на парсинг с пустым текстом вакансии");
            SmartOpenPositionParsedData emptyData = new SmartOpenPositionParsedData();
            emptyData.setRawText("");
            return emptyData;
        }

        // Предотвращение HTML-разметки от RichTextArea: очищаем до форматированного plain text
        String cleanedPlainText = cleanHtmlToPlainText(rawText);
        String textForAi = !cleanedPlainText.isEmpty() ? cleanedPlainText : rawText.trim();

        log.info("[SMART_VACANCY_OPENING] >>> Начало парсинга текста вакансии (длина исходного: {}, очищенного: {} символов). Превью: [{}]",
                rawText.length(), textForAi.length(), preview(textForAi, 150));

        SmartOpenPositionParsedData data = new SmartOpenPositionParsedData();
        data.setRawText(textForAi);

        // 1. Попытка AI-анализа через системный сервис
        boolean parsedByAi = false;
        try {
            log.info("[SMART_VACANCY_OPENING] Отправка запроса к AI-функции '{}'...", FUNCTION_VACANCY_SMART_PARSE_JSON);
            Map<String, Object> ctx = Collections.singletonMap("sourceText", textForAi);
            AiExecutionResult aiResult = aiExecutionService.executeText(FUNCTION_VACANCY_SMART_PARSE_JSON, ctx);

            if (aiResult != null && aiResult.getText() != null && !aiResult.getText().trim().isEmpty()) {
                String aiRawResponse = aiResult.getText();
                log.info("[SMART_VACANCY_OPENING] Получен ответ от AI (длина: {}): [{}]",
                        aiRawResponse.length(), preview(aiRawResponse, 300));

                String aiText = extractCleanJson(aiRawResponse);
                JsonNode json = objectMapper.readTree(aiText);
                
                // AI может вернуть массив [{...}] вместо объекта {...} — берём первый элемент
                if (json.isArray() && json.size() > 0) {
                    json = json.get(0);
                }
                
                log.info("[SMART_VACANCY_OPENING] JSON успешно десериализован из AI-ответа: {}", json.toString());

                // 1.1 Название вакансии (поддержка вариаций c/s и алиасов)
                String vacName = getJsonString(json, "vacancyName", "vacansyName", "title", "name", "position");
                if (vacName != null && !vacName.isEmpty()) {
                    data.setVacansyName(cleanTitle(vacName));
                }

                // 1.2 Название проекта
                String projName = getJsonString(json, "projectName", "project", "projectTitle");
                if (projName != null && !projName.isEmpty()) {
                    data.setProjectName(cleanTitle(projName));
                }

                // 1.3 Название компании
                String compName = getJsonString(json, "companyName", "company", "customer", "client");
                if (compName != null && !compName.isEmpty()) {
                    data.setCompanyName(cleanTitle(compName));
                }

                // 1.4 Тип позиции / должность
                String posTypeName = getJsonString(json, "positionName", "positionTypeName", "role", "jobTitle", "specialization");
                if (posTypeName != null && !posTypeName.isEmpty()) {
                    data.setPositionTypeName(cleanTitle(posTypeName));
                }

                // 1.5 Грейд
                String grade = getJsonString(json, "grade", "gradeName", "level", "seniority");
                if (grade != null && !grade.isEmpty()) {
                    data.setGradeName(cleanTitle(grade));
                }

                // 1.6 Город / локация
                String city = getJsonString(json, "cityName", "city", "location");
                if (city == null && json.has("cities") && json.get("cities").isArray() && json.get("cities").size() > 0) {
                    city = json.get("cities").get(0).asText(null);
                }
                if (city != null && !city.isEmpty()) {
                    data.setCityName(cleanTitle(city));
                }

                // 1.7 Формат работы (REMOTE / HYBRID / OFFICE или 1 / 2 / 0)
                if (json.has("remoteWork")) {
                    JsonNode rwNode = json.get("remoteWork");
                    if (rwNode.isNumber()) {
                        data.setRemoteWork(rwNode.asInt(1));
                    } else if (rwNode.isTextual()) {
                        String rwStr = rwNode.asText().toUpperCase().trim();
                        if (rwStr.contains("OFFICE") || rwStr.contains("ONSITE") || rwStr.contains("ОФИС")) {
                            data.setRemoteWork(0);
                        } else if (rwStr.contains("HYBRID") || rwStr.contains("ГИБРИД")) {
                            data.setRemoteWork(2);
                        } else {
                            data.setRemoteWork(1); // REMOTE
                        }
                    }
                } else if (json.has("workFormat")) {
                    String wf = json.get("workFormat").asText("").toUpperCase();
                    if (wf.contains("OFFICE") || wf.contains("ОФИС")) data.setRemoteWork(0);
                    else if (wf.contains("HYBRID") || wf.contains("ГИБРИД")) data.setRemoteWork(2);
                    else data.setRemoteWork(1);
                }

                // 1.8 Зарплата
                BigDecimal sMin = getJsonBigDecimal(json, "salaryMin", "minSalary", "salaryFrom", "rateMin");
                if (sMin != null) data.setSalaryMin(sMin);
                BigDecimal sMax = getJsonBigDecimal(json, "salaryMax", "maxSalary", "salaryTo", "rateMax", "rate");
                if (sMax != null) data.setSalaryMax(sMax);

                // 1.9 Опыт работы
                if (json.has("workExperience") || json.has("experience")) {
                    JsonNode expNode = json.has("workExperience") ? json.get("workExperience") : json.get("experience");
                    if (expNode.isNumber()) {
                        data.setWorkExperience(expNode.asInt(3));
                    } else if (expNode.isTextual()) {
                        Pattern numPat = Pattern.compile("(\\d+)");
                        Matcher numMat = numPat.matcher(expNode.asText());
                        if (numMat.find()) {
                            try {
                                data.setWorkExperience(Integer.parseInt(numMat.group(1)));
                            } catch (Exception ignored) {
                                data.setWorkExperience(3);
                            }
                        } else {
                            data.setWorkExperience(3);
                        }
                    }
                }

                // 1.10 Краткое описание
                String shortDesc = getJsonString(json, "shortDescription", "projectShortDescription", "description", "summary");
                if (shortDesc != null && !shortDesc.isEmpty()) {
                    data.setShortDescription(cleanHtmlToPlainText(shortDesc));
                }

                // 1.11 Навыки
                List<String> skills = getJsonStringList(json, "skills", "requiredSkills", "techStack", "technologies", "keySkills");
                if (!skills.isEmpty()) {
                    data.setRequiredSkills(skills);
                }

                // 1.12 Чеклист / требования
                List<String> chk = getJsonStringList(json, "checklist", "requirements", "tasks", "responsibilities");
                if (!chk.isEmpty()) {
                    data.setChecklist(chk);
                }

                if (data.getVacansyName() != null && !data.getVacansyName().isEmpty()) {
                    parsedByAi = true;
                    log.info("[SMART_VACANCY_OPENING] ✓ AI-распознавание завершено успешно. Позиция: '{}', Проект: '{}', Компания: '{}', Зарплата: {} - {}, Навыки: {}",
                            data.getVacansyName(), data.getProjectName(), data.getCompanyName(), data.getSalaryMin(), data.getSalaryMax(), data.getRequiredSkills());
                } else {
                    log.warn("[SMART_VACANCY_OPENING] AI вернул JSON без поля наименования вакансии, запуск fallback-парсера");
                }
            } else {
                log.warn("[SMART_VACANCY_OPENING] AI вернул пустой результат, переключение на эвристический парсер");
            }
        } catch (Exception e) {
            log.warn("[SMART_VACANCY_OPENING] AI-распознавание вакансии завершилось с ошибкой ({}), используется встроенный эвристический парсер: {}",
                    e.getClass().getSimpleName(), e.getMessage(), e);
        }

        // 2. Эвристический fallback-парсинг, если AI недоступен или вернул пустые поля
        if (!parsedByAi || data.getVacansyName() == null || data.getVacansyName().isEmpty()) {
            log.info("[SMART_VACANCY_OPENING] Запуск эвристического fallback-парсера...");
            fallbackHeuristicParse(textForAi, data);
            log.info("[SMART_VACANCY_OPENING] Результаты эвристического парсинга: vacansyName='{}', remoteWork={}, salaryMin={}, salaryMax={}, grade='{}', skills={}",
                    data.getVacansyName(), data.getRemoteWork(), data.getSalaryMin(), data.getSalaryMax(), data.getGradeName(), data.getRequiredSkills());
        }

        data.setComment(textForAi);

        // Проверка обязательных полей
        if (data.getVacansyName() == null || data.getVacansyName().isEmpty()) {
            data.setVacansyName("Новая открытая вакансия");
            data.getMissingFields().add("Название вакансии определено автоматически по умолчанию");
            log.warn("[SMART_VACANCY_OPENING] Название вакансии не удалось извлечь, установлено имя по умолчанию");
        }
        if (data.getProjectName() == null || data.getProjectName().isEmpty()) {
            data.getMissingFields().add("Проект не указан в тексте (будет выбран основной)");
            log.info("[SMART_VACANCY_OPENING] Проект не указан в описании вакансии");
        }

        log.info("[SMART_VACANCY_OPENING] <<< Итоговая структура ParsedData: name='{}', project='{}', salary={}-{}, skillsCount={}, missingFields={}",
                data.getVacansyName(), data.getProjectName(), data.getSalaryMin(), data.getSalaryMax(),
                data.getRequiredSkills() != null ? data.getRequiredSkills().size() : 0, data.getMissingFields());

        return data;
    }

    private String extractCleanJson(String raw) {
        String s = raw.trim();
        if (s.startsWith("```json")) {
            s = s.substring(7);
        } else if (s.startsWith("```")) {
            s = s.substring(3);
        }
        if (s.endsWith("```")) {
            s = s.substring(0, s.length() - 3);
        }
        return s.trim();
    }

    private String getJsonString(JsonNode json, String... keys) {
        for (String k : keys) {
            if (json.hasNonNull(k) && !json.get(k).asText().trim().isEmpty()) {
                return json.get(k).asText().trim();
            }
        }
        return null;
    }

    private BigDecimal getJsonBigDecimal(JsonNode json, String... keys) {
        for (String k : keys) {
            if (json.hasNonNull(k)) {
                JsonNode node = json.get(k);
                if (node.isNumber()) {
                    return BigDecimal.valueOf(node.asDouble());
                } else if (node.isTextual()) {
                    String text = node.asText().trim();
                    // Извлекаем первое число из строки (защита от склеивания диапазонов "150 000 - 200 000")
                    Pattern numPat = Pattern.compile("(\\d[\\d\\s]*[.,]?\\d*)");
                    Matcher numMat = numPat.matcher(text.replaceAll("\\s+", ""));
                    if (numMat.find()) {
                        try {
                            String numStr = numMat.group(1).replace(",", ".");
                            return new BigDecimal(numStr);
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
        return null;
    }

    private List<String> getJsonStringList(JsonNode json, String... keys) {
        List<String> result = new ArrayList<>();
        for (String k : keys) {
            if (json.has(k)) {
                JsonNode node = json.get(k);
                if (node.isArray()) {
                    for (JsonNode item : node) {
                        String s = item.asText().trim();
                        if (!s.isEmpty()) result.add(s);
                    }
                    if (!result.isEmpty()) return result;
                } else if (node.isTextual() && !node.asText().trim().isEmpty()) {
                    String[] parts = node.asText().split("[,;\\n]+");
                    for (String p : parts) {
                        String s = p.trim();
                        if (!s.isEmpty()) result.add(s);
                    }
                    if (!result.isEmpty()) return result;
                }
            }
        }
        return result;
    }

    private void fallbackHeuristicParse(String text, SmartOpenPositionParsedData data) {
        String cleanText = cleanHtmlToPlainText(text);
        String[] lines = cleanText.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            // Пропускаем служебные строки с ID
            if (trimmed.matches("(?i)^(?:[🆔\\s]*ID|🆔|запрос|номер).*?\\d+.*")) {
                continue;
            }

            if (data.getVacansyName() == null) {
                if (trimmed.toLowerCase().startsWith("вакансия:") || trimmed.toLowerCase().startsWith("позиция:") || trimmed.toLowerCase().startsWith("должность:")) {
                    data.setVacansyName(cleanTitle(trimmed.substring(trimmed.indexOf(":") + 1).trim()));
                } else if (trimmed.length() < 80 && (trimmed.toLowerCase().contains("developer") || trimmed.toLowerCase().contains("engineer") || trimmed.toLowerCase().contains("разработчик") || trimmed.toLowerCase().contains("аналитик") || trimmed.toLowerCase().contains("тестировщик") || trimmed.toLowerCase().contains("дизайнер") || trimmed.toLowerCase().contains("менеджер") || trimmed.toLowerCase().contains("lead") || trimmed.toLowerCase().contains("devops") || trimmed.toLowerCase().contains("architect"))) {
                    data.setVacansyName(cleanTitle(trimmed));
                }
            }

            // Зарплата / ставка
            Pattern salaryPattern = Pattern.compile("(?:ставка|зп|оплата|доход)?[:\\s]*(\\d[\\d\\s]{2,})\\s*(?:-|до|—|–)\\s*(\\d[\\d\\s]{2,})\\s*(?:руб|р|rub|usd|\\$|€|рд|\\/час)?", Pattern.CASE_INSENSITIVE);
            Matcher salaryMatcher = salaryPattern.matcher(trimmed);
            if (salaryMatcher.find()) {
                try {
                    String minStr = salaryMatcher.group(1).replaceAll("\\s+", "");
                    String maxStr = salaryMatcher.group(2).replaceAll("\\s+", "");
                    if (data.getSalaryMin() == null) data.setSalaryMin(new BigDecimal(minStr));
                    if (data.getSalaryMax() == null) data.setSalaryMax(new BigDecimal(maxStr));
                } catch (Exception ignored) {}
            } else {
                Pattern singleRatePattern = Pattern.compile("(?:ставка|до)\\s*(\\d[\\d\\s]{2,})", Pattern.CASE_INSENSITIVE);
                Matcher singleRateMatcher = singleRatePattern.matcher(trimmed);
                if (singleRateMatcher.find() && data.getSalaryMax() == null) {
                    try {
                        String rateStr = singleRateMatcher.group(1).replaceAll("\\s+", "");
                        data.setSalaryMax(new BigDecimal(rateStr));
                    } catch (Exception ignored) {}
                }
            }

            // Формат работы
            if (trimmed.toLowerCase().contains("удален") || trimmed.toLowerCase().contains("remote")) {
                data.setRemoteWork(1);
            } else if (trimmed.toLowerCase().contains("гибрид") || trimmed.toLowerCase().contains("hybrid")) {
                data.setRemoteWork(2);
            } else if (trimmed.toLowerCase().contains("офис") || trimmed.toLowerCase().contains("office")) {
                data.setRemoteWork(0);
            }

            // Опыт
            Pattern expPattern = Pattern.compile("(?:опыт|стаж).*?(\\d+)\\s*(?:лет|года|год)", Pattern.CASE_INSENSITIVE);
            Matcher expMatcher = expPattern.matcher(trimmed);
            if (expMatcher.find()) {
                try {
                    data.setWorkExperience(Integer.parseInt(expMatcher.group(1)));
                } catch (Exception ignored) {}
            }

            // Грейд
            if (trimmed.toLowerCase().contains("senior") || trimmed.toLowerCase().contains("сеньор")) {
                data.setGradeName("Senior");
            } else if (trimmed.toLowerCase().contains("middle") || trimmed.toLowerCase().contains("мидл")) {
                data.setGradeName("Middle");
            } else if (trimmed.toLowerCase().contains("junior") || trimmed.toLowerCase().contains("джуниор")) {
                data.setGradeName("Junior");
            } else if (trimmed.toLowerCase().contains("lead") || trimmed.toLowerCase().contains("тимлид") || trimmed.toLowerCase().contains("лид")) {
                data.setGradeName("Lead");
            }
        }

        if (data.getVacansyName() == null) {
            for (String l : lines) {
                String cleanL = cleanTitle(l);
                if (!cleanL.isEmpty() && cleanL.length() <= 80 && !cleanL.matches("(?i)^(?:[🆔\\s]*ID|🆔|запрос|номер).*")) {
                    data.setVacansyName(cleanL);
                    break;
                }
            }
        }

        // Поиск навыков по ключевым словам
        String[] popularSkills = {"Java", "Spring", "Kotlin", "Python", "PostgreSQL", "Docker", "Kubernetes", "React", "TypeScript", "JavaScript", "Go", "C#", "Kafka", "Redis", "Git", "CI/CD", "Linux", "SQL", "BPMN", "Бизнес-анализ", "Честный знак"};
        List<String> foundSkills = new ArrayList<>();
        for (String skill : popularSkills) {
            if (Pattern.compile("\\b" + Pattern.quote(skill) + "\\b", Pattern.CASE_INSENSITIVE).matcher(cleanText).find()) {
                foundSkills.add(skill);
            }
        }
        if (data.getRequiredSkills() == null || data.getRequiredSkills().isEmpty()) {
            data.setRequiredSkills(foundSkills);
        }
    }

    @Override
    public OpenPosition findDuplicate(SmartOpenPositionParsedData data) {
        if (data == null || data.getVacansyName() == null) return null;
        String cleanName = cleanTitle(data.getVacansyName());
        if (cleanName.isEmpty()) return null;

        log.info("[SMART_VACANCY_OPENING] Поиск дубликата для вакансии: '{}'", cleanName);
        List<OpenPosition> list = dataManager.load(OpenPosition.class)
                .query("select e from hunttech_OpenPosition e where lower(e.vacansyName) = lower(:name) and e.openClose = false")
                .parameter("name", cleanName)
                .view("openPosition-browse-view")
                .maxResults(1)
                .list();
        if (!list.isEmpty()) {
            OpenPosition duplicate = list.get(0);
            log.info("[SMART_VACANCY_OPENING] Найдена существующая открытая вакансия (дубликат): ID={}, vacansyID={}, name='{}'",
                    duplicate.getId(), duplicate.getVacansyID(), duplicate.getVacansyName());
            return duplicate;
        }
        log.info("[SMART_VACANCY_OPENING] Дубликатов для вакансии '{}' не обнаружено", cleanName);
        return null;
    }

    @Override
    public SmartOpenPositionIngestResult createOpenPosition(SmartOpenPositionParsedData data, ExtUser recruiter) {
        log.info("[SMART_VACANCY_OPENING] >>> Создание новой сущности OpenPosition в БД. Рекрутер: '{}', Вакансия: '{}'",
                recruiter != null ? recruiter.getLogin() : "SYSTEM", data.getVacansyName());
        SmartOpenPositionIngestResult result = new SmartOpenPositionIngestResult();
        try {
            CommitContext commitContext = new CommitContext();

            String rawName = data.getVacansyName() != null ? data.getVacansyName() : "Открытая вакансия";
            String safeVacName = truncate(cleanTitle(rawName), 250);
            if (safeVacName.isEmpty()) safeVacName = "Открытая вакансия";

            OpenPosition openPosition = metadata.create(OpenPosition.class);
            openPosition.setVacansyName(safeVacName);
            openPosition.setOpenClose(false); // Открыта
            openPosition.setSignDraft(true);  // Вакансия создается как черновик (требование: скрыта до ручной проверки и снятия черновика)
            openPosition.setRemoteWork(data.getRemoteWork() != null ? data.getRemoteWork() : 1);
            openPosition.setWorkExperience(data.getWorkExperience() != null ? data.getWorkExperience() : 3);
            openPosition.setNumberPosition(data.getNumberPosition() != null ? data.getNumberPosition() : 1);
            openPosition.setSalaryMin(data.getSalaryMin());
            openPosition.setSalaryMax(data.getSalaryMax());
            // Все вакансии, созданные через умную ИИ-загрузку, получают статус «На проверку» (-2)
            openPosition.setPriority(OpenPositionPriority.UNDER_REVIEW.getId());

            String fullComment = data.getComment() != null ? data.getComment() : data.getRawText();
            openPosition.setComment(cleanHtmlToPlainText(fullComment));

            String shortDesc = data.getShortDescription() != null && !data.getShortDescription().isEmpty()
                    ? cleanHtmlToPlainText(data.getShortDescription()) : safeVacName;
            openPosition.setShortDescription(truncate(shortDesc, 250));

            openPosition.setLastOpenDate(new Date());
            openPosition.setOwner(recruiter);
            openPosition.setCommandCandidate(1);

            log.info("[SMART_VACANCY_OPENING] Заполнены атрибуты OpenPosition: name='{}', signDraft=true (ЧЕРНОВИК), priority={} (UNDER_REVIEW), remoteWork={}, salaryMin={}, salaryMax={}, exp={}",
                    openPosition.getVacansyName(), openPosition.getPriority(), openPosition.getRemoteWork(), openPosition.getSalaryMin(), openPosition.getSalaryMax(), openPosition.getWorkExperience());

            // Поиск / привязка проекта
            Project project = findOrCreateProject(data.getProjectName(), data.getCompanyName());
            openPosition.setProjectName(project);
            log.info("[SMART_VACANCY_OPENING] Проект позиции: {}", project != null ? (project.getProjectName() + " (ID=" + project.getId() + ")") : "НЕ НАЙДЕН");

            // Поиск / привязка типа позиции
            String posName = data.getPositionTypeName() != null ? data.getPositionTypeName() : openPosition.getVacansyName();
            Position positionType = findOrCreatePositionType(posName);
            openPosition.setPositionType(positionType);
            log.info("[SMART_VACANCY_OPENING] Тип позиции: {}", positionType != null ? (positionType.getPositionRuName() + " (ID=" + positionType.getId() + ")") : "НЕ НАЙДЕН");

            // Поиск / привязка грейда
            if (data.getGradeName() != null) {
                Grade grade = findGrade(data.getGradeName());
                if (grade != null) {
                    openPosition.setGrade(grade);
                    log.info("[SMART_VACANCY_OPENING] Грейд позиции: {} (ID={})", grade.getGradeName(), grade.getId());
                }
            }

            // Поиск / привязка города
            if (data.getCityName() != null) {
                City city = findCity(data.getCityName());
                if (city != null) {
                    openPosition.setCityPosition(city);
                    log.info("[SMART_VACANCY_OPENING] Город позиции: {} (ID={})", city.getCityRuName(), city.getId());
                }
            }

            // Создание дерева навыков SkillTree для позиции
            if (data.getRequiredSkills() != null && !data.getRequiredSkills().isEmpty()) {
                log.info("[SMART_VACANCY_OPENING] Привязка навыков к вакансии (всего: {})...", data.getRequiredSkills().size());
                int count = 0;
                for (String skillName : data.getRequiredSkills()) {
                    if (skillName == null || skillName.trim().isEmpty()) continue;
                    String cleanSkill = truncate(cleanTitle(skillName), 80);
                    if (cleanSkill.isEmpty()) continue;

                    SkillTree skill = metadata.create(SkillTree.class);
                    skill.setSkillName(cleanSkill);
                    skill.setOpenPosition(openPosition);
                    commitContext.addInstanceToCommit(skill);
                    count++;
                }
                log.info("[SMART_VACANCY_OPENING] Создано {} сущностей SkillTree для вакансии", count);
            }

            log.info("[SMART_VACANCY_OPENING] Отправка CommitContext в DataManager...");
            dataManager.commit(commitContext);
            log.info("[SMART_VACANCY_OPENING] ✓ Вакансия успешно зафиксирована в БД! ID={}, vacansyID={}, name='{}', signDraft=true, priority={}",
                    openPosition.getId(), openPosition.getVacansyID(), openPosition.getVacansyName(), openPosition.getPriority());

            result.setSuccess(true);
            result.setOpenPosition(openPosition);
            result.setMessage("Черновик вакансии «" + openPosition.getVacansyName() + "» успешно создан!");
        } catch (Exception e) {
            log.error("[SMART_VACANCY_OPENING] ✘ КРИТИЧЕСКАЯ ОШИБКА при сохранении вакансии в БД", e);
            result.setSuccess(false);
            result.setMessage("Ошибка: " + e.getMessage());
        }
        return result;
    }

    private Project findOrCreateProject(String projectName, String companyName) {
        String cleanName = projectName != null ? cleanTitle(projectName) : "";
        log.info("[SMART_VACANCY_OPENING] Поиск проекта: name='{}', company='{}'", cleanName, companyName);
        if (!cleanName.isEmpty()) {
            List<Project> list = dataManager.load(Project.class)
                    .query("select e from hunttech_Project e where lower(e.projectName) like lower(:name) and (e.projectIsClosed is null or e.projectIsClosed = false)")
                    .parameter("name", "%" + cleanName + "%")
                    .view("project-picker-view")
                    .maxResults(1)
                    .list();
            if (!list.isEmpty()) {
                log.info("[SMART_VACANCY_OPENING] Найден существующий открытый проект: '{}' (ID={})", list.get(0).getProjectName(), list.get(0).getId());
                return list.get(0);
            }
        }
        if (companyName != null && !companyName.trim().isEmpty()) {
            String cleanCompany = cleanTitle(companyName);
            List<Project> companyProjects = dataManager.load(Project.class)
                    .query("select e from hunttech_Project e where lower(e.projectName) like lower(:comp) and (e.projectIsClosed is null or e.projectIsClosed = false)")
                    .parameter("comp", "%" + cleanCompany + "%")
                    .view("project-picker-view")
                    .maxResults(1)
                    .list();
            if (!companyProjects.isEmpty()) {
                log.info("[SMART_VACANCY_OPENING] Найден открытый проект по компании '{}': '{}' (ID={})", cleanCompany, companyProjects.get(0).getProjectName(), companyProjects.get(0).getId());
                return companyProjects.get(0);
            }
        }
        // Поиск системного проекта по умолчанию
        List<Project> defaultList = dataManager.load(Project.class)
                .query("select e from hunttech_Project e where e.defaultProject = true and (e.projectIsClosed is null or e.projectIsClosed = false)")
                .view("project-picker-view")
                .maxResults(1)
                .list();
        if (!defaultList.isEmpty()) {
            log.info("[SMART_VACANCY_OPENING] Использован системный проект по умолчанию: '{}' (ID={})", defaultList.get(0).getProjectName(), defaultList.get(0).getId());
            return defaultList.get(0);
        }
        // Первый доступный открытый проект в системе
        List<Project> fallbackList = dataManager.load(Project.class)
                .query("select e from hunttech_Project e where (e.projectIsClosed is null or e.projectIsClosed = false) order by e.createTs desc")
                .view("project-picker-view")
                .maxResults(1)
                .list();
        if (!fallbackList.isEmpty()) {
            log.warn("[SMART_VACANCY_OPENING] Проект не найден, использован открытый проект: '{}' (ID={})", fallbackList.get(0).getProjectName(), fallbackList.get(0).getId());
            return fallbackList.get(0);
        }
        log.warn("[SMART_VACANCY_OPENING] В БД нет ни одного открытого проекта hunttech_Project");
        return null;
    }

    private Position findOrCreatePositionType(String positionName) {
        if (positionName == null || positionName.trim().isEmpty()) return null;
        String cleanName = cleanTitle(positionName);
        if (cleanName.isEmpty()) return null;

        // Строгое ограничение: колонка HUNTTECH_POSITION.POSITION_RU_NAME имеет тип varchar(80)
        String safeName = truncate(cleanName, 80);

        log.info("[SMART_VACANCY_OPENING] Поиск / создание типа должности: '{}'", safeName);
        List<Position> list = dataManager.load(Position.class)
                .query("select e from hunttech_Position e where lower(e.positionRuName) = lower(:name) or lower(e.positionEnName) = lower(:name)")
                .parameter("name", safeName)
                .view("position-picker-view")
                .maxResults(1)
                .list();
        if (!list.isEmpty()) {
            log.info("[SMART_VACANCY_OPENING] Найден тип должности: '{}' (ID={})", list.get(0).getPositionRuName(), list.get(0).getId());
            return list.get(0);
        }
        Position pos = metadata.create(Position.class);
        pos.setPositionRuName(safeName);
        dataManager.commit(pos);
        log.info("[SMART_VACANCY_OPENING] Создан новый тип должности: '{}' (ID={})", pos.getPositionRuName(), pos.getId());
        return pos;
    }

    private Grade findGrade(String gradeName) {
        if (gradeName == null) return null;
        String cleanGrade = cleanTitle(gradeName);
        if (cleanGrade.isEmpty()) return null;

        log.info("[SMART_VACANCY_OPENING] Поиск грейда: '{}'", cleanGrade);
        List<Grade> list = dataManager.load(Grade.class)
                .query("select e from hunttech_Grade e where lower(e.gradeName) like lower(:g)")
                .parameter("g", "%" + cleanGrade + "%")
                .view("_local")
                .maxResults(1)
                .list();
        return list.isEmpty() ? null : list.get(0);
    }

    private City findCity(String cityName) {
        if (cityName == null) return null;
        String cleanCity = cleanTitle(cityName);
        if (cleanCity.isEmpty()) return null;

        log.info("[SMART_VACANCY_OPENING] Поиск города: '{}'", cleanCity);
        List<City> list = dataManager.load(City.class)
                .query("select e from hunttech_City e where lower(e.cityRuName) like lower(:c)")
                .parameter("c", "%" + cleanCity + "%")
                .view("city-picker-view")
                .maxResults(1)
                .list();
        return list.isEmpty() ? null : list.get(0);
    }

    private static String cleanHtmlToPlainText(String text) {
        if (text == null || text.trim().isEmpty()) return "";
        String s = text.replaceAll("(?is)<script.*?</script>", " ")
                .replaceAll("(?is)<style.*?</style>", " ")
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</?(?:div|p|li|tr|h[1-6])[^>]*>", "\n")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&quot;", "\"")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&apos;", "'")
                .replaceAll("&#39;", "'");

        return s.replaceAll("\\r", "").replaceAll("\\n{3,}", "\n\n").trim();
    }

    private static String cleanTitle(String title) {
        if (title == null) return "";
        String clean = cleanHtmlToPlainText(title);
        // Удаляем эмодзи и спецсимволы в начале строки (например 🥇, 🆔, 🎯, 🔹, —)
        clean = clean.replaceAll("^[\\s\\p{Punct}\\p{So}\\p{Sc}\\p{Sm}\\p{Sk}]+", "").trim();
        clean = clean.replaceAll("[\\s:;,-]+$", "").trim();
        return clean;
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        String t = s.trim();
        return t.length() <= maxLen ? t : t.substring(0, maxLen).trim();
    }

    private static String preview(String s, int maxLen) {
        if (s == null) return "null";
        String oneLine = s.replaceAll("\\s+", " ").trim();
        return oneLine.length() <= maxLen ? oneLine : oneLine.substring(0, maxLen) + "...";
    }
}
