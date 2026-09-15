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

    private static final Pattern MD_DETECT_HEADER_PATTERN = Pattern.compile("(?m)^#{1,6}\\s+");
    private static final Pattern MD_DETECT_LIST_PATTERN = Pattern.compile("(?m)^\\s*[-*•]\\s+");
    private static final Pattern HTML_TAG_DETECT_PATTERN = Pattern.compile("(?i)<[a-z1-6]+(?:\\s+[^>]*?)?>");
    private static final Pattern MD_HEADER_LINE_PATTERN = Pattern.compile("^(#{1,6})\\s+(.*)$");
    private static final Pattern MD_UL_LINE_PATTERN = Pattern.compile("^\\s*[-*•]\\s+(.*)$");
    private static final Pattern MD_OL_LINE_PATTERN = Pattern.compile("^\\s*\\d+[.)]\\s+(.*)$");
    private static final Pattern MD_TABLE_SPLIT_PATTERN = Pattern.compile("^\\|?\\s*(:?-+:?\\s*\\|?)+\\s*$");
    private static final Pattern DANGEROUS_TAGS_PATTERN = Pattern.compile("(?is)<(script|iframe|object|embed|applet|svg|meta|link)[^>]*?>.*?</\\1>");
    private static final Pattern DANGEROUS_TAG_SINGLE_PATTERN = Pattern.compile("(?is)<(script|iframe|object|embed|applet|svg|meta|link)[^>]*?>");
    private static final Pattern JAVASCRIPT_URI_PATTERN = Pattern.compile("(?i)javascript:[^\"'\\s>]*");
    private static final Pattern ON_EVENT_HANDLER_PATTERN = Pattern.compile("(?i)[/\\s]on[a-z0-9_-]+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)");
    private static final Pattern URL_VACANCY_ID_PATTERN = Pattern.compile("(?iu)/(?:vacancy|openPosition|requisition)/((?=[A-Za-z0-9_-]*\\d)[A-Za-z0-9_-]{3,16})");
    private static final Pattern KEYWORD_VACANCY_ID_PATTERN = Pattern.compile("(?iu)(?:id\\s*вакансии|req[\\s_-]*id|requisition[\\s_-]*id|код\\s*вакансии|номер\\s*вакансии|вакансия\\s*№|заявка\\s*№|№\\s*заявки|№\\s*вакансии)\\s*[:#№=\\-\\s]+((?=[A-Za-z0-9_-]*\\d)[A-Za-z0-9_-]{2,16})");
    private static final Pattern HASHTAG_VACANCY_ID_PATTERN = Pattern.compile("(?iu)#((?:req|vac|id|job)-?\\d{2,12})\\b");

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
    public String fetchTextFromUrl(String urlString) {
        if (urlString == null || urlString.trim().isEmpty()) return "";
        String cleanUrl = urlString.trim();
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://" + cleanUrl;
        }

        log.info("[SMART_VACANCY_OPENING] HTTP GET запрос к странице вакансии: {}", cleanUrl);
        try {
            java.net.URL url = new java.net.URL(cleanUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(20000);
            conn.setInstanceFollowRedirects(true);

            int code = conn.getResponseCode();
            log.info("[SMART_VACANCY_OPENING] HTTP статус ответа для {}: {}", cleanUrl, code);
            if (code >= 400) {
                log.warn("[SMART_VACANCY_OPENING] HTTP ошибка {} при загрузке страницы: {}", code, cleanUrl);
                return "";
            }

            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                String structuredText = convertHtmlToStructuredText(sb.toString());
                log.info("[SMART_VACANCY_OPENING] Извлечен структурированный текст со страницы (длина: {} символов)", structuredText.length());
                return structuredText;
            }
        } catch (Exception e) {
            log.error("[SMART_VACANCY_OPENING] Ошибка при загрузке контента по ссылке " + cleanUrl + ": " + e.getMessage(), e);
            return "";
        }
    }

    public static String convertHtmlToStructuredText(String html) {
        if (html == null || html.trim().isEmpty()) return "";
        String s = html;

        // Извлекаем заголовок title, если есть
        String titlePrefix = "";
        Pattern titlePat = Pattern.compile("(?is)<title>(.*?)</title>");
        Matcher titleMat = titlePat.matcher(s);
        if (titleMat.find()) {
            titlePrefix = titleMat.group(1).trim() + "\n\n";
        }

        s = s.replaceAll("(?is)<script.*?</script>", " ")
                .replaceAll("(?is)<style.*?</style>", " ")
                .replaceAll("(?is)<!--.*?-->", " ");

        // Превращаем заголовки, параграфы и переносы в структурированные строки
        s = s.replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</?(?:div|p|h[1-6]|tr|thead|tbody)[^>]*>", "\n")
                .replaceAll("(?i)<li[^>]*>", "\n• ")
                .replaceAll("(?i)</?li>", "\n")
                .replaceAll("(?i)<td[^>]*>", " ")
                .replaceAll("(?i)</td>", "  ")
                .replaceAll("(?i)<b[^>]*>#", "\n\n#")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&quot;", "\"")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&apos;", "'")
                .replaceAll("&#39;", "'")
                .replaceAll("&#937[0-9];", " ");

        String result = (titlePrefix + s).replaceAll("\\r", "").replaceAll("[ \\t]+", " ").replaceAll("\\n{3,}", "\n\n").trim();
        return result;
    }

    private static boolean isUrl(String text) {
        if (text == null) return false;
        String trimmed = text.trim();
        return (trimmed.startsWith("http://") || trimmed.startsWith("https://")) && !trimmed.contains("\n") && trimmed.length() < 500;
    }

    @Override
    public SmartOpenPositionParsedData parseVacancyText(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            log.warn("[SMART_VACANCY_OPENING] Запрос на парсинг с пустым текстом вакансии");
            SmartOpenPositionParsedData emptyData = new SmartOpenPositionParsedData();
            emptyData.setRawText("");
            return emptyData;
        }

        // Если передана интернет-ссылка (например, https://need.ssp-soft.com/details?id=62630) — сначала скачиваем страницу
        String actualSourceText = rawText.trim();
        if (isUrl(actualSourceText)) {
            log.info("[SMART_VACANCY_OPENING] Обнаружена интернет-ссылка в качестве входных данных: '{}'. Загрузка содержимого по HTTP...", actualSourceText);
            String fetchedFromUrl = fetchTextFromUrl(actualSourceText);
            if (!fetchedFromUrl.isEmpty()) {
                actualSourceText = fetchedFromUrl + "\n\n[Источник: " + actualSourceText + "]";
            }
        }

        // Предотвращение HTML-разметки от RichTextArea: очищаем до форматированного plain text
        String cleanedPlainText = cleanHtmlToPlainText(actualSourceText);
        String textForAi = !cleanedPlainText.isEmpty() ? cleanedPlainText : actualSourceText;

        log.info("[SMART_VACANCY_OPENING] >>> Начало парсинга текста вакансии (длина исходного: {}, очищенного: {} символов). Превью: [{}]",
                actualSourceText.length(), textForAi.length(), preview(textForAi, 150));

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

                // 1.0 ID вакансии (поддержка вариаций id, vacansyId, vacancyId, requisitionId)
                String vacId = getJsonString(json, "vacansyId", "vacancyId", "id", "vacansyID", "requisitionId", "externalId", "reqId");
                if (vacId != null && !vacId.isEmpty()) {
                    data.setVacansyID(cleanVacansyId(vacId));
                    log.info("[SMART_VACANCY_OPENING] Извлечен ID вакансии из AI JSON: '{}'", data.getVacansyID());
                }

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

                // 1.13 Ставка ИП и комментарий по зарплате
                BigDecimal sIE = getJsonBigDecimal(json, "salaryIE", "rateIE", "ieSalary");
                if (sIE != null) data.setSalaryIE(sIE);
                String sCom = getJsonString(json, "salaryComment", "paymentConditions", "salaryConditions");
                if (sCom != null) data.setSalaryComment(cleanHtmlToPlainText(sCom));

                // 1.14 Описание проекта
                String prjShort = getJsonString(json, "projectShortDescription", "shortProjectDescription");
                if (prjShort != null) data.setProjectShortDescription(cleanHtmlToPlainText(prjShort));
                String prjFull = getJsonString(json, "projectDescription", "projectFullDescription");
                if (prjFull != null) data.setProjectFullDescription(cleanHtmlToPlainText(prjFull));

                // 1.15 Тестовое задание и памятка для собеседования
                String exercise = getJsonString(json, "exercise", "testExercise", "testTask");
                if (exercise != null) data.setExercise(cleanHtmlToPlainText(exercise));
                String memo = getJsonString(json, "memoForInterview", "memo", "interviewMemo");
                if (memo != null) data.setMemoForInterview(cleanHtmlToPlainText(memo));

                // 1.16 Разделы из AI-промптов: чек-лист, карта поиска, план собеседования (строго валидный HTML)
                String chkText = getJsonString(json, "interviewChecklist", "checklistText");
                if (chkText != null && !chkText.trim().isEmpty()) {
                    data.setInterviewChecklist(formatAsCleanHtml(chkText));
                }
                String sMap = getJsonString(json, "searchMap", "sourcingMap");
                if (sMap != null && !sMap.trim().isEmpty()) {
                    data.setSearchMap(formatAsCleanHtml(sMap));
                }
                String iPlan = getJsonString(json, "interviewPlan", "interviewStructure");
                if (iPlan != null && !iPlan.trim().isEmpty()) {
                    data.setInterviewPlan(formatAsCleanHtml(iPlan));
                }

                // 1.17 Стандартизированное описание вакансии (строго валидный HTML)
                String fullCommentJson = getJsonString(json, "comment", "standardizedDescription");
                if (fullCommentJson != null && fullCommentJson.length() > 50) {
                    data.setComment(formatAsCleanHtml(fullCommentJson));
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

        // 1.18 Резервное извлечение ID вакансии/заявки регулярными выражениями из текста и URL (если AI не определил)
        if (data.getVacansyID() == null || data.getVacansyID().isEmpty()) {
            String extractedId = extractVacancyIdFromTextOrUrl(actualSourceText);
            if (extractedId != null && !extractedId.isEmpty()) {
                data.setVacansyID(extractedId);
                log.info("[SMART_VACANCY_OPENING] ID вакансии извлечен регулярным выражением: '{}'", data.getVacansyID());
            }
        }

        // 2. Эвристический fallback-парсинг, если AI недоступен или вернул пустые поля
        if (!parsedByAi || data.getVacansyName() == null || data.getVacansyName().isEmpty()) {
            log.info("[SMART_VACANCY_OPENING] Запуск эвристического fallback-парсера...");
            fallbackHeuristicParse(textForAi, data);
            log.info("[SMART_VACANCY_OPENING] Результаты эвристического парсинга: vacansyName='{}', vacansyID='{}', remoteWork={}, salaryMin={}, salaryMax={}, grade='{}', skills={}",
                    data.getVacansyName(), data.getVacansyID(), data.getRemoteWork(), data.getSalaryMin(), data.getSalaryMax(), data.getGradeName(), data.getRequiredSkills());
        }

        if (data.getComment() == null || data.getComment().trim().isEmpty()) {
            data.setComment(textForAi);
        }

        // 3. Формирование канонического наименования по алгоритму кнопки «Генерировать» из OpenPositionEdit
        data.setRawVacansyName(data.getVacansyName());
        if (dataManager != null) {
            Position posPreview = findBestMatchingPositionType(data.getPositionTypeName() != null ? data.getPositionTypeName() : data.getRawVacansyName());
            Grade grPreview = findGrade(data.getGradeName());
            Project prjPreview = findExistingOpenProject(data.getProjectName(), data.getCompanyName());
            if (prjPreview == null && data.getProjectName() != null && !data.getProjectName().trim().isEmpty() && metadata != null) {
                prjPreview = metadata.create(Project.class);
                prjPreview.setProjectName(cleanTitle(data.getProjectName()));
            }
            City cityPreview = findCity(data.getCityName());
            if (posPreview != null) {
                String canonical = generateCanonicalVacancyName(grPreview, posPreview, prjPreview, cityPreview, null);
                if (canonical != null && !canonical.trim().isEmpty()) {
                    log.info("[SMART_VACANCY_OPENING] Установлено каноническое название вакансии для превью: '{}'", canonical);
                    data.setVacansyName(canonical);
                }
            }
        }

        // Проверка обязательных полей
        if (data.getVacansyName() == null || data.getVacansyName().isEmpty()) {
            data.setVacansyName("Новая открытая вакансия");
            data.getMissingFields().add("Название вакансии определено автоматически по умолчанию");
            log.warn("[SMART_VACANCY_OPENING] Название вакансии не удалось извлечь, установлено имя по умолчанию");
        }
        if (data.getProjectName() == null || data.getProjectName().isEmpty()) {
            data.getMissingFields().add("Проект не указан в тексте (будет создан проект «Новый проект» или выбран существующий)");
            log.info("[SMART_VACANCY_OPENING] Проект не указан в описании вакансии");
        }

        // 4. Гарантированное формирование 4 ключевых артефактов стандарта HuntTech
        ensureFourArtifacts(data, textForAi);

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
        String currentSection = "";
        List<String> obligatories = new ArrayList<>();
        List<String> desirables = new ArrayList<>();
        List<String> taskLines = new ArrayList<>();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            String lower = trimmed.toLowerCase();
            if (lower.startsWith("#проект") || lower.equals("проект")) {
                currentSection = "PROJECT";
                continue;
            } else if (lower.startsWith("#условия") || lower.equals("условия")) {
                currentSection = "CONDITIONS";
                continue;
            } else if (lower.startsWith("#позиция") || lower.equals("позиция")) {
                currentSection = "POSITION";
                continue;
            } else if (lower.startsWith("#обязательно") || lower.startsWith("#требования")) {
                currentSection = "OBLIGATORY";
                continue;
            } else if (lower.startsWith("#желательно")) {
                currentSection = "DESIRABLE";
                continue;
            } else if (lower.startsWith("#задачи") || lower.startsWith("#обязанности")) {
                currentSection = "TASKS";
                continue;
            }

            // Формат работы (проверяется для любой строки текста)
            if (lower.contains("гибрид") || lower.contains("hybrid")) {
                data.setRemoteWork(2);
            } else if (lower.contains("офис") || lower.contains("office") || lower.contains("onsite")) {
                data.setRemoteWork(0);
            } else if (lower.contains("удален") || lower.contains("remote")) {
                data.setRemoteWork(1);
            }

            // Грейд (проверяется для любой строки текста)
            if (data.getGradeName() == null) {
                if (lower.contains("middle+")) data.setGradeName("Middle+");
                else if (lower.contains("senior") || lower.contains("сеньор")) data.setGradeName("Senior");
                else if (lower.contains("middle") || lower.contains("мидл")) data.setGradeName("Middle");
                else if (lower.contains("lead") || lower.contains("тимлид") || lower.contains("лид")) data.setGradeName("Lead");
                else if (lower.contains("junior") || lower.contains("джуниор")) data.setGradeName("Junior");
            }

            // Позиция / вакансия из префиксов
            if (data.getVacansyName() == null) {
                if (lower.startsWith("вакансия:") || lower.startsWith("позиция:") || lower.startsWith("должность:")) {
                    data.setVacansyName(cleanTitle(trimmed.substring(trimmed.indexOf(":") + 1).trim()));
                } else if (trimmed.length() < 80 && (lower.contains("developer") || lower.contains("engineer") || lower.contains("разработчик") || lower.contains("аналитик") || lower.contains("тестировщик") || lower.contains("дизайнер") || lower.contains("менеджер") || lower.contains("lead") || lower.contains("devops") || lower.contains("architect"))) {
                    if (!lower.startsWith("опыт") && !lower.startsWith("требован") && !lower.startsWith("стек") && !lower.startsWith("компания")) {
                        data.setVacansyName(cleanTitle(trimmed));
                    }
                }
            }

            // Парсинг секций формата SSP Soft и типовых заявок
            if ("PROJECT".equals(currentSection)) {
                String prjCandidate = cleanTitle(trimmed.replaceAll("^[-*•—\\s]+", ""));
                if (data.getProjectName() == null && !prjCandidate.isEmpty() && prjCandidate.length() > 2) {
                    data.setProjectName(prjCandidate);
                    data.setProjectFullDescription(trimmed);
                }
            } else if ("POSITION".equals(currentSection)) {
                String posCandidate = trimmed.replaceAll("^[-*•—\\s]+", "").replaceAll("(?i)^(?:роль|позиция|должность)[:\\s]*", "").trim();
                if (!posCandidate.isEmpty() && data.getVacansyName() == null) {
                    data.setVacansyName(cleanTitle(posCandidate));
                }
            } else if ("CONDITIONS".equals(currentSection)) {
                if (lower.contains("москва") || lower.contains("мск") || lower.contains("рф")) {
                    if (data.getCityName() == null) {
                        data.setCityName(lower.contains("москва") ? "Москва" : "РФ");
                    }
                }
            } else if ("OBLIGATORY".equals(currentSection)) {
                String item = cleanTitle(trimmed.replaceAll("^[-*•—\\s]+", ""));
                if (!item.isEmpty() && item.length() > 2) {
                    obligatories.add(item);
                }
            } else if ("DESIRABLE".equals(currentSection)) {
                String item = cleanTitle(trimmed.replaceAll("^[-*•—\\s]+", ""));
                if (!item.isEmpty() && item.length() > 2) {
                    desirables.add(item);
                }
            } else if ("TASKS".equals(currentSection)) {
                String item = cleanTitle(trimmed.replaceAll("^[-*•—\\s]+", ""));
                if (!item.isEmpty() && item.length() > 2) {
                    taskLines.add(item);
                }
            }

            // Поиск зарплаты / ставки
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

            // Опыт работы
            Pattern expPattern = Pattern.compile("(?:опыт|стаж).*?(\\d+)\\s*(?:лет|года|год)", Pattern.CASE_INSENSITIVE);
            Matcher expMatcher = expPattern.matcher(trimmed);
            if (expMatcher.find()) {
                try {
                    data.setWorkExperience(Integer.parseInt(expMatcher.group(1)));
                } catch (Exception ignored) {}
            }
        }

        // Если заголовок не был найден в явных секциях
        if (data.getVacansyName() == null) {
            for (String l : lines) {
                String cleanL = cleanTitle(l);
                if (cleanL.toLowerCase().startsWith("запрос") || cleanL.toLowerCase().startsWith("id")) {
                    String afterId = cleanL.replaceAll("(?i)^(?:запрос|id|🆔)[\\s:]*\\d+[:\\s-]*", "").trim();
                    if (!afterId.isEmpty() && afterId.length() <= 100) {
                        data.setVacansyName(afterId);
                        break;
                    }
                }
                if (!cleanL.isEmpty() && cleanL.length() <= 80 && !cleanL.matches("(?i)^(?:[🆔\\s]*ID|🆔|запрос|номер).*")) {
                    data.setVacansyName(cleanL);
                    break;
                }
            }
        }

        if (data.getChecklist() == null || data.getChecklist().isEmpty()) {
            data.setChecklist(obligatories);
        }

        // Поиск навыков по ключевым словам и из обязательных секций
        List<String> foundSkills = new ArrayList<>();
        if (!obligatories.isEmpty()) {
            for (String ob : obligatories) {
                if (ob.length() <= 40 && !ob.contains(" ")) {
                    foundSkills.add(ob);
                }
            }
        }
        String[] popularSkills = {"Java", "Spring", "Spring Boot", "Kotlin", "Python", "1C", "1С", "1С ERP", "PostgreSQL", "Docker", "Kubernetes", "React", "TypeScript", "JavaScript", "Go", "C#", "Kafka", "Redis", "Git", "CI/CD", "Linux", "SQL", "BPMN", "REST API", "Jira", "AI-Workflow"};
        for (String skill : popularSkills) {
            if (Pattern.compile("\\b" + Pattern.quote(skill) + "\\b", Pattern.CASE_INSENSITIVE).matcher(cleanText).find()) {
                if (!foundSkills.contains(skill)) {
                    foundSkills.add(skill);
                }
            }
        }
        if (data.getRequiredSkills() == null || data.getRequiredSkills().isEmpty()) {
            data.setRequiredSkills(foundSkills);
        }

        // Извлечение ID вакансии/заявки в fallback (если еще не извлечен)
        if (data.getVacansyID() == null || data.getVacansyID().isEmpty()) {
            String extractedId = extractVacancyIdFromTextOrUrl(text);
            if (extractedId != null && !extractedId.isEmpty()) {
                data.setVacansyID(extractedId);
            }
        }

        // Если должность не определена явно, используем название вакансии
        if (data.getPositionTypeName() == null && data.getVacansyName() != null) {
            data.setPositionTypeName(cleanTitle(data.getVacansyName()));
        }
    }

    private void ensureFourArtifacts(SmartOpenPositionParsedData data, String sourceText) {
        if (data == null) return;

        // 1. Стандартизированное описание вакансии (14 обязательных разделов стандарта HuntTech)
        if (data.getComment() == null || data.getComment().trim().isEmpty() || !data.getComment().contains("1. Роль") || data.getComment().length() < 200) {
            data.setComment(buildStandardizedDescription(data, sourceText));
        }

        // 2. Чек-лист первичного скрининга кандидата (must-have)
        if (data.getInterviewChecklist() == null || data.getInterviewChecklist().trim().isEmpty() || data.getInterviewChecklist().length() < 100) {
            data.setInterviewChecklist(buildInterviewChecklist(data));
        }

        // 3. Карта поиска / инструкция сорсеру
        if (data.getSearchMap() == null || data.getSearchMap().trim().isEmpty() || data.getSearchMap().length() < 100) {
            data.setSearchMap(buildSearchMap(data));
        }

        // 4. План продающего собеседования (6 блоков)
        if (data.getInterviewPlan() == null || data.getInterviewPlan().trim().isEmpty() || data.getInterviewPlan().length() < 100) {
            data.setInterviewPlan(buildInterviewPlan(data));
        }
    }

    private String buildStandardizedDescription(SmartOpenPositionParsedData data, String sourceText) {
        String role = data.getPositionTypeName() != null ? data.getPositionTypeName() : (data.getVacansyName() != null ? data.getVacansyName() : "НЕТ ДАННЫХ, УТОЧНЯЙТЕ У РЕКРУТЕРА НА СОБЕСЕДОВАНИИ.");
        String grade = (data.getGradeName() != null ? data.getGradeName() : "Middle+") + ", опыт от " + (data.getWorkExperience() != null ? data.getWorkExperience() : 3) + " лет";
        String proj = data.getProjectFullDescription() != null ? data.getProjectFullDescription() : (data.getProjectName() != null ? data.getProjectName() : "НЕТ ДАННЫХ, УТОЧНЯЙТЕ У РЕКРУТЕРА НА СОБЕСЕДОВАНИИ.");

        StringBuilder sb = new StringBuilder();
        sb.append("<h3>1. Роль, название должности</h3>\n<p>").append(escapeHtml(role)).append("</p>\n\n");
        sb.append("<h3>2. Грейд, опыт работы</h3>\n<p>").append(escapeHtml(grade)).append("</p>\n\n");
        sb.append("<h3>3. Описание проекта</h3>\n<p>");
        if (data.getProjectFullDescription() != null && !data.getProjectFullDescription().trim().isEmpty()) {
            sb.append(escapeHtml(data.getProjectFullDescription()));
        } else if (data.getProjectShortDescription() != null && !data.getProjectShortDescription().trim().isEmpty()) {
            sb.append(escapeHtml(data.getProjectShortDescription()));
        } else {
            sb.append(escapeHtml(proj));
        }
        sb.append("</p>\n\n");

        sb.append("<h3>4. Обязанности</h3>\n");
        if (data.getChecklist() != null && !data.getChecklist().isEmpty()) {
            sb.append("<ul>\n");
            for (String ch : data.getChecklist()) {
                sb.append("  <li>").append(escapeHtml(ch)).append("</li>\n");
            }
            sb.append("</ul>\n\n");
        } else {
            sb.append("<p>НЕТ ДАННЫХ, УТОЧНЯЙТЕ У РЕКРУТЕРА НА СОБЕСЕДОВАНИИ.</p>\n\n");
        }

        sb.append("<h3>5. Описание требований к вакансии (Хард-скиллы) обязательные</h3>\n");
        if (data.getRequiredSkills() != null && !data.getRequiredSkills().isEmpty()) {
            sb.append("<ul>\n");
            for (String s : data.getRequiredSkills()) {
                sb.append("  <li>").append(escapeHtml(s)).append("</li>\n");
            }
            sb.append("</ul>\n\n");
        } else {
            sb.append("<p>НЕТ ДАННЫХ, УТОЧНЯЙТЕ У РЕКРУТЕРА НА СОБЕСЕДОВАНИИ.</p>\n\n");
        }

        sb.append("<h3>6. Описание требований к вакансии (Хард-скиллы) желательные</h3>\n");
        sb.append("<ul>\n");
        sb.append("  <li>Опыт работы в кросс-функциональных командах</li>\n");
        sb.append("  <li>Понимание CI/CD и современных процессов разработки</li>\n");
        sb.append("</ul>\n\n");

        sb.append("<h3>7. Требования к софт-скиллам</h3>\n");
        sb.append("<ul>\n");
        sb.append("  <li>Высокие коммуникативные навыки, ответственность, системность мышления</li>\n");
        sb.append("  <li>Умение работать в распределенной команде и договариваться с заказчиками</li>\n");
        sb.append("</ul>\n\n");

        sb.append("<h3>8. Дополнительная информация</h3>\n");
        String loc = data.getCityName() != null ? ("Локация / география: " + data.getCityName() + " (РФ, РБ). ") : "Локация: РФ, РБ. ";
        sb.append("<ul>\n");
        sb.append("  <li>").append(escapeHtml(loc)).append("Часовой пояс: МСК ± 2 часа.</li>\n");
        sb.append("  <li>Формат оформления: по ТК РФ или ИП/ГПХ. Долгосрочное сотрудничество.</li>\n");
        sb.append("  <li>Проверки: прохождение внутренней проверки службы безопасности заказчика.</li>\n");
        sb.append("</ul>\n\n");

        sb.append("<h3>9. Условия работы</h3>\n");
        String rw = (data.getRemoteWork() != null && data.getRemoteWork() == 1) ? "Удаленный формат работы" : ((data.getRemoteWork() != null && data.getRemoteWork() == 2) ? "Гибридный формат" : "Работа в офисе");
        sb.append("<ul>\n");
        sb.append("  <li>").append(escapeHtml(rw)).append("</li>\n");
        sb.append("  <li>График: полная занятость (МСК ± 2 часа)</li>\n");
        sb.append("  <li>Ставка обсуждается индивидуально с успешным кандидатом</li>\n");
        sb.append("</ul>\n\n");

        sb.append("<h3>10. Список обязательных знаний технологий</h3>\n<p>");
        sb.append(!data.getRequiredSkills().isEmpty() ? escapeHtml(String.join(", ", data.getRequiredSkills())) : "НЕТ ДАННЫХ, УТОЧНЯЙТЕ У РЕКРУТЕРА НА СОБЕСЕДОВАНИИ.");
        sb.append("</p>\n\n");

        sb.append("<h3>11. Список желательных знаний технологий</h3>\n<p>Git, Docker, Jira, Confluence, Linux</p>\n\n");

        sb.append("<h3>12. Требования к резюме</h3>\n");
        sb.append("<ul>\n");
        sb.append("  <li>Экспресс-скрининг (30 секунд): в опыте за последние 1-2 года должен быть явно подтвержден Core-стек (")
                .append(!data.getRequiredSkills().isEmpty() ? escapeHtml(String.join(", ", data.getRequiredSkills())) : "ключевые технологии роли")
                .append(").</li>\n");
        sb.append("  <li>Описание коммерческих проектов с указанием решаемых задач, архитектурных подходов и личного вклада кандидата.</li>\n");
        sb.append("  <li>В резюме обязательно должны быть указаны текущая локация, гражданство и готовность к полной занятости.</li>\n");
        sb.append("</ul>\n\n");

        sb.append("<h3>13. Собеседование</h3>\n");
        sb.append("<ol>\n");
        sb.append("  <li>Первичное скрининг-интервью с рекрутером (30 мин).</li>\n");
        sb.append("  <li>Техническое интервью с лидом проекта (60 мин).</li>\n");
        sb.append("</ol>\n\n");

        sb.append("<h3>14. Рекомендации рекрутеру</h3>\n");
        sb.append("<p><b>Elevator Pitch:</b> Ищем опытного ").append(escapeHtml(role)).append(" в проект ")
                .append(data.getProjectName() != null ? escapeHtml(data.getProjectName()) : "заказчика")
                .append(" на полный удаленный формат без лишней бюрократии с современным стеком задач.</p>\n");
        sb.append("<p><b>Selling Points:</b> полная стабильная удаленка, масштабный продукт, быстрое согласование кандидатов, прозрачные выплаты.</p>\n");
        sb.append("<p><b>Красные флаги отсева:</b> отсутствие подтвержденного практического опыта с Core-стеком, частая смена мест (менее 6 мес.), опыт только в саппорте при требовании разработки с нуля.</p>\n");
        String topSkill = !data.getRequiredSkills().isEmpty() ? data.getRequiredSkills().get(0) : "ключевой технологии";
        sb.append("<p><b>Контрольный вопрос для скрининга:</b> «Расскажите о самом сложном кейсе использования ")
                .append(escapeHtml(topSkill))
                .append(" на ваших последних проектах: какую проблему решали и каков был результат?»</p>");
        return sb.toString();
    }

    private String buildInterviewChecklist(SmartOpenPositionParsedData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h3>Чек-лист первичного скрининга кандидата (Must-Have критерии за 60 секунд)</h3>\n");
        sb.append("<table border=\"1\" cellpadding=\"6\" cellspacing=\"0\" style=\"border-collapse: collapse; width: 100%; font-size: 13px;\">\n");
        sb.append("<thead>\n");
        sb.append("  <tr style=\"background-color: #f1f5f9; text-align: left;\">\n");
        sb.append("    <th style=\"width: 18%;\">Группа навыков</th>\n");
        sb.append("    <th style=\"width: 38%;\">Конкретный навык / Вопрос для проверки</th>\n");
        sb.append("    <th style=\"width: 14%;\">Ключевой навык?</th>\n");
        sb.append("    <th style=\"width: 12%;\">Отметка рекрутера</th>\n");
        sb.append("    <th style=\"width: 18%;\">Где искать в резюме?</th>\n");
        sb.append("  </tr>\n");
        sb.append("</thead>\n");
        sb.append("<tbody>\n");

        // 1. Административные фильтры
        String loc = data.getCityName() != null ? escapeHtml(data.getCityName()) : "РФ, РБ";
        sb.append("  <tr>\n");
        sb.append("    <td rowspan=\"2\"><b>1. Административные фильтры</b></td>\n");
        sb.append("    <td>Проживание: ").append(loc).append(", рабочий график строго по МСК (± 2 часа).<br>• <i>Вопрос:</i> «Подходит ли вам график работы по Москве и где вы территориально находитесь?»<br>• <i>Маркер зачета:</i> резидент РФ/РБ, доступен в рабочие часы МСК.<br>• <i>Красный флаг:</i> таймзона &gt; ±3 ч, закрытые локации.</td>\n");
        sb.append("    <td><b style=\"color: #b91c1c;\">Да (Блокирующий)</b></td>\n");
        sb.append("    <td></td>\n");
        sb.append("    <td>Шапка резюме, контакты</td>\n");
        sb.append("  </tr>\n");
        sb.append("  <tr>\n");
        sb.append("    <td>Формат оформления: готовность к ИП / ГПХ или ТК РФ на полный день (40 ч/нед).<br>• <i>Вопрос:</i> «Рассматриваете ли оформление по ИП/ГПХ и готовы ли к полной загрузке без совмещения?»<br>• <i>Маркер:</i> готовность к сотрудничеству full-time.<br>• <i>Красный флаг:</i> только part-time или оверлап.</td>\n");
        sb.append("    <td><b style=\"color: #b91c1c;\">Да (Блокирующий)</b></td>\n");
        sb.append("    <td></td>\n");
        sb.append("    <td>Желаемые условия, занятость</td>\n");
        sb.append("  </tr>\n");

        // 2. Опыт и грейд
        String role = data.getPositionTypeName() != null ? escapeHtml(data.getPositionTypeName()) : (data.getVacansyName() != null ? escapeHtml(data.getVacansyName()) : "специалиста");
        String expYears = (data.getWorkExperience() != null ? data.getWorkExperience() : 3) + "+ лет";
        sb.append("  <tr>\n");
        sb.append("    <td><b>2. Опыт и грейд</b></td>\n");
        sb.append("    <td>Подтвержденный коммерческий опыт в роли ").append(role).append(" от ").append(expYears).append(".<br>• <i>Вопрос:</i> «Сколько лет решаете подобные задачи на коммерческих проектах в продакшене?»<br>• <i>Маркер:</i> непрерывный релевантный опыт.<br>• <i>Красный флаг:</i> суммарный стаж менее требуемого или большой перерыв.</td>\n");
        sb.append("    <td><b style=\"color: #b91c1c;\">Да (Блокирующий)</b></td>\n");
        sb.append("    <td></td>\n");
        sb.append("    <td>Раздел «Опыт работы» за 3–5 лет</td>\n");
        sb.append("  </tr>\n");

        // 3. Core-стек
        if (data.getRequiredSkills() != null && !data.getRequiredSkills().isEmpty()) {
            boolean firstSkill = true;
            for (String skill : data.getRequiredSkills()) {
                String escSkill = escapeHtml(skill);
                sb.append("  <tr>\n");
                sb.append("    <td>").append(firstSkill ? "<b>3. Ключевой Core-стек</b>" : "").append("</td>\n");
                firstSkill = false;
                sb.append("    <td>Уверенное практическое владение ").append(escSkill).append(".<br>• <i>Вопрос:</i> «С какими версиями ").append(escSkill).append(" и архитектурными задачами сталкивались на последнем месте?»<br>• <i>Маркер:</i> уверенно называет технологии, компоненты и сценарии применения.<br>• <i>Красный флаг:</i> поверхностные знания, только курсы без коммерческой практики.</td>\n");
                sb.append("    <td><b style=\"color: #b91c1c;\">Да (Блокирующий)</b></td>\n");
                sb.append("    <td></td>\n");
                sb.append("    <td>Последние 1–2 места работы (2024–2026 гг.)</td>\n");
                sb.append("  </tr>\n");
            }
        } else {
            sb.append("  <tr>\n");
            sb.append("    <td><b>3. Ключевой Core-стек</b></td>\n");
            sb.append("    <td>Практический опыт решения производственных задач по профилю позиции.<br>• <i>Вопрос:</i> «Какие основные технологии использовали в недавних проектах?»</td>\n");
            sb.append("    <td><b style=\"color: #b91c1c;\">Да (Блокирующий)</b></td>\n");
            sb.append("    <td></td>\n");
            sb.append("    <td>Проекты за последние 2 года</td>\n");
            sb.append("  </tr>\n");
        }

        // 4. Архитектура и базы данных
        sb.append("  <tr>\n");
        sb.append("    <td><b>4. Архитектура и БД</b></td>\n");
        sb.append("    <td>Проектирование API (REST/gRPC), интеграции сервисов, работа с СУБД и структурами данных.<br>• <i>Вопрос:</i> «Как проектировали контракты взаимодействия и как оптимизировали запросы к БД?»<br>• <i>Маркер:</i> понимание нормализации, индексов, обработки ошибок API.<br>• <i>Красный флаг:</i> только простой CRUD без интеграций.</td>\n");
        sb.append("    <td>Да</td>\n");
        sb.append("    <td></td>\n");
        sb.append("    <td>Описания проектов, стек</td>\n");
        sb.append("  </tr>\n");

        // 5. Инженерные практики
        sb.append("  <tr>\n");
        sb.append("    <td><b>5. Инженерные практики</b></td>\n");
        sb.append("    <td>Работа с Git, участие в код-ревью, понимание CI/CD пайплайнов и командных процессов (Agile/Scrum).<br>• <i>Вопрос:</i> «Какой процесс код-ревью и доставки кода в прод был выстроен в вашей команде?»<br>• <i>Маркер:</i> знание Git flow, автотестов, культуры разработки.</td>\n");
        sb.append("    <td>Да</td>\n");
        sb.append("    <td></td>\n");
        sb.append("    <td>Инструменты, обязанности</td>\n");
        sb.append("  </tr>\n");

        sb.append("</tbody>\n");
        sb.append("</table>");
        return sb.toString();
    }

    private String buildSearchMap(SmartOpenPositionParsedData data) {
        String role = data.getPositionTypeName() != null ? escapeHtml(data.getPositionTypeName()) : (data.getVacansyName() != null ? escapeHtml(data.getVacansyName()) : "IT-специалист");
        String grade = data.getGradeName() != null ? escapeHtml(data.getGradeName()) : "Middle+ / Senior";
        String location = data.getCityName() != null ? escapeHtml(data.getCityName()) : "РФ (Удаленно)";
        String skillsListStr = (data.getRequiredSkills() != null && !data.getRequiredSkills().isEmpty())
                ? escapeHtml(String.join(", ", data.getRequiredSkills())) : "Java";
        String booleanSkillsStr = (data.getRequiredSkills() != null && !data.getRequiredSkills().isEmpty())
                ? escapeHtml(String.join("\" AND \"", data.getRequiredSkills())) : "Java";
        String firstSkill = (data.getRequiredSkills() != null && !data.getRequiredSkills().isEmpty())
                ? escapeHtml(data.getRequiredSkills().get(0)) : "Java";

        StringBuilder sb = new StringBuilder();
        sb.append("<h3>Карта поиска кандидата (Search Map) / Инструкция сорсеру и рекрутеру HuntTech</h3>\n\n");

        sb.append("<h4>1. Целевой профиль кандидата (Target Profile)</h4>\n");
        sb.append("<ul>\n");
        sb.append("  <li><b>Целевая роль:</b> ").append(role).append("</li>\n");
        sb.append("  <li><b>Альтернативные наименования:</b> ").append(role).append(", Senior ").append(role)
                .append(", ").append(firstSkill).append(" Developer, Инженер-разработчик ").append(firstSkill).append("</li>\n");
        sb.append("  <li><b>Целевой грейд и подтвержденный стаж:</b> ").append(grade)
                .append(", коммерческий опыт от ").append(data.getWorkExperience() != null ? data.getWorkExperience() : 3).append(" лет в профильном стеке</li>\n");
        sb.append("  <li><b>Локация и часовой пояс:</b> ").append(location).append(", рабочий день в интервале 09:00–18:00 или 10:00–19:00 по МСК</li>\n");
        sb.append("</ul>\n\n");

        sb.append("<h4>2. Экспресс-фильтр: «Рентген резюме за 15 секунд»</h4>\n");
        sb.append("<p><b>Что обязательно должно быть в опыте за последние 1–2 года (Must-have маркеры):</b></p>\n");
        sb.append("<ul>\n");
        sb.append("  <li>Реальный коммерческий опыт с ключевым стеком: ").append(skillsListStr).append(";</li>\n");
        sb.append("  <li>Решение прикладных задач: проектирование модулей, разработка интеграций, оптимизация производительности;</li>\n");
        sb.append("  <li>Роль в команде: самостоятельная реализация фич, участие в архитектурных обсуждениях и код-ревью.</li>\n");
        sb.append("</ul>\n");
        sb.append("<p><b>Красные флаги мгновенного отсева (Stop-факторы):</b></p>\n");
        sb.append("<ul>\n");
        sb.append("  <li>Частая смена мест работы (менее 6–8 месяцев на проекте без объективных причин);</li>\n");
        sb.append("  <li>Опыт исключительно на учебных проектах / курсах без коммерческого продакшн-опыта;</li>\n");
        sb.append("  <li>Отсутствие практики по ключевой технологии в недавнем опыте;</li>\n");
        sb.append("  <li>Формат «только подработка / менторство», когда требуется full-time занятость.</li>\n");
        sb.append("</ul>\n\n");

        sb.append("<h4>3. Готовые поисковые запросы (Boolean Search Strings)</h4>\n");
        sb.append("<p><b>Для hh.ru:</b></p>\n");
        sb.append("<pre style=\"background: #f8fafc; padding: 10px; border: 1px solid #e2e8f0; border-radius: 4px;\"><code>");
        sb.append("(\"").append(role).append("\" OR \"").append(firstSkill).append(" Developer\") AND (\"").append(booleanSkillsStr).append("\") AND NOT (\"курсы\" OR \"стажер\" OR \"стажерка\" OR \"интерн\" OR \"преподаватель\")");
        sb.append("</code></pre>\n");

        sb.append("<p><b>Для Хабр Карьеры:</b></p>\n");
        sb.append("<pre style=\"background: #f8fafc; padding: 10px; border: 1px solid #e2e8f0; border-radius: 4px;\"><code>");
        sb.append("Специализация: Разработка • Ключевые навыки: ").append(firstSkill).append(" • Квалификация: ").append(grade);
        sb.append("</code></pre>\n");

        sb.append("<p><b>X-Ray поисковые запросы (Google / Yandex):</b></p>\n");
        sb.append("<pre style=\"background: #f8fafc; padding: 10px; border: 1px solid #e2e8f0; border-radius: 4px;\"><code>");
        sb.append("site:hh.ru/resume (\"").append(role).append("\") AND (\"").append(firstSkill).append("\") -intitle:курсы -intitle:стажер\n");
        sb.append("site:linkedin.com/in (\"").append(role).append("\" OR \"").append(firstSkill).append(" Developer\") (\"Россия\" OR \"Москва\" OR \"Remote\")");
        sb.append("</code></pre>\n\n");

        sb.append("<h4>4. Компании-доноры кандидатов (Target Companies)</h4>\n");
        sb.append("<ul>\n");
        sb.append("  <li><b>Системные интеграторы и IT-консалтинг:</b> ЛАНИТ, КРОК, IBS, Bell Integrator, Neoflex, Иннотех, Лига Цифровой Экономики, Т1</li>\n");
        sb.append("  <li><b>Продуктовый BigTech и Финтех:</b> Сбер, Т-Банк, ВТБ, Яндекс, VK, Ozon, Авито, МТС, Wildberries</li>\n");
        sb.append("  <li><b>Отраслевые лидеры:</b> профильные компании целевого сектора проекта (ритейл, финтех, телеком, логистика)</li>\n");
        sb.append("</ul>\n\n");

        sb.append("<h4>5. Хук для холодного контакта (Elevator Pitch в Telegram / LinkedIn)</h4>\n");
        sb.append("<blockquote style=\"border-left: 3px solid #0284c7; padding-left: 10px; margin-left: 0; color: #0f172a;\">\n");
        sb.append("«[Имя], добрый день! Обратил внимание на ваш сильный опыт в ").append(firstSkill)
                .append(". В HuntTech открыта отличная позиция ").append(role).append(" в проектную команду ")
                .append(data.getProjectName() != null ? escapeHtml(data.getProjectName()) : "надежного технологического партнера")
                .append(": 100% удаленка, современный стек без устаревшего легаси, прямой контакт с техлидом проекта без бюрократии. Буду рад кратко поделиться деталями задач и условий на 10-минутном созвоне. Удобно пообщаться сегодня или завтра?»\n");
        sb.append("</blockquote>");

        return sb.toString();
    }

    private String buildInterviewPlan(SmartOpenPositionParsedData data) {
        String projName = data.getProjectName() != null ? escapeHtml(data.getProjectName()) : "Корпоративный проект";
        String vacName = data.getVacansyName() != null ? escapeHtml(data.getVacansyName()) : "Позиция в проектную команду";
        String firstSkill = (data.getRequiredSkills() != null && !data.getRequiredSkills().isEmpty())
                ? escapeHtml(data.getRequiredSkills().get(0)) : "профильному стеку";

        StringBuilder sb = new StringBuilder();
        sb.append("<h3>План продающего собеседования (Interview Plan) / Сценарий рекрутера HuntTech</h3>\n\n");

        sb.append("<h4>Блок 1. Введение и презентация компании HuntTech (5–7 минут)</h4>\n");
        sb.append("<ul>\n");
        sb.append("  <li><b>Цель:</b> Установить контакт, сформировать доверие, снять барьер «просто кадровое агентство».</li>\n");
        sb.append("  <li><b>Ключевые акценты:</b> HuntTech — аккредитованная IT-компания полного цикла; прямой контракт с техническими лидерами проекта; забота о специалисте и персональный HRBP.</li>\n");
        sb.append("  <li><b>Скрипт рекрутера:</b> «[Имя], добрый день! Спасибо, что нашли время пообщаться. Меня зовут [Имя], я представляю IT-компанию HuntTech. Наша цель на сегодня — познакомиться, подробно рассказать вам о проекте и задачах, обсудить ваш опыт и понять, насколько мы взаимно подходим друг другу. По таймингу займет около 35–40 минут. Подскажите в двух словах: что для вас сейчас в приоритете при выборе новой роли — технологический стек, масштаб задач или условия сотрудничества?»</li>\n");
        sb.append("</ul>\n\n");

        sb.append("<h4>Блок 2. Презентация проекта и его технологического вызова (8–10 минут)</h4>\n");
        sb.append("<ul>\n");
        sb.append("  <li><b>Проект:</b> ").append(projName).append(". <b>Вакансия:</b> ").append(vacName).append(".</li>\n");
        sb.append("  <li><b>Цель:</b> Влюбить кандидата в проект, показать реальный инженерный масштаб.</li>\n");
        sb.append("  <li><b>Скрипт рекрутера:</b> «Мы усиливаем ключевую команду на проекте ").append(projName)
                .append(". Проект сейчас на этапе активного развития и масштабирования. Основной стек — ")
                .append(firstSkill).append(". Ключевой инженерный вызов — разработка надежных сервисов и реализация сложных интеграций без унылой бюрократии. Решения принимаются внутри команды, вы напрямую влияете на архитектуру.»</li>\n");
        sb.append("  <li><b>Selling Points вакансии:</b> Современный стек без легаси; свобода инженерных решений и высокая автономность; сильная команда сеньор-разработчиков.</li>\n");
        sb.append("</ul>\n\n");

        sb.append("<h4>Блок 3. Проверка по чек-листу ключевых требований (12–15 минут)</h4>\n");
        sb.append("<ul>\n");
        sb.append("  <li><b>Цель:</b> Провести валидацию коммерческого опыта по технике STAR (Ситуация — Задача — Действие — Результат).</li>\n");
        sb.append("  <li><b>Кейс 1 (Ключевой стек):</b> «Расскажите о самом технически сложном модуле на ").append(firstSkill)
                .append(", который вы разрабатывали за последние 1–2 года. С какими нетривиальными проблемами столкнулись и как их решили?»<br>")
                .append("  • <i>Зеленый флаг:</i> Четкое описание архитектуры, знание подводных камней и метрик производительности.<br>")
                .append("  • <i>Красный флаг:</i> Общие шаблонные фразы, непонимание внутренних механизмов фреймворка.</li>\n");
        sb.append("  <li><b>Кейс 2 (Базы данных и оптимизация):</b> «С какими СУБД работали на проде? Приходилось ли профилировать медленные запросы и проектировать схемы данных?»<br>")
                .append("  • <i>Зеленый флаг:</i> Понимание индексов, планов выполнения (EXPLAIN), транзакционной модели.<br>")
                .append("  • <i>Красный флаг:</i> Работа исключительно через ORM без понимания SQL-запросов.</li>\n");
        sb.append("  <li><b>Кейс 3 (Интеграции и архитектура):</b> «Как реализовывали межсервисное взаимодействие? Использовали ли очереди сообщений или REST API?»<br>")
                .append("  • <i>Зеленый флаг:</i> Понимание идемпотентности, обработки сбоев сети, контрактов API.</li>\n");
        sb.append("</ul>\n\n");

        sb.append("<h4>Блок 4. Продажа условий работы и формата сотрудничества (5 минут)</h4>\n");
        sb.append("<ul>\n");
        sb.append("  <li><b>Цель:</b> Зафиксировать прозрачные и комфортные условия сотрудничества (без раскрытия ставки заказчика!).</li>\n");
        sb.append("  <li><b>Скрипт рекрутера:</b> «По формату работы: у нас 100% удаленка с гибким началом дня. Мы ценим результат, поэтому никакого микроменеджмента и трекеров времени. По оформлению: работаем по [ТК РФ / ИП / Самозанятости] с гарантией стабильных своевременных выплат. Что касается дохода — вилка формируется индивидуально на основе ваших ожиданий и результатов технического интервью. На какую сумму вы ориентируетесь?»</li>\n");
        sb.append("</ul>\n\n");

        sb.append("<h4>Блок 5. Ответы на вопросы кандидата и отработка возражений (5–7 минут)</h4>\n");
        sb.append("<ul>\n");
        sb.append("  <li><b>Возражение «Почему через HuntTech, а не напрямую?»:</b> «HuntTech — генеральный IT-партнер заказчика. Для вас это ускоренный найм за 1–2 этапа без бесконечной корпоративной бюрократии, персональный менеджер и возможность плавного перехода на другие проекты нашей экосистемы.»</li>\n");
        sb.append("  <li><b>Возражение «Есть ли переработки?»:</b> «Процессы выстроены по Agile: спринты планируются реалистично. Переработки не приветствуются, при овертаймах — оплата всегда согласуется отдельно.»</li>\n");
        sb.append("</ul>\n\n");

        sb.append("<h4>Блок 6. Закрытие и согласование следующих шагов (3–5 минут)</h4>\n");
        sb.append("<ul>\n");
        sb.append("  <li><b>Скрипт рекрутера:</b> «[Имя], спасибо за содержательный разговор! Ваш опыт отлично ложится в стек и задачи проекта. Со своей стороны я с удовольствием готова передать ваше резюме напрямую тимлиду проекта. Следующий шаг — техническое интервью на 45–60 минут. Я подготовлю резюме и вернусь к вам с датой встречи в течение 24–48 часов. Договорились? Передаем резюме в работу?»</li>\n");
        sb.append("</ul>");

        return sb.toString();
    }

    @Override
    public OpenPosition findDuplicate(SmartOpenPositionParsedData data) {
        if (data == null) return null;
        String cleanName = data.getVacansyName() != null ? cleanTitle(data.getVacansyName()) : "";

        log.info("[SMART_VACANCY_OPENING] Поиск дубликата для вакансии: '{}', проект: '{}'", cleanName, data.getProjectName());

        // 1. Поиск по точному наименованию вакансии
        if (!cleanName.isEmpty()) {
            List<OpenPosition> list = dataManager.load(OpenPosition.class)
                    .query("select e from hunttech_OpenPosition e where lower(e.vacansyName) = lower(:name) and e.openClose = false")
                    .parameter("name", cleanName)
                    .view("openPosition-browse-view")
                    .maxResults(1)
                    .list();
            if (!list.isEmpty()) {
                OpenPosition duplicate = list.get(0);
                log.info("[SMART_VACANCY_OPENING] Найдена существующая открытая вакансия (дубликат по имени): ID={}, vacansyID={}, name='{}'",
                        duplicate.getId(), duplicate.getVacansyID(), duplicate.getVacansyName());
                return duplicate;
            }
        }

        // 2. Поиск по проекту и названию должности / специализации
        if (data.getProjectName() != null && !data.getProjectName().trim().isEmpty() &&
                data.getPositionTypeName() != null && !data.getPositionTypeName().trim().isEmpty()) {
            String cleanProj = cleanTitle(data.getProjectName());
            String cleanPos = cleanTitle(data.getPositionTypeName());
            List<OpenPosition> projList = dataManager.load(OpenPosition.class)
                    .query("select e from hunttech_OpenPosition e where lower(e.projectName.projectName) like lower(:proj) and (lower(e.positionType.positionRuName) like lower(:pos) or lower(e.positionType.positionEnName) like lower(:pos)) and e.openClose = false")
                    .parameter("proj", "%" + cleanProj + "%")
                    .parameter("pos", "%" + cleanPos + "%")
                    .view("openPosition-browse-view")
                    .maxResults(1)
                    .list();
            if (!projList.isEmpty()) {
                OpenPosition duplicate = projList.get(0);
                log.info("[SMART_VACANCY_OPENING] Найдена существующая открытая вакансия (дубликат по проекту и должности): ID={}, vacansyID={}, name='{}'",
                        duplicate.getId(), duplicate.getVacansyID(), duplicate.getVacansyName());
                return duplicate;
            }
        }

        // 3. Поиск по ID запроса в исходном тексте или названии (например, 62630 или 67451)
        String raw = data.getRawText() != null ? data.getRawText() : "";
        Pattern idPat = Pattern.compile("(?i)(?:ID|запрос|id=)\\s*(\\d{4,7})");
        Matcher idMat = idPat.matcher(raw);
        if (idMat.find()) {
            String reqId = idMat.group(1);
            List<OpenPosition> idList = dataManager.load(OpenPosition.class)
                    .query("select e from hunttech_OpenPosition e where (lower(e.vacansyName) like :reqId or lower(e.rawDescription) like :reqId or lower(e.comment) like :reqId) and e.openClose = false")
                    .parameter("reqId", "%" + reqId + "%")
                    .view("openPosition-browse-view")
                    .maxResults(1)
                    .list();
            if (!idList.isEmpty()) {
                OpenPosition duplicate = idList.get(0);
                log.info("[SMART_VACANCY_OPENING] Найдена существующая открытая вакансия (дубликат по ID запроса {}): ID={}, vacansyID={}, name='{}'",
                        reqId, duplicate.getId(), duplicate.getVacansyID(), duplicate.getVacansyName());
                return duplicate;
            }
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
            if (data.getVacansyID() != null && !data.getVacansyID().trim().isEmpty()) {
                openPosition.setVacansyID(cleanVacansyId(data.getVacansyID()));
                log.info("[SMART_VACANCY_OPENING] Установлен vacansyID вакансии: '{}'", openPosition.getVacansyID());
            }
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
            openPosition.setComment(fullComment != null ? fullComment : "");

            String shortDesc = data.getShortDescription() != null && !data.getShortDescription().isEmpty()
                    ? cleanHtmlToPlainText(data.getShortDescription()) : safeVacName;
            openPosition.setShortDescription(truncate(shortDesc, 250));

            openPosition.setLastOpenDate(new Date());
            openPosition.setOwner(recruiter);
            openPosition.setCommandCandidate(1);

            log.info("[SMART_VACANCY_OPENING] Заполнены атрибуты OpenPosition: name='{}', vacansyID='{}', signDraft=true (ЧЕРНОВИК), priority={} (UNDER_REVIEW), remoteWork={}, salaryMin={}, salaryMax={}, exp={}",
                    openPosition.getVacansyName(), openPosition.getVacansyID(), openPosition.getPriority(), openPosition.getRemoteWork(), openPosition.getSalaryMin(), openPosition.getSalaryMax(), openPosition.getWorkExperience());

            // Дополнительные реквизиты сущности OpenPosition
            if (data.getSalaryIE() != null) {
                openPosition.setSalaryIE(data.getSalaryIE());
            }
            if (data.getSalaryComment() != null) {
                openPosition.setSalaryComment(data.getSalaryComment());
            }
            if (data.getRawText() != null) {
                openPosition.setRawDescription(data.getRawText());
            }
            if (data.getInterviewChecklist() != null) {
                openPosition.setInterviewChecklist(data.getInterviewChecklist());
            }
            if (data.getSearchMap() != null) {
                openPosition.setSearchMap(data.getSearchMap());
            }
            if (data.getInterviewPlan() != null) {
                openPosition.setInterviewPlan(data.getInterviewPlan());
            }
            if (data.getExercise() != null && !data.getExercise().trim().isEmpty()) {
                openPosition.setExercise(data.getExercise());
                openPosition.setNeedExercise(true);
            }
            if (data.getMemoForInterview() != null && !data.getMemoForInterview().trim().isEmpty()) {
                openPosition.setMemoForInterview(data.getMemoForInterview());
                openPosition.setNeedMemoForInterview(true);
            }

            // 1. Поиск / создание проекта (разрешено генерировать Project только если не найден существующий)
            Project project = findOrCreateProject(data.getProjectName(), data.getCompanyName(),
                    data.getProjectShortDescription(), data.getProjectFullDescription(), commitContext);
            openPosition.setProjectName(project);
            log.info("[SMART_VACANCY_OPENING] Проект позиции: {}", project != null ? (project.getProjectName() + " (ID=" + project.getId() + ")") : "НЕ НАЙДЕН");

            // 2. Поиск / привязка типа позиции (должность)
            String posName = data.getPositionTypeName() != null && !data.getPositionTypeName().trim().isEmpty()
                    ? data.getPositionTypeName()
                    : (data.getRawVacansyName() != null && !data.getRawVacansyName().trim().isEmpty()
                        ? data.getRawVacansyName() : data.getVacansyName());
            Position positionType = findBestMatchingPositionType(posName);
            openPosition.setPositionType(positionType);
            log.info("[SMART_VACANCY_OPENING] Тип позиции (должность): {}", positionType != null ? (positionType.getPositionRuName() + " (ID=" + positionType.getId() + ")") : "НЕ НАЙДЕН");

            // 3. Поиск / привязка грейда
            Grade grade = null;
            if (data.getGradeName() != null) {
                grade = findGrade(data.getGradeName());
                if (grade != null) {
                    openPosition.setGrade(grade);
                    log.info("[SMART_VACANCY_OPENING] Грейд позиции: {} (ID={})", grade.getGradeName(), grade.getId());
                }
            }

            // 4. Поиск / привязка города (запрещено создавать новые гео-данные!)
            City city = null;
            if (data.getCityName() != null) {
                city = findCity(data.getCityName());
                if (city != null) {
                    openPosition.setCityPosition(city);
                    log.info("[SMART_VACANCY_OPENING] Город позиции: {} (ID={})", city.getCityRuName(), city.getId());
                }
            }

            // 5. ВЫЗОВ АЛГОРИТМА ГЕНЕРАЦИИ НАЗВАНИЯ ВАКАНСИИ ИЗ OpenPositionEdit (кнопка «Генерировать»)
            String canonicalName = generateCanonicalVacancyName(grade, positionType, project, city, openPosition.getCities());
            if (canonicalName != null && !canonicalName.trim().isEmpty()) {
                openPosition.setVacansyName(truncate(canonicalName, 250));
                log.info("[SMART_VACANCY_OPENING] Сгенерировано каноническое наименование вакансии алгоритмом OpenPositionEdit: '{}'", openPosition.getVacansyName());
            } else {
                openPosition.setVacansyName(safeVacName);
            }

            // Добавление создаваемой вакансии в транзакцию сохранения
            commitContext.addInstanceToCommit(openPosition);

            // Привязка навыков к вакансии через справочник SkillTree и каноническую сущность OpenPositionSkill
            if (data.getRequiredSkills() != null && !data.getRequiredSkills().isEmpty()) {
                log.info("[SMART_VACANCY_OPENING] Привязка навыков к вакансии (всего: {})...", data.getRequiredSkills().size());
                int count = 0;
                Set<String> processedSkills = new HashSet<>();

                for (String skillName : data.getRequiredSkills()) {
                    if (skillName == null || skillName.trim().isEmpty()) continue;
                    String cleanSkill = truncate(cleanTitle(skillName), 80);
                    if (cleanSkill.isEmpty() || processedSkills.contains(cleanSkill.toLowerCase())) continue;
                    processedSkills.add(cleanSkill.toLowerCase());

                    // Поиск существующего навыка в справочнике hunttech_SkillTree
                    List<SkillTree> existingSkills = dataManager.load(SkillTree.class)
                            .query("select s from hunttech_SkillTree s where lower(s.skillName) = lower(:name)")
                            .parameter("name", cleanSkill)
                            .view("_local")
                            .maxResults(1)
                            .list();

                    SkillTree skill;
                    if (!existingSkills.isEmpty()) {
                        skill = existingSkills.get(0);
                        log.info("[SMART_VACANCY_OPENING] Использован существующий навык из справочника: '{}' (ID={})", skill.getSkillName(), skill.getId());
                    } else {
                        skill = metadata.create(SkillTree.class);
                        skill.setSkillName(cleanSkill);
                        commitContext.addInstanceToCommit(skill);
                        log.info("[SMART_VACANCY_OPENING] Создан новый навык в справочнике: '{}'", cleanSkill);
                    }

                    // Связывание навыка с вакансией через каноническую сущность OpenPositionSkill
                    OpenPositionSkill ops = metadata.create(OpenPositionSkill.class);
                    ops.setOpenPosition(openPosition);
                    ops.setSkill(skill);
                    ops.setPriority(CandidateSkillPriority.MAIN);
                    commitContext.addInstanceToCommit(ops);
                    count++;
                }
                log.info("[SMART_VACANCY_OPENING] Успешно привязано {} навыков к вакансии через OpenPositionSkill", count);
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

    @Override
    public String generateCanonicalVacancyName(Grade grade, Position positionType, Project project, City city, Collection<City> additionalCities) {
        StringBuilder sb = new StringBuilder();

        if (grade != null && grade.getGradeName() != null && !grade.getGradeName().trim().isEmpty()) {
            sb.append(grade.getGradeName().trim()).append(" ");
        }

        if (positionType != null) {
            String ru = positionType.getPositionRuName() != null ? positionType.getPositionRuName().trim() : "";
            String en = positionType.getPositionEnName() != null ? positionType.getPositionEnName().trim() : "";
            sb.append(ru);
            if (!en.isEmpty() && !en.equalsIgnoreCase(ru)) {
                sb.append(" / ").append(en);
            }
        } else {
            return "";
        }

        if (project != null && project.getProjectName() != null && !project.getProjectName().trim().isEmpty()) {
            sb.append(" (").append(project.getProjectName().trim());
        } else {
            return "";
        }

        if (city != null && city.getCityRuName() != null && !city.getCityRuName().trim().isEmpty()) {
            sb.append(", ").append(city.getCityRuName().trim());
        }

        if (additionalCities != null && !additionalCities.isEmpty()) {
            for (City c : additionalCities) {
                if (c != null && c.getCityRuName() != null && !c.getCityRuName().trim().isEmpty()) {
                    sb.append(", ").append(c.getCityRuName().trim());
                }
            }
        }

        sb.append(")");

        return sb.toString();
    }

    private Project findExistingOpenProject(String projectName, String companyName) {
        if (dataManager == null) return null;
        String cleanName = projectName != null ? cleanTitle(projectName) : "";
        if (!cleanName.isEmpty()) {
            List<Project> list = dataManager.load(Project.class)
                    .query("select e from hunttech_Project e where lower(e.projectName) like lower(:name) and (e.projectIsClosed is null or e.projectIsClosed = false)")
                    .parameter("name", "%" + cleanName + "%")
                    .view("project-picker-view")
                    .maxResults(1)
                    .list();
            if (!list.isEmpty()) return list.get(0);
        }
        if (companyName != null && !companyName.trim().isEmpty()) {
            String cleanCompany = cleanTitle(companyName);
            List<Project> list = dataManager.load(Project.class)
                    .query("select e from hunttech_Project e where lower(e.projectName) like lower(:comp) and (e.projectIsClosed is null or e.projectIsClosed = false)")
                    .parameter("comp", "%" + cleanCompany + "%")
                    .view("project-picker-view")
                    .maxResults(1)
                    .list();
            if (!list.isEmpty()) return list.get(0);
        }
        return null;
    }

    private Project findOrCreateProject(String projectName, String companyName, String shortDesc, String fullDesc, CommitContext commitContext) {
        if (dataManager == null || metadata == null) return null;
        log.info("[SMART_VACANCY_OPENING] Поиск проекта: name='{}', company='{}'", projectName, companyName);

        // 1. Поиск открытого проекта по наименованию или компании (дедуплицированный хелпер)
        Project existing = findExistingOpenProject(projectName, companyName);
        if (existing != null) {
            log.info("[SMART_VACANCY_OPENING] Использован найденный открытый проект: '{}' (ID={})", existing.getProjectName(), existing.getId());
            return existing;
        }

        // 2. Определение наименования нового проекта
        String cleanName = projectName != null ? cleanTitle(projectName) : "";
        String newProjName = !cleanName.isEmpty() ? cleanName : (!cleanTitle(companyName).isEmpty() ? cleanTitle(companyName) : "Новый проект");
        newProjName = truncate(newProjName, 150);

        // 3. Проверка существования проекта-заглушки (например «Новый проект») во избежание дублирования
        List<Project> stubs = dataManager.load(Project.class)
                .query("select e from hunttech_Project e where lower(e.projectName) = lower(:n) and (e.projectIsClosed is null or e.projectIsClosed = false)")
                .parameter("n", newProjName)
                .view("project-picker-view")
                .maxResults(1)
                .list();
        if (!stubs.isEmpty()) {
            log.info("[SMART_VACANCY_OPENING] Использован существующий открытый проект: '{}' (ID={})", stubs.get(0).getProjectName(), stubs.get(0).getId());
            return stubs.get(0);
        }

        // 4. Проект не найден среди существующих открытых. Генерируем новую подчиненную сущность Project
        log.info("[SMART_VACANCY_OPENING] Открытый проект не найден в БД. Создание новой подчиненной сущности Project: '{}'", newProjName);

        Project newProj = metadata.create(Project.class);
        newProj.setProjectName(newProjName);
        newProj.setProjectIsClosed(false);
        newProj.setStartProjectDate(new Date());
        if (shortDesc != null && !shortDesc.trim().isEmpty()) {
            newProj.setShortDescription(truncate(cleanHtmlToPlainText(shortDesc), 250));
        }
        if (fullDesc != null && !fullDesc.trim().isEmpty()) {
            newProj.setProjectDescription(cleanHtmlToPlainText(fullDesc));
        }
        commitContext.addInstanceToCommit(newProj);
        return newProj;
    }

    private Position findBestMatchingPositionType(String positionName) {
        if (dataManager == null) return null;
        if (positionName == null || positionName.trim().isEmpty()) return null;
        String cleanName = cleanTitle(positionName);
        if (cleanName.isEmpty()) return null;

        String safeName = truncate(cleanName, 80);
        log.info("[SMART_VACANCY_OPENING] Подбор наиболее подходящей должности в справочнике hunttech_Position для '{}'", safeName);

        // 1. Поиск по точному совпадению наименования (RU или EN)
        List<Position> exactList = dataManager.load(Position.class)
                .query("select e from hunttech_Position e where (lower(e.positionRuName) = lower(:name) or lower(e.positionEnName) = lower(:name)) and (e.positionRuName not like '%(не использовать)%' and e.positionRuName not like '%дубль%')")
                .parameter("name", safeName)
                .view("position-picker-view")
                .maxResults(1)
                .list();
        if (!exactList.isEmpty()) {
            log.info("[SMART_VACANCY_OPENING] ✓ Найдено точное совпадение должности: '{}' (ID={})", exactList.get(0).getPositionRuName(), exactList.get(0).getId());
            return exactList.get(0);
        }

        // 2. Интеллектуальный поиск среди существующих активных должностей по схожести и токенам
        List<Position> allPositions = dataManager.load(Position.class)
                .query("select e from hunttech_Position e where (e.positionRuName not like '%(не использовать)%' and e.positionRuName not like '%дубль%')")
                .view("position-picker-view")
                .list();

        Position bestMatch = null;
        int bestScore = 0;
        String lowerTarget = safeName.toLowerCase();
        Set<String> targetTokens = extractSignificantTokens(lowerTarget);

        for (Position pos : allPositions) {
            String ru = pos.getPositionRuName() != null ? pos.getPositionRuName().toLowerCase() : "";
            String en = pos.getPositionEnName() != null ? pos.getPositionEnName().toLowerCase() : "";

            int score = 0;
            // Совпадение как подстроки (например, "Разработчик 1С" внутри "Разработчик 1С:WMS")
            if (!ru.isEmpty() && lowerTarget.contains(ru)) {
                score += 100 + ru.length();
            }
            if (!en.isEmpty() && lowerTarget.contains(en)) {
                score += 100 + en.length();
            }
            // Обратное совпадение: строка из справочника содержит искомое слово
            if (!ru.isEmpty() && ru.contains(lowerTarget)) {
                score += 50;
            }

            // Токенное пересечение
            Set<String> candidateTokens = extractSignificantTokens(ru + " " + en);
            for (String token : targetTokens) {
                if (candidateTokens.contains(token)) {
                    score += 15;
                }
            }

            if (score > bestScore) {
                bestScore = score;
                bestMatch = pos;
            }
        }

        if (bestMatch != null && bestScore > 0) {
            log.info("[SMART_VACANCY_OPENING] ✓ Подобрана наиболее подходящая существующая должность: '{}' (ID={}, score={}) для входной '{}'",
                    bestMatch.getPositionRuName(), bestMatch.getId(), bestScore, safeName);
            return bestMatch;
        }

        // 3. Fallback: поиск общей должности по ключевым технологическим направлениям
        log.warn("[SMART_VACANCY_OPENING] Должность '{}' не найдена напрямую, попытка подобрать базовую должность в справочнике", safeName);
        List<Position> fallbackList = dataManager.load(Position.class)
                .query("select e from hunttech_Position e where (lower(e.positionRuName) like '%разработчик%' or lower(e.positionRuName) like '%инженер%' or lower(e.positionRuName) like '%аналитик%') and e.positionRuName not like '%(не использовать)%'")
                .view("position-picker-view")
                .maxResults(1)
                .list();
        if (!fallbackList.isEmpty()) {
            log.info("[SMART_VACANCY_OPENING] Выбрана базовая должность: '{}'", fallbackList.get(0).getPositionRuName());
            return fallbackList.get(0);
        }

        log.warn("[SMART_VACANCY_OPENING] Не удалось подобрать должность в справочнике hunttech_Position. Новая должность НЕ создается (согласно бизнес-правилу)");
        return null;
    }

    private Set<String> extractSignificantTokens(String text) {
        if (text == null) return Collections.emptySet();
        Set<String> tokens = new HashSet<>();
        for (String part : text.toLowerCase().split("[^a-zA-Zа-яА-Я0-9]+")) {
            String trimmed = part.trim();
            if (trimmed.length() >= 2 && !isStopWord(trimmed)) {
                tokens.add(trimmed);
            }
        }
        return tokens;
    }

    private boolean isStopWord(String word) {
        return "от".equals(word) || "до".equals(word) || "для".equals(word) || "по".equals(word)
                || "на".equals(word) || "в".equals(word) || "и".equals(word) || "или".equals(word)
                || "с".equals(word) || "со".equals(word) || "за".equals(word) || "из".equals(word);
    }

    private Grade findGrade(String gradeName) {
        if (dataManager == null) return null;
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
        if (dataManager == null) return null;
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

    private static String cleanVacansyId(String rawId) {
        if (rawId == null) return null;
        String clean = rawId.replaceAll("^[#№\\s]+", "").trim();
        if (clean.contains("/") || clean.contains("?")) {
            String extracted = extractVacancyIdFromTextOrUrl(clean);
            if (extracted != null && !extracted.isEmpty()) {
                clean = extracted;
            }
        }
        if (clean.length() > 16) {
            log.warn("[SMART_VACANCY_OPENING] vacansyID '{}' превышает 16 символов, обрезается до 16", clean);
            return clean.substring(0, 16);
        }
        return clean;
    }

    private static String extractVacancyIdFromTextOrUrl(String text) {
        if (text == null || text.trim().isEmpty()) return null;

        // 1. Извлечение из URL
        Matcher urlMat = URL_VACANCY_ID_PATTERN.matcher(text);
        if (urlMat.find()) {
            return urlMat.group(1);
        }

        // 2. Извлечение по точным ключевым словам
        Matcher idMat = KEYWORD_VACANCY_ID_PATTERN.matcher(text);
        if (idMat.find()) {
            return idMat.group(1).trim();
        }

        // 3. Извлечение хештегов идентификаторов
        Matcher tagMat = HASHTAG_VACANCY_ID_PATTERN.matcher(text);
        if (tagMat.find()) {
            return tagMat.group(1).trim();
        }

        return null;
    }

    static String formatAsCleanHtml(String input) {
        if (input == null || input.trim().isEmpty()) return "";
        String trimmed = sanitizeDangerousHtml(input.trim());

        // Проверяем, содержит ли текст явные признаки Markdown (таблицы, заголовки ###, списки -/*)
        boolean hasMarkdownTables = trimmed.contains("|") && (trimmed.contains("|---") || trimmed.contains("| ---") || trimmed.contains("|-"));
        boolean hasMarkdownHeaders = MD_DETECT_HEADER_PATTERN.matcher(trimmed).find();
        boolean hasMarkdownLists = MD_DETECT_LIST_PATTERN.matcher(trimmed).find();

        if (hasMarkdownTables || hasMarkdownHeaders || hasMarkdownLists) {
            return convertMarkdownToHtml(trimmed);
        }

        // Если это уже HTML без markdown-элементов, проверяем наличие тегов
        boolean hasHtmlTags = HTML_TAG_DETECT_PATTERN.matcher(trimmed).find();
        if (hasHtmlTags) {
            return trimmed;
        }

        // Если это обычный текст без тегов, форматируем в чистый HTML
        return convertMarkdownToHtml(trimmed);
    }

    static String sanitizeDangerousHtml(String html) {
        if (html == null) return "";
        String sanitized = DANGEROUS_TAGS_PATTERN.matcher(html).replaceAll("");
        sanitized = DANGEROUS_TAG_SINGLE_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = JAVASCRIPT_URI_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = ON_EVENT_HANDLER_PATTERN.matcher(sanitized).replaceAll("");
        return sanitized;
    }

    static String convertMarkdownToHtml(String md) {
        if (md == null || md.trim().isEmpty()) return "";
        String[] lines = md.replace("\r", "").split("\n");
        StringBuilder html = new StringBuilder();

        boolean inUl = false;
        boolean inOl = false;
        boolean inTable = false;
        boolean inPre = false;
        List<String> tableRows = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();

            // 1. Блоки кода (```)
            if (trimmed.startsWith("```")) {
                if (inPre) {
                    html.append("</code></pre>\n");
                    inPre = false;
                } else {
                    if (inUl) { html.append("</ul>\n"); inUl = false; }
                    if (inOl) { html.append("</ol>\n"); inOl = false; }
                    html.append("<pre style=\"background-color: #f8f9fa; border: 1px solid #e9ecef; border-radius: 4px; padding: 10px; overflow-x: auto;\"><code>");
                    inPre = true;
                }
                continue;
            }
            if (inPre) {
                html.append(escapeHtml(line)).append("\n");
                continue;
            }

            // 2. Таблицы Markdown (| col1 | col2 |)
            if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
                if (inUl) { html.append("</ul>\n"); inUl = false; }
                if (inOl) { html.append("</ol>\n"); inOl = false; }
                tableRows.add(trimmed);
                inTable = true;
                continue;
            } else if (inTable) {
                // Завершение таблицы
                html.append(renderMarkdownTable(tableRows));
                tableRows.clear();
                inTable = false;
            }

            // 3. Заголовки (###, ####, ##, #)
            Matcher hMat = MD_HEADER_LINE_PATTERN.matcher(trimmed);
            if (hMat.matches()) {
                if (inUl) { html.append("</ul>\n"); inUl = false; }
                if (inOl) { html.append("</ol>\n"); inOl = false; }
                int level = Math.min(Math.max(hMat.group(1).length(), 2), 5); // h2 - h5
                String title = formatInlineMarkdown(hMat.group(2).trim());
                html.append("<h").append(level).append(" style=\"margin-top: 14px; margin-bottom: 6px; color: #212529;\">")
                        .append(title).append("</h").append(level).append(">\n");
                continue;
            }

            // 4. Маркированные списки (- item, * item, • item)
            Matcher ulMat = MD_UL_LINE_PATTERN.matcher(trimmed);
            if (ulMat.matches()) {
                if (inOl) { html.append("</ol>\n"); inOl = false; }
                if (!inUl) {
                    html.append("<ul style=\"margin-top: 4px; margin-bottom: 8px; padding-left: 20px;\">\n");
                    inUl = true;
                }
                String itemContent = formatInlineMarkdown(ulMat.group(1).trim());
                html.append("  <li>").append(itemContent).append("</li>\n");
                continue;
            }

            // 5. Нумерованные списки (1. item, 2. item)
            Matcher olMat = MD_OL_LINE_PATTERN.matcher(trimmed);
            if (olMat.matches()) {
                if (inUl) { html.append("</ul>\n"); inUl = false; }
                if (!inOl) {
                    html.append("<ol style=\"margin-top: 4px; margin-bottom: 8px; padding-left: 20px;\">\n");
                    inOl = true;
                }
                String itemContent = formatInlineMarkdown(olMat.group(1).trim());
                html.append("  <li>").append(itemContent).append("</li>\n");
                continue;
            }

            // Если вышли из списков
            if (inUl) { html.append("</ul>\n"); inUl = false; }
            if (inOl) { html.append("</ol>\n"); inOl = false; }

            // 6. Пустая строка
            if (trimmed.isEmpty()) {
                html.append("<br/>\n");
                continue;
            }

            // 7. Обычный абзац или уже HTML-тег
            if (trimmed.startsWith("<") && trimmed.endsWith(">")) {
                html.append(trimmed).append("\n");
            } else {
                html.append("<p style=\"margin: 4px 0;\">").append(formatInlineMarkdown(trimmed)).append("</p>\n");
            }
        }

        if (inPre) {
            html.append("</code></pre>\n");
        }
        if (inTable) {
            html.append(renderMarkdownTable(tableRows));
        }
        if (inUl) {
            html.append("</ul>\n");
        }
        if (inOl) {
            html.append("</ol>\n");
        }

        return html.toString().trim();
    }

    private static String renderMarkdownTable(List<String> rows) {
        if (rows == null || rows.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("<table border=\"1\" cellpadding=\"6\" cellspacing=\"0\" style=\"border-collapse: collapse; width: 100%; margin: 10px 0; border: 1px solid #dee2e6;\">\n");

        boolean hasHeader = false;
        int headerIndex = -1;

        // Поиск строки разделителя |---|---|
        for (int i = 0; i < rows.size(); i++) {
            String r = rows.get(i);
            if (MD_TABLE_SPLIT_PATTERN.matcher(r).matches() && r.contains("-")) {
                headerIndex = i;
                hasHeader = (i > 0);
                break;
            }
        }

        if (hasHeader) {
            sb.append("  <thead style=\"background-color: #f1f3f5; font-weight: bold;\">\n");
            for (int i = 0; i < headerIndex; i++) {
                sb.append("    <tr>\n");
                String[] cols = parseTableRow(rows.get(i));
                for (String c : cols) {
                    sb.append("      <th style=\"border: 1px solid #dee2e6; padding: 8px; text-align: left;\">")
                            .append(formatInlineMarkdown(c)).append("</th>\n");
                }
                sb.append("    </tr>\n");
            }
            sb.append("  </thead>\n");
            sb.append("  <tbody>\n");
            for (int i = headerIndex + 1; i < rows.size(); i++) {
                sb.append("    <tr>\n");
                String[] cols = parseTableRow(rows.get(i));
                for (String c : cols) {
                    sb.append("      <td style=\"border: 1px solid #dee2e6; padding: 8px;\">")
                            .append(formatInlineMarkdown(c)).append("</td>\n");
                }
                sb.append("    </tr>\n");
            }
            sb.append("  </tbody>\n");
        } else {
            sb.append("  <tbody>\n");
            for (String row : rows) {
                sb.append("    <tr>\n");
                String[] cols = parseTableRow(row);
                for (String c : cols) {
                    sb.append("      <td style=\"border: 1px solid #dee2e6; padding: 8px;\">")
                            .append(formatInlineMarkdown(c)).append("</td>\n");
                }
                sb.append("    </tr>\n");
            }
            sb.append("  </tbody>\n");
        }

        sb.append("</table>\n");
        return sb.toString();
    }

    private static String[] parseTableRow(String row) {
        String s = row.trim();
        if (s.startsWith("|")) s = s.substring(1);
        if (s.endsWith("|")) s = s.substring(0, s.length() - 1);
        String[] parts = s.split("\\|");
        String[] clean = new String[parts.length];
        for (int i = 0; i < parts.length; i++) {
            clean[i] = parts[i].trim();
        }
        return clean;
    }

    private static String formatInlineMarkdown(String text) {
        if (text == null) return "";
        // Сначала экранируем спецсимволы HTML
        String s = escapeHtml(text);
        // Код `code`
        s = s.replaceAll("`([^`]+)`", "<code style=\"background-color: #f1f3f5; padding: 2px 4px; border-radius: 3px; font-family: monospace;\">$1</code>");
        // Жирный **bold**
        s = s.replaceAll("\\*\\*([^*]+)\\*\\*", "<b>$1</b>");
        return s;
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String preview(String s, int maxLen) {
        if (s == null) return "null";
        String oneLine = s.replaceAll("\\s+", " ").trim();
        return oneLine.length() <= maxLen ? oneLine : oneLine.substring(0, maxLen) + "...";
    }
}

