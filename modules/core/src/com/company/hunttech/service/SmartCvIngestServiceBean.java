package com.company.hunttech.service;

import com.company.hunttech.entity.CandidateCV;
import com.company.hunttech.entity.CandidateSkill;
import com.company.hunttech.entity.CandidateSkillPriority;
import com.company.hunttech.entity.City;
import com.company.hunttech.entity.Company;
import com.company.hunttech.entity.ExtUser;
import com.company.hunttech.entity.Iteraction;
import com.company.hunttech.entity.IteractionList;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.entity.Position;
import com.company.hunttech.entity.SkillTree;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.core.global.FileStorageException;
import com.haulmont.cuba.core.global.Metadata;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.swing.text.rtf.RTFEditorKit;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service(SmartCvIngestService.NAME)
public class SmartCvIngestServiceBean implements SmartCvIngestService {
    private static final Logger log = LoggerFactory.getLogger(SmartCvIngestServiceBean.class);

    private static final String FUNCTION_CV_SMART_PARSE_JSON = "CV_SMART_PARSE_JSON";

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
                    log.error("Не удалось прочитать файл из FileStorage", e);
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
            } else if ("rtf".equals(ext)) {
                RTFEditorKit rtfKit = new RTFEditorKit();
                javax.swing.text.Document rtfDoc = rtfKit.createDefaultDocument();
                try (Reader reader = new InputStreamReader(new ByteArrayInputStream(fileBytes), StandardCharsets.UTF_8)) {
                    rtfKit.read(reader, rtfDoc, 0);
                    return rtfDoc.getText(0, rtfDoc.getLength());
                }
            } else if ("pages".equals(ext)) {
                // Apple Pages - zip-пакет с Preview.pdf или QuickLook
                try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(fileBytes))) {
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        String name = entry.getName();
                        if (name.endsWith("Preview.pdf") || name.contains("QuickLook/Preview.pdf")) {
                            byte[] pdfBytes = zis.readAllBytes();
                            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                                PDFTextStripper stripper = new PDFTextStripper();
                                return stripper.getText(document);
                            }
                        }
                    }
                }
            }
            // Fallback plain text
            return new String(fileBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Ошибка извлечения текста из файла резюме (" + ext + "): " + e.getMessage(), e);
            return "";
        }
    }

    @Override
    public SmartCvParsedData parseCvText(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return new SmartCvParsedData();
        }

        Map<String, Object> ctx = Collections.singletonMap("sourceText", rawText);
        AiExecutionResult aiResult = aiExecutionService.executeText(FUNCTION_CV_SMART_PARSE_JSON, ctx);
        String jsonText = aiResult != null ? aiResult.getText() : null;

        SmartCvParsedData data = new SmartCvParsedData();
        data.setRawText(rawText);

        if (jsonText != null && !jsonText.trim().isEmpty()) {
            try {
                String cleanJson = extractCleanJson(jsonText);
                JsonNode root = objectMapper.readTree(cleanJson);

                if (root.hasNonNull("lastName")) data.setLastName(root.get("lastName").asText().trim());
                if (root.hasNonNull("firstName")) data.setFirstName(root.get("firstName").asText().trim());
                if (root.hasNonNull("middleName")) data.setMiddleName(root.get("middleName").asText().trim());
                if (root.hasNonNull("birthDate")) data.setBirthDate(root.get("birthDate").asText().trim());
                if (root.hasNonNull("phone")) data.setPhone(root.get("phone").asText().trim());
                if (root.hasNonNull("mobilePhone")) data.setMobilePhone(root.get("mobilePhone").asText().trim());
                if (root.hasNonNull("email")) data.setEmail(root.get("email").asText().trim().toLowerCase());
                if (root.hasNonNull("telegram")) data.setTelegram(cleanTelegram(root.get("telegram").asText().trim()));
                if (root.hasNonNull("skype")) data.setSkype(root.get("skype").asText().trim());
                if (root.hasNonNull("whatsapp")) data.setWhatsapp(root.get("whatsapp").asText().trim());
                if (root.hasNonNull("position")) data.setPosition(root.get("position").asText().trim());
                if (root.hasNonNull("city")) data.setCity(root.get("city").asText().trim());
                if (root.hasNonNull("currentCompany")) data.setCurrentCompany(root.get("currentCompany").asText().trim());
                if (root.hasNonNull("salary")) data.setSalary(root.get("salary").asText().trim());
                if (root.hasNonNull("summary")) data.setSummary(root.get("summary").asText().trim());
                if (root.hasNonNull("experienceYears")) data.setExperienceYears(root.get("experienceYears").asInt());

                if (root.has("skills") && root.get("skills").isArray()) {
                    List<String> skillsList = new ArrayList<>();
                    for (JsonNode sn : root.get("skills")) {
                        String s = sn.asText().trim();
                        if (!s.isEmpty()) {
                            skillsList.add(s);
                        }
                    }
                    data.setSkills(skillsList);
                }
            } catch (Exception e) {
                log.error("Ошибка парсинга JSON ответа AI: " + e.getMessage(), e);
            }
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

    private String cleanTelegram(String tg) {
        if (tg == null) return null;
        tg = tg.trim();
        if (tg.startsWith("@")) tg = tg.substring(1);
        if (tg.startsWith("https://t.me/")) tg = tg.substring(13);
        if (tg.startsWith("t.me/")) tg = tg.substring(5);
        return tg.trim();
    }

    @Override
    public JobCandidate findDuplicate(SmartCvParsedData data) {
        if (data == null) return null;

        // 1. Поиск по нормализованному телефону (последние 10 цифр)
        String phoneDigits = normalizeDigits(data.getPhone());
        if (phoneDigits.length() >= 10) {
            String last10 = phoneDigits.substring(phoneDigits.length() - 10);
            List<JobCandidate> list = dataManager.load(JobCandidate.class)
                    .query("select e from hunttech_JobCandidate e where e.phone like :digits or e.mobilePhone like :digits")
                    .parameter("digits", "%" + last10 + "%")
                    .view("jobCandidate-view")
                    .list();
            if (!list.isEmpty()) {
                return list.get(0);
            }
        }

        // 2. Поиск по Email
        if (data.getEmail() != null && !data.getEmail().trim().isEmpty()) {
            List<JobCandidate> list = dataManager.load(JobCandidate.class)
                    .query("select e from hunttech_JobCandidate e where lower(e.email) = :email")
                    .parameter("email", data.getEmail().trim().toLowerCase())
                    .view("jobCandidate-view")
                    .list();
            if (!list.isEmpty()) {
                return list.get(0);
            }
        }

        // 3. Поиск по Telegram
        if (data.getTelegram() != null && !data.getTelegram().trim().isEmpty()) {
            List<JobCandidate> list = dataManager.load(JobCandidate.class)
                    .query("select e from hunttech_JobCandidate e where lower(e.telegramName) = :tg")
                    .parameter("tg", data.getTelegram().trim().toLowerCase())
                    .view("jobCandidate-view")
                    .list();
            if (!list.isEmpty()) {
                return list.get(0);
            }
        }

        // 4. Поиск по ФИО
        if (data.getFirstName() != null && !data.getFirstName().trim().isEmpty()
                && data.getLastName() != null && !data.getLastName().trim().isEmpty()) {
            List<JobCandidate> list = dataManager.load(JobCandidate.class)
                    .query("select e from hunttech_JobCandidate e where lower(e.firstName) = :fn and lower(e.secondName) = :sn")
                    .parameter("fn", data.getFirstName().trim().toLowerCase())
                    .parameter("sn", data.getLastName().trim().toLowerCase())
                    .view("jobCandidate-view")
                    .list();
            if (!list.isEmpty()) {
                return list.get(0);
            }
        }

        return null;
    }

    private String normalizeDigits(String phone) {
        if (phone == null) return "";
        return phone.replaceAll("\\D", "");
    }

    @Override
    public SmartCvIngestResult createNewCandidate(SmartCvParsedData data, FileDescriptor fileDescriptor, FileDescriptor faceImage, ExtUser recruiter) {
        CommitContext commitContext = new CommitContext();

        JobCandidate candidate = metadata.create(JobCandidate.class);
        candidate.setFirstName(data.getFirstName() != null && !data.getFirstName().isEmpty() ? data.getFirstName() : "Кандидат");
        candidate.setSecondName(data.getLastName() != null && !data.getLastName().isEmpty() ? data.getLastName() : "Новый");
        candidate.setMiddleName(data.getMiddleName());
        candidate.setFullName(candidate.getSecondName() + " " + candidate.getFirstName() + (candidate.getMiddleName() != null ? " " + candidate.getMiddleName() : ""));

        candidate.setPhone(data.getPhone());
        candidate.setMobilePhone(data.getMobilePhone());
        candidate.setEmail(data.getEmail());
        candidate.setTelegramName(data.getTelegram());
        candidate.setSkypeName(data.getSkype());
        candidate.setWhatsupName(data.getWhatsapp());
        candidate.setFileImageFace(faceImage);

        if (data.getBirthDate() != null && !data.getBirthDate().isEmpty()) {
            try {
                String bd = data.getBirthDate().trim();
                SimpleDateFormat sdf = bd.contains("-")
                        ? new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
                        : new SimpleDateFormat("dd.MM.yyyy", Locale.ROOT);
                candidate.setBirdhDate(sdf.parse(bd));
            } catch (Exception e) {
                log.warn("Некорректный формат даты рождения '{}': {}", data.getBirthDate(), e.getMessage());
            }
        }

        // Разрешение подчиненных справочников
        Position position = resolvePosition(data.getPosition(), commitContext);
        if (position != null) {
            candidate.setPersonPosition(position);
        }

        City city = resolveCity(data.getCity(), commitContext);
        if (city != null) {
            candidate.setCityOfResidence(city);
        }

        Company company = resolveCompany(data.getCurrentCompany(), commitContext);
        if (company != null) {
            candidate.setCurrentCompany(company);
        }

        commitContext.addInstanceToCommit(candidate);

        // Создание CandidateCV
        CandidateCV cv = metadata.create(CandidateCV.class);
        cv.setCandidate(candidate);
        cv.setResumePosition(position);
        cv.setTextCV(data.getRawText() != null ? data.getRawText() : "");
        cv.setFileCV(fileDescriptor);
        cv.setOriginalFileCV(fileDescriptor);
        cv.setFileImageFace(faceImage);
        cv.setDatePost(new Date());
        cv.setOwner(recruiter);
        commitContext.addInstanceToCommit(cv);

        // Создание CandidateSkills
        if (data.getSkills() != null && !data.getSkills().isEmpty()) {
            Set<UUID> addedSkills = new HashSet<>();
            for (String sName : data.getSkills()) {
                SkillTree skill = resolveSkill(sName, commitContext);
                if (skill != null && addedSkills.add(skill.getId())) {
                    CandidateSkill cs = metadata.create(CandidateSkill.class);
                    cs.setCandidate(candidate);
                    cs.setSkill(skill);
                    cs.setPriority(CandidateSkillPriority.MAIN);
                    commitContext.addInstanceToCommit(cs);
                }
            }
        }

        // Создание взаимодействия «Новый кандидат»
        Iteraction interactionType = resolveNewCandidateInteractionType();
        if (interactionType != null) {
            IteractionList interaction = metadata.create(IteractionList.class);
            interaction.setCandidate(candidate);
            interaction.setRecrutier(recruiter);
            interaction.setDateIteraction(new Date());
            interaction.setIteractionType(interactionType);
            if (data.getSalary() != null && !data.getSalary().isEmpty()) {
                interaction.setAddString("Зарплатные ожидания: " + data.getSalary());
            }
            interaction.setComment("Кандидат автоматически импортирован из файла " + (fileDescriptor != null ? fileDescriptor.getName() : "резюме"));
            commitContext.addInstanceToCommit(interaction);
        }

        dataManager.commit(commitContext);

        List<String> missing = validateMissingFields(candidate);
        return SmartCvIngestResult.success(candidate, cv, data, missing, null);
    }

    @Override
    public SmartCvIngestResult attachCvToExistingCandidate(UUID existingCandidateId, SmartCvParsedData data, FileDescriptor fileDescriptor, FileDescriptor faceImage, ExtUser recruiter) {
        JobCandidate existing = dataManager.load(JobCandidate.class)
                .id(existingCandidateId)
                .view("jobCandidate-view")
                .one();

        CommitContext commitContext = new CommitContext();

        // Дополняем пустые поля существующего кандидата без затирания существующих данных
        boolean updated = false;
        if (existing.getPhone() == null && data.getPhone() != null) {
            existing.setPhone(data.getPhone());
            updated = true;
        }
        if (existing.getEmail() == null && data.getEmail() != null) {
            existing.setEmail(data.getEmail());
            updated = true;
        }
        if (existing.getTelegramName() == null && data.getTelegram() != null) {
            existing.setTelegramName(data.getTelegram());
            updated = true;
        }
        if (existing.getCityOfResidence() == null && data.getCity() != null) {
            City city = resolveCity(data.getCity(), commitContext);
            if (city != null) {
                existing.setCityOfResidence(city);
                updated = true;
            }
        }
        if (existing.getCurrentCompany() == null && data.getCurrentCompany() != null) {
            Company company = resolveCompany(data.getCurrentCompany(), commitContext);
            if (company != null) {
                existing.setCurrentCompany(company);
                updated = true;
            }
        }
        if (existing.getFileImageFace() == null && faceImage != null) {
            existing.setFileImageFace(faceImage);
            updated = true;
        }

        if (updated) {
            commitContext.addInstanceToCommit(existing);
        }

        // Создаем новую версию CandidateCV
        CandidateCV cv = metadata.create(CandidateCV.class);
        cv.setCandidate(existing);
        cv.setResumePosition(resolvePosition(data.getPosition(), commitContext));
        cv.setTextCV(data.getRawText() != null ? data.getRawText() : "");
        cv.setFileCV(fileDescriptor);
        cv.setOriginalFileCV(fileDescriptor);
        cv.setFileImageFace(faceImage);
        cv.setDatePost(new Date());
        cv.setOwner(recruiter);
        commitContext.addInstanceToCommit(cv);

        // Добавляем новое взаимодействие об обновлении резюме
        Iteraction interactionType = resolveNewCandidateInteractionType();
        if (interactionType != null) {
            IteractionList interaction = metadata.create(IteractionList.class);
            interaction.setCandidate(existing);
            interaction.setRecrutier(recruiter);
            interaction.setDateIteraction(new Date());
            interaction.setIteractionType(interactionType);
            if (data.getSalary() != null && !data.getSalary().isEmpty()) {
                interaction.setAddString("Зарплатные ожидания: " + data.getSalary());
            }
            interaction.setComment("Загружена новая версия резюме из файла " + (fileDescriptor != null ? fileDescriptor.getName() : "резюме"));
            commitContext.addInstanceToCommit(interaction);
        }

        dataManager.commit(commitContext);

        List<String> missing = validateMissingFields(existing);
        return SmartCvIngestResult.success(existing, cv, data, missing, null);
    }

    private List<String> validateMissingFields(JobCandidate c) {
        List<String> missing = new ArrayList<>();
        if (c.getFirstName() == null || c.getFirstName().trim().isEmpty() || "Кандидат".equals(c.getFirstName())) {
            missing.add("Имя");
        }
        if (c.getSecondName() == null || c.getSecondName().trim().isEmpty() || "Новый".equals(c.getSecondName())) {
            missing.add("Фамилия");
        }
        if (c.getPersonPosition() == null) {
            missing.add("Должность");
        }
        if (c.getCityOfResidence() == null) {
            missing.add("Город проживания");
        }
        if ((c.getPhone() == null || c.getPhone().isEmpty()) && (c.getEmail() == null || c.getEmail().isEmpty()) && (c.getTelegramName() == null || c.getTelegramName().isEmpty())) {
            missing.add("Контакты (телефон/email/telegram)");
        }
        return missing;
    }

    private Position resolvePosition(String name, CommitContext commitContext) {
        if (name == null || name.trim().isEmpty()) return null;
        name = name.trim();
        List<Position> list = dataManager.load(Position.class)
                .query("select e from hunttech_Position e where lower(e.positionRuName) = :name or lower(e.positionEnName) = :name")
                .parameter("name", name.toLowerCase())
                .list();
        if (!list.isEmpty()) {
            return list.get(0);
        }
        Position pos = metadata.create(Position.class);
        pos.setPositionRuName(name.length() > 80 ? name.substring(0, 80) : name);
        if (commitContext != null) {
            commitContext.addInstanceToCommit(pos);
        } else {
            dataManager.commit(pos);
        }
        return pos;
    }

    private City resolveCity(String name, CommitContext commitContext) {
        if (name == null || name.trim().isEmpty()) return null;
        name = name.trim();
        List<City> list = dataManager.load(City.class)
                .query("select e from hunttech_City e where lower(e.cityRuName) = :name")
                .parameter("name", name.toLowerCase())
                .list();
        if (!list.isEmpty()) {
            return list.get(0);
        }
        City city = metadata.create(City.class);
        city.setCityRuName(name.length() > 50 ? name.substring(0, 50) : name);
        if (commitContext != null) {
            commitContext.addInstanceToCommit(city);
        } else {
            dataManager.commit(city);
        }
        return city;
    }

    private Company resolveCompany(String name, CommitContext commitContext) {
        if (name == null || name.trim().isEmpty()) return null;
        name = name.trim();
        List<Company> list = dataManager.load(Company.class)
                .query("select e from hunttech_Company e where lower(e.comanyName) = :name or lower(e.companyShortName) = :name")
                .parameter("name", name.toLowerCase())
                .list();
        if (!list.isEmpty()) {
            return list.get(0);
        }
        Company comp = metadata.create(Company.class);
        comp.setComanyName(name.length() > 80 ? name.substring(0, 80) : name);
        if (commitContext != null) {
            commitContext.addInstanceToCommit(comp);
        } else {
            dataManager.commit(comp);
        }
        return comp;
    }

    private SkillTree resolveSkill(String name, CommitContext commitContext) {
        if (name == null || name.trim().isEmpty()) return null;
        name = name.trim();
        List<SkillTree> list = dataManager.load(SkillTree.class)
                .query("select e from hunttech_SkillTree e where lower(e.skillName) = :name")
                .parameter("name", name.toLowerCase())
                .list();
        if (!list.isEmpty()) {
            return list.get(0);
        }
        SkillTree st = metadata.create(SkillTree.class);
        st.setSkillName(name.length() > 80 ? name.substring(0, 80) : name);
        if (commitContext != null) {
            commitContext.addInstanceToCommit(st);
        } else {
            dataManager.commit(st);
        }
        return st;
    }

    private Iteraction resolveNewCandidateInteractionType() {
        try {
            List<Iteraction> list = dataManager.load(Iteraction.class)
                    .query("select e from hunttech_Iteraction e where lower(e.iteractionTree.iteractionRuName) like :name")
                    .parameter("name", "%новый кандидат%")
                    .view("iteraction-view")
                    .list();
            if (!list.isEmpty()) {
                return list.get(0);
            }
            return dataManager.load(Iteraction.class)
                    .query("select e from hunttech_Iteraction e order by e.createTs asc")
                    .maxResults(1)
                    .view("iteraction-view")
                    .optional()
                    .orElse(null);
        } catch (Exception e) {
            log.error("Не удалось определить тип взаимодействия: " + e.getMessage());
            return null;
        }
    }
}
