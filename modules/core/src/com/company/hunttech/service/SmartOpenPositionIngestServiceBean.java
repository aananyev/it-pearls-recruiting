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
        if (fileBytes == null || fileBytes.length == 0) {
            if (fileDescriptor != null) {
                try (InputStream is = fileLoader.openStream(fileDescriptor)) {
                    fileBytes = is.readAllBytes();
                } catch (Exception e) {
                    log.error("Не удалось прочитать файл вакансии из хранилища", e);
                    return "";
                }
            } else {
                return "";
            }
        }

        String ext = fileDescriptor != null && fileDescriptor.getExtension() != null
                ? fileDescriptor.getExtension().toLowerCase() : "";

        try {
            if ("pdf".equals(ext)) {
                try (PDDocument document = Loader.loadPDF(fileBytes)) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    return stripper.getText(document);
                }
            } else if ("docx".equals(ext)) {
                try (InputStream is = new ByteArrayInputStream(fileBytes);
                     XWPFDocument doc = new XWPFDocument(is);
                     XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
                    return extractor.getText();
                }
            } else if ("doc".equals(ext)) {
                try (InputStream is = new ByteArrayInputStream(fileBytes);
                     WordExtractor extractor = new WordExtractor(is)) {
                    return extractor.getText();
                }
            } else {
                return new String(fileBytes, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.error("Ошибка при извлечении текста вакансии из файла", e);
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
            SmartOpenPositionParsedData emptyData = new SmartOpenPositionParsedData();
            emptyData.setRawText("");
            return emptyData;
        }

        String cleanedText = rawText.trim();
        SmartOpenPositionParsedData data = new SmartOpenPositionParsedData();
        data.setRawText(cleanedText);

        // 1. Попытка AI-анализа через системный сервис
        boolean parsedByAi = false;
        try {
            Map<String, Object> ctx = Collections.singletonMap("sourceText", cleanedText);
            AiExecutionResult aiResult = aiExecutionService.executeText(FUNCTION_VACANCY_SMART_PARSE_JSON, ctx);

            if (aiResult != null && aiResult.getText() != null && !aiResult.getText().trim().isEmpty()) {
                String aiText = extractCleanJson(aiResult.getText());
                JsonNode json = objectMapper.readTree(aiText);

                if (json.hasNonNull("vacansyName")) {
                    data.setVacansyName(json.get("vacansyName").asText().trim());
                }
                if (json.hasNonNull("projectName")) {
                    data.setProjectName(json.get("projectName").asText().trim());
                }
                if (json.hasNonNull("companyName")) {
                    data.setCompanyName(json.get("companyName").asText().trim());
                }
                if (json.hasNonNull("positionTypeName")) {
                    data.setPositionTypeName(json.get("positionTypeName").asText().trim());
                }
                if (json.hasNonNull("gradeName")) {
                    data.setGradeName(json.get("gradeName").asText().trim());
                }
                if (json.hasNonNull("cityName")) {
                    data.setCityName(json.get("cityName").asText().trim());
                }
                if (json.hasNonNull("remoteWork")) {
                    data.setRemoteWork(json.get("remoteWork").asInt(1));
                }
                if (json.hasNonNull("salaryMin")) {
                    data.setSalaryMin(BigDecimal.valueOf(json.get("salaryMin").asDouble()));
                }
                if (json.hasNonNull("salaryMax")) {
                    data.setSalaryMax(BigDecimal.valueOf(json.get("salaryMax").asDouble()));
                }
                if (json.hasNonNull("workExperience")) {
                    data.setWorkExperience(json.get("workExperience").asInt(3));
                }
                if (json.hasNonNull("shortDescription")) {
                    data.setShortDescription(json.get("shortDescription").asText().trim());
                }
                if (json.has("requiredSkills") && json.get("requiredSkills").isArray()) {
                    List<String> skills = new ArrayList<>();
                    for (JsonNode skillNode : json.get("requiredSkills")) {
                        String s = skillNode.asText().trim();
                        if (!s.isEmpty()) skills.add(s);
                    }
                    data.setRequiredSkills(skills);
                }
                if (json.has("checklist") && json.get("checklist").isArray()) {
                    List<String> chk = new ArrayList<>();
                    for (JsonNode chkNode : json.get("checklist")) {
                        String c = chkNode.asText().trim();
                        if (!c.isEmpty()) chk.add(c);
                    }
                    data.setChecklist(chk);
                }
                parsedByAi = true;
            }
        } catch (Exception e) {
            log.warn("AI-распознавание вакансии не выполнено, используется встроенный эвристический парсер: {}", e.getMessage());
        }

        // 2. Эвристический fallback-парсинг, если AI недоступен или вернул пустые поля
        if (!parsedByAi || data.getVacansyName() == null || data.getVacansyName().isEmpty()) {
            fallbackHeuristicParse(cleanedText, data);
        }

        data.setComment(cleanedText);

        // Проверка обязательных полей
        if (data.getVacansyName() == null || data.getVacansyName().isEmpty()) {
            data.setVacansyName("Новая открытая вакансия");
            data.getMissingFields().add("Название вакансии определено автоматически по умолчанию");
        }
        if (data.getProjectName() == null || data.getProjectName().isEmpty()) {
            data.getMissingFields().add("Проект не указан в тексте (будет выбран основной)");
        }

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

    private void fallbackHeuristicParse(String text, SmartOpenPositionParsedData data) {
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            if (data.getVacansyName() == null) {
                if (trimmed.toLowerCase().startsWith("вакансия:") || trimmed.toLowerCase().startsWith("позиция:") || trimmed.toLowerCase().startsWith("должность:")) {
                    data.setVacansyName(trimmed.substring(trimmed.indexOf(":") + 1).trim());
                } else if (trimmed.length() < 80 && (trimmed.toLowerCase().contains("developer") || trimmed.toLowerCase().contains("engineer") || trimmed.toLowerCase().contains("разработчик") || trimmed.toLowerCase().contains("аналитик") || trimmed.toLowerCase().contains("тестировщик") || trimmed.toLowerCase().contains("дизайнер") || trimmed.toLowerCase().contains("менеджер") || trimmed.toLowerCase().contains("lead") || trimmed.toLowerCase().contains("devops"))) {
                    data.setVacansyName(trimmed);
                }
            }

            // Зарплата
            Pattern salaryPattern = Pattern.compile("(\\d[\\d\\s]{3,})\\s*(?:-|до|—|–)\\s*(\\d[\\d\\s]{3,})\\s*(?:руб|р|rub|usd|\\$|€)?", Pattern.CASE_INSENSITIVE);
            Matcher salaryMatcher = salaryPattern.matcher(trimmed);
            if (salaryMatcher.find()) {
                try {
                    String minStr = salaryMatcher.group(1).replaceAll("\\s+", "");
                    String maxStr = salaryMatcher.group(2).replaceAll("\\s+", "");
                    if (data.getSalaryMin() == null) data.setSalaryMin(new BigDecimal(minStr));
                    if (data.getSalaryMax() == null) data.setSalaryMax(new BigDecimal(maxStr));
                } catch (Exception ignored) {}
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
            if (trimmed.toLowerCase().contains("senior")) {
                data.setGradeName("Senior");
            } else if (trimmed.toLowerCase().contains("middle")) {
                data.setGradeName("Middle");
            } else if (trimmed.toLowerCase().contains("junior")) {
                data.setGradeName("Junior");
            } else if (trimmed.toLowerCase().contains("lead")) {
                data.setGradeName("Lead");
            }
        }

        if (data.getVacansyName() == null && lines.length > 0) {
            data.setVacansyName(lines[0].trim());
        }

        // Поиск навыков по ключевым словам
        String[] popularSkills = {"Java", "Spring", "Kotlin", "Python", "PostgreSQL", "Docker", "Kubernetes", "React", "TypeScript", "JavaScript", "Go", "C#", "Kafka", "Redis", "Git", "CI/CD", "Linux", "SQL"};
        List<String> foundSkills = new ArrayList<>();
        for (String skill : popularSkills) {
            if (Pattern.compile("\\b" + Pattern.quote(skill) + "\\b", Pattern.CASE_INSENSITIVE).matcher(text).find()) {
                foundSkills.add(skill);
            }
        }
        data.setRequiredSkills(foundSkills);
    }

    @Override
    public OpenPosition findDuplicate(SmartOpenPositionParsedData data) {
        if (data == null || data.getVacansyName() == null) return null;
        List<OpenPosition> list = dataManager.load(OpenPosition.class)
                .query("select e from hunttech_OpenPosition e where lower(e.vacansyName) = lower(:name) and e.openClose = false")
                .parameter("name", data.getVacansyName().trim())
                .view("openPosition-browse-view")
                .maxResults(1)
                .list();
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public SmartOpenPositionIngestResult createOpenPosition(SmartOpenPositionParsedData data, ExtUser recruiter) {
        SmartOpenPositionIngestResult result = new SmartOpenPositionIngestResult();
        try {
            CommitContext commitContext = new CommitContext();

            OpenPosition openPosition = metadata.create(OpenPosition.class);
            openPosition.setVacansyName(data.getVacansyName() != null ? data.getVacansyName() : "Открытая вакансия");
            openPosition.setOpenClose(false); // Открыта
            openPosition.setSignDraft(false);
            openPosition.setRemoteWork(data.getRemoteWork() != null ? data.getRemoteWork() : 1);
            openPosition.setWorkExperience(data.getWorkExperience() != null ? data.getWorkExperience() : 3);
            openPosition.setNumberPosition(data.getNumberPosition() != null ? data.getNumberPosition() : 1);
            openPosition.setSalaryMin(data.getSalaryMin());
            openPosition.setSalaryMax(data.getSalaryMax());
            openPosition.setPriority(data.getPriority() != null ? data.getPriority() : 2);
            openPosition.setComment(data.getComment() != null ? data.getComment() : data.getRawText());
            openPosition.setShortDescription(data.getShortDescription() != null && data.getShortDescription().length() <= 250
                    ? data.getShortDescription() : (openPosition.getVacansyName().length() <= 250 ? openPosition.getVacansyName() : openPosition.getVacansyName().substring(0, 250)));
            openPosition.setLastOpenDate(new Date());
            openPosition.setOwner(recruiter);
            openPosition.setCommandCandidate(1);

            // Поиск / привязка проекта
            Project project = findOrCreateProject(data.getProjectName(), data.getCompanyName());
            openPosition.setProjectName(project);

            // Поиск / привязка типа позиции
            if (data.getPositionTypeName() != null || openPosition.getVacansyName() != null) {
                String posName = data.getPositionTypeName() != null ? data.getPositionTypeName() : openPosition.getVacansyName();
                Position positionType = findOrCreatePositionType(posName);
                openPosition.setPositionType(positionType);
            }

            // Поиск / привязка грейда
            if (data.getGradeName() != null) {
                Grade grade = findGrade(data.getGradeName());
                if (grade != null) {
                    openPosition.setGrade(grade);
                }
            }

            // Поиск / привязка города
            if (data.getCityName() != null) {
                City city = findCity(data.getCityName());
                if (city != null) {
                    openPosition.setCityPosition(city);
                }
            }

            commitContext.addInstanceToCommit(openPosition);

            // Добавление ключевых навыков в позицию
            if (data.getRequiredSkills() != null) {
                for (String skillName : data.getRequiredSkills()) {
                    SkillTree st = metadata.create(SkillTree.class);
                    st.setSkillName(skillName);
                    st.setOpenPosition(openPosition);
                    commitContext.addInstanceToCommit(st);
                }
            }

            dataManager.commit(commitContext);

            result.setSuccess(true);
            result.setOpenPosition(openPosition);
            result.setMessage("Вакансия «" + openPosition.getVacansyName() + "» успешно открыта!");
        } catch (Exception e) {
            log.error("Ошибка при создании вакансии", e);
            result.setSuccess(false);
            result.setMessage("Ошибка: " + e.getMessage());
        }
        return result;
    }

    private Project findOrCreateProject(String projectName, String companyName) {
        if (projectName != null && !projectName.trim().isEmpty()) {
            List<Project> list = dataManager.load(Project.class)
                    .query("select e from hunttech_Project e where lower(e.projectName) like lower(:name)")
                    .parameter("name", "%" + projectName.trim() + "%")
                    .view("project-picker-view")
                    .maxResults(1)
                    .list();
            if (!list.isEmpty()) {
                return list.get(0);
            }
        }
        // Первый доступный проект в системе по умолчанию
        List<Project> defaultList = dataManager.load(Project.class)
                .query("select e from hunttech_Project e order by e.createTs desc")
                .view("project-picker-view")
                .maxResults(1)
                .list();
        return defaultList.isEmpty() ? null : defaultList.get(0);
    }

    private Position findOrCreatePositionType(String positionName) {
        if (positionName == null || positionName.trim().isEmpty()) return null;
        List<Position> list = dataManager.load(Position.class)
                .query("select e from hunttech_Position e where lower(e.positionRuName) = lower(:name) or lower(e.positionEnName) = lower(:name)")
                .parameter("name", positionName.trim())
                .view("position-picker-view")
                .maxResults(1)
                .list();
        if (!list.isEmpty()) {
            return list.get(0);
        }
        Position pos = metadata.create(Position.class);
        pos.setPositionRuName(positionName.trim());
        dataManager.commit(pos);
        return pos;
    }

    private Grade findGrade(String gradeName) {
        if (gradeName == null) return null;
        List<Grade> list = dataManager.load(Grade.class)
                .query("select e from hunttech_Grade e where lower(e.gradeName) like lower(:g)")
                .parameter("g", "%" + gradeName.trim() + "%")
                .view("_local")
                .maxResults(1)
                .list();
        return list.isEmpty() ? null : list.get(0);
    }

    private City findCity(String cityName) {
        if (cityName == null) return null;
        List<City> list = dataManager.load(City.class)
                .query("select e from hunttech_City e where lower(e.cityRuName) like lower(:c)")
                .parameter("c", "%" + cityName.trim() + "%")
                .view("city-picker-view")
                .maxResults(1)
                .list();
        return list.isEmpty() ? null : list.get(0);
    }
}
