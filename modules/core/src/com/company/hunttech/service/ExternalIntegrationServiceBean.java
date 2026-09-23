package com.company.hunttech.service;

import com.company.hunttech.dto.integration.CandidateCompositeCreateRequestDto;
import com.company.hunttech.dto.integration.CandidateCompositeResponseDto;
import com.company.hunttech.dto.integration.CandidateCvCreateRequestDto;
import com.company.hunttech.dto.integration.CandidateCvResponseDto;
import com.company.hunttech.dto.integration.CompanyCreateRequestDto;
import com.company.hunttech.dto.integration.CompanyResponseDto;
import com.company.hunttech.dto.integration.InteractionCreateRequestDto;
import com.company.hunttech.dto.integration.InteractionResponseDto;
import com.company.hunttech.dto.integration.ProjectLogoResponseDto;
import com.company.hunttech.dto.integration.ProjectLogoUploadRequestDto;
import com.company.hunttech.dto.integration.ProjectVacancyCreateRequestDto;
import com.company.hunttech.dto.integration.ProjectVacancyResponseDto;
import com.company.hunttech.entity.CandidateCV;
import com.company.hunttech.entity.City;
import com.company.hunttech.entity.Company;
import com.company.hunttech.entity.CompanyDepartament;
import com.company.hunttech.entity.Country;
import com.company.hunttech.entity.ExtUser;
import com.company.hunttech.entity.Grade;
import com.company.hunttech.entity.Iteraction;
import com.company.hunttech.entity.IteractionList;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.entity.OpenPosition;
import com.company.hunttech.entity.OutstaffingRates;
import com.company.hunttech.entity.Person;
import com.company.hunttech.entity.Position;
import com.company.hunttech.entity.Project;
import com.company.hunttech.entity.Region;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.UserSessionSource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service(ExternalIntegrationService.NAME)
public class ExternalIntegrationServiceBean implements ExternalIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(ExternalIntegrationServiceBean.class);
    private static final Pattern SSP_PATTERN = Pattern.compile("(?i)\\b(?:ssp|ссп)\\b");

    private static final int MAX_LOGO_SIZE_BYTES = 5 * 1024 * 1024;
    private static final int MAX_BASE64_LOGO_CHARS = 7 * 1024 * 1024;
    private static final int MAX_IMAGE_DIMENSION = 4096;

    // In-memory кэш ответов для идемпотентности запросов в рамках процесса (BL-2026-030/036)
    private final Map<String, CompanyResponseDto> idempotencyCache = new ConcurrentHashMap<>();
    private final Map<String, ProjectVacancyResponseDto> projectVacancyIdempotencyCache = new ConcurrentHashMap<>();
    private final Map<String, CandidateCvResponseDto> candidateCvIdempotencyCache = new ConcurrentHashMap<>();
    private final Map<String, InteractionResponseDto> interactionIdempotencyCache = new ConcurrentHashMap<>();
    private final Map<String, CandidateCompositeResponseDto> candidateCompositeIdempotencyCache = new ConcurrentHashMap<>();
    private final Map<String, ProjectLogoResponseDto> projectLogoIdempotencyCache = new ConcurrentHashMap<>();

    private final Object companyCreateLock = new Object();
    private final Object projectVacancyLock = new Object();
    private final Object candidateCvLock = new Object();
    private final Object interactionLock = new Object();
    private final Object candidateCompositeLock = new Object();
    private final Object interactionNumberLock = new Object();

    @Inject
    private DataManager dataManager;

    @Inject
    private Metadata metadata;

    @Inject
    private UserSessionSource userSessionSource;

    @Inject
    private HrmAiService hrmAiService;

    @Inject
    private AiExecutionService aiExecutionService;

    @Inject
    private SmartOpenPositionIngestService smartOpenPositionIngestService;

    private volatile ExtUser cachedHunttechUser;

    @Override
    public CompanyResponseDto createCompany(CompanyCreateRequestDto request) {
        if (request == null) {
            return CompanyResponseDto.error("Тело запроса отсутствует", null, "VALIDATION_ERROR");
        }

        String correlationId = request.getCorrelationId();
        String idempotencyKey = StringUtils.trimToNull(request.getIdempotencyKey());

        // Проверка идемпотентности по Idempotency-Key
        if (idempotencyKey != null) {
            CompanyResponseDto cached = idempotencyCache.get(idempotencyKey);
            if (cached != null) {
                log.info("Returning cached response for idempotencyKey: {} [correlationId={}]", idempotencyKey, correlationId);
                return cached;
            }
        }

        String companyName = request.getCompanyName();
        if (StringUtils.isBlank(companyName)) {
            return CompanyResponseDto.error("Наименование компании обязательно для заполнения", correlationId, "VALIDATION_ERROR");
        }

        companyName = companyName.trim();
        String truncatedName = truncate(companyName, 80);
        String inn = StringUtils.trimToNull(request.getInn());
        String kpp = StringUtils.trimToNull(request.getKpp());
        String ogrn = StringUtils.trimToNull(request.getOgrn());

        // Валидация форматов реквизитов при их наличии
        if (inn != null && !inn.matches("\\d{10}|\\d{12}")) {
            return CompanyResponseDto.error("ИНН должен содержать 10 или 12 цифр", correlationId, "VALIDATION_ERROR");
        }
        if (kpp != null && !kpp.matches("\\d{9}")) {
            return CompanyResponseDto.error("КПП должен содержать 9 цифр", correlationId, "VALIDATION_ERROR");
        }
        if (ogrn != null && !ogrn.matches("\\d{13}|\\d{15}")) {
            return CompanyResponseDto.error("ОГРН должен содержать 13 или 15 цифр", correlationId, "VALIDATION_ERROR");
        }

        try {
            CompanyResponseDto response;
            synchronized (companyCreateLock) {
                if (idempotencyKey != null) {
                    CompanyResponseDto cached = idempotencyCache.get(idempotencyKey);
                    if (cached != null) {
                        log.info("Returning cached response inside lock for idempotencyKey: {} [correlationId={}]", idempotencyKey, correlationId);
                        return cached;
                    }
                }

                // 1. Дедупликация по ИНН
                if (inn != null) {
                    Company existingByInn = dataManager.load(Company.class)
                            .query("select e from hunttech_Company e where e.inn = :inn")
                            .parameter("inn", inn)
                            .optional()
                            .orElse(null);

                    if (existingByInn != null) {
                        log.info("Company deduplicated by INN: {} -> id={}", inn, existingByInn.getId());
                        response = CompanyResponseDto.ok(
                                existingByInn.getId().toString(),
                                request.getExternalId(),
                                existingByInn.getComanyName(),
                                CompanyResponseDto.STATUS_EXISTING_FOUND,
                                correlationId
                        );
                        cacheIfIdempotent(idempotencyKey, response);
                        return response;
                    }
                }

                // 2. Дедупликация по точному нормализованному наименованию
                Company existingByName = dataManager.load(Company.class)
                        .query("select e from hunttech_Company e where lower(e.comanyName) = :name")
                        .parameter("name", truncatedName.toLowerCase(Locale.ROOT))
                        .optional()
                        .orElse(null);

                if (existingByName != null) {
                    log.info("Company deduplicated by name: {} -> id={}", truncatedName, existingByName.getId());
                    response = CompanyResponseDto.ok(
                            existingByName.getId().toString(),
                            request.getExternalId(),
                            existingByName.getComanyName(),
                            CompanyResponseDto.STATUS_EXISTING_FOUND,
                            correlationId
                    );
                    cacheIfIdempotent(idempotencyKey, response);
                    return response;
                }

                // 3. Создание новой записи Company
                Company company = metadata.create(Company.class);
                company.setComanyName(truncatedName);
                company.setCompanyShortName(truncate(request.getCompanyShortName(), 80));
                company.setLegalEntityName(truncate(request.getLegalEntityName(), 255));
                company.setInn(inn);
                company.setKpp(kpp);
                company.setOgrn(ogrn);
                company.setAddressOfCompany(request.getAddressOfCompany());
                company.setWebsite(truncate(request.getWebsite(), 255));

                if (StringUtils.isNotBlank(request.getCityId())) {
                    try {
                        UUID cityUuid = UUID.fromString(request.getCityId().trim());
                        City city = dataManager.load(City.class).id(cityUuid).optional().orElse(null);
                        company.setCityOfCompany(city);
                    } catch (IllegalArgumentException ex) {
                        log.warn("Invalid cityId format [correlationId={}]: {}", correlationId, request.getCityId(), ex);
                    }
                }

                if (StringUtils.isNotBlank(request.getCountryId())) {
                    try {
                        UUID countryUuid = UUID.fromString(request.getCountryId().trim());
                        Country country = dataManager.load(Country.class).id(countryUuid).optional().orElse(null);
                        company.setCountryOfCompany(country);
                    } catch (IllegalArgumentException ex) {
                        log.warn("Invalid countryId format [correlationId={}]: {}", correlationId, request.getCountryId(), ex);
                    }
                }

                if (StringUtils.isNotBlank(request.getRegionId())) {
                    try {
                        UUID regionUuid = UUID.fromString(request.getRegionId().trim());
                        Region region = dataManager.load(Region.class).id(regionUuid).optional().orElse(null);
                        company.setRegionOfCompany(region);
                    } catch (IllegalArgumentException ex) {
                        log.warn("Invalid regionId format [correlationId={}]: {}", correlationId, request.getRegionId(), ex);
                    }
                }

                company.setOurClient(false);
                dataManager.commit(company);

                log.info("Successfully created company: id={}, name='{}' [correlationId={}]", company.getId(), company.getComanyName(), correlationId);
                response = CompanyResponseDto.ok(
                        company.getId().toString(),
                        request.getExternalId(),
                        company.getComanyName(),
                        CompanyResponseDto.STATUS_CREATED,
                        correlationId
                );
                cacheIfIdempotent(idempotencyKey, response);
                return response;
            }

        } catch (Exception e) {
            log.error("Failed to create company for external API [correlationId={}]", correlationId, e);
            return CompanyResponseDto.error("Ошибка при сохранении компании. Correlation ID: " + correlationId, correlationId, "SYSTEM_ERROR");
        }
    }

    @Override
    public com.company.hunttech.dto.integration.ProjectVacancyResponseDto createProjectAndVacancy(
            com.company.hunttech.dto.integration.ProjectVacancyCreateRequestDto request) {
        if (request == null) {
            return com.company.hunttech.dto.integration.ProjectVacancyResponseDto.error("Тело запроса отсутствует", null, "VALIDATION_ERROR");
        }

        String correlationId = request.getCorrelationId();
        String idempotencyKey = StringUtils.trimToNull(request.getIdempotencyKey());

        // Проверка идемпотентности по Idempotency-Key
        if (idempotencyKey != null) {
            com.company.hunttech.dto.integration.ProjectVacancyResponseDto cached = projectVacancyIdempotencyCache.get(idempotencyKey);
            if (cached != null) {
                log.info("Returning cached projectVacancy response for idempotencyKey: {} [correlationId={}]", idempotencyKey, correlationId);
                return cached;
            }
        }

        if (StringUtils.isBlank(request.getVacancyName())) {
            return com.company.hunttech.dto.integration.ProjectVacancyResponseDto.error(
                    "Наименование вакансии (vacancyName) обязательно для заполнения", correlationId, "VALIDATION_ERROR");
        }

        boolean hasExistingProject = StringUtils.isNotBlank(request.getExistingProjectId());

        // Предварительная валидация existingProjectId (если задан)
        if (hasExistingProject) {
            try {
                UUID projectUuid = UUID.fromString(request.getExistingProjectId().trim());
                Project existingProj = dataManager.load(Project.class)
                        .id(projectUuid).optional().orElse(null);
                if (existingProj == null) {
                    return com.company.hunttech.dto.integration.ProjectVacancyResponseDto.error(
                            "Проект с указанным existingProjectId не найден: " + request.getExistingProjectId(),
                            correlationId, "NOT_FOUND");
                }
            } catch (IllegalArgumentException ex) {
                return com.company.hunttech.dto.integration.ProjectVacancyResponseDto.error(
                        "Некорректный формат UUID для existingProjectId: " + request.getExistingProjectId(),
                        correlationId, "VALIDATION_ERROR");
            }
        }

        // Быстрая проверка идемпотентности ДО долгих вызовов AI
        if (idempotencyKey != null) {
            ProjectVacancyResponseDto cached = projectVacancyIdempotencyCache.get(idempotencyKey);
            if (cached != null) {
                log.info("Returning early cached projectVacancy response for idempotencyKey: {} [correlationId={}]", idempotencyKey, correlationId);
                return cached;
            }
        }

        // Оригинал вакансии для сохранения в rawDescription и генерации AI-артефактов
        String rawText = StringUtils.isNotBlank(request.getComment())
                ? request.getComment().trim()
                : StringUtils.trimToEmpty(request.getShortDescription());

        // 1. Первичный AI-парсинг описания вакансии через SmartOpenPositionIngestService
        SmartOpenPositionParsedData parsedData = null;
        if (StringUtils.isNotBlank(rawText)) {
            try {
                log.info("Parsing vacancy via SmartOpenPositionIngestService [name='{}', correlationId={}]",
                        request.getVacancyName(), correlationId);
                parsedData = smartOpenPositionIngestService.parseVacancyText(rawText);
            } catch (Exception ex) {
                log.warn("Failed to parse vacancy text via SmartOpenPositionIngestService [correlationId={}]: {}", correlationId, ex.getMessage(), ex);
            }
        }

        // 2. Умное AI-определение наименования должности
        Position positionType = null;
        if (StringUtils.isNotBlank(request.getPositionTypeId())) {
            try {
                UUID posUuid = UUID.fromString(request.getPositionTypeId().trim());
                positionType = dataManager.load(Position.class).id(posUuid).optional().orElse(null);
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid positionTypeId format [correlationId={}]: {}", correlationId, request.getPositionTypeId(), ex);
            }
        }
        if (positionType == null) {
            String candidatePosName = StringUtils.isNotBlank(request.getPositionName())
                    ? request.getPositionName()
                    : (parsedData != null && StringUtils.isNotBlank(parsedData.getPositionTypeName())
                        ? parsedData.getPositionTypeName()
                        : request.getVacancyName());
            positionType = findBestMatchingPositionType(candidatePosName);
            if (positionType == null && StringUtils.isNotBlank(request.getVacancyName())) {
                positionType = findBestMatchingPositionType(request.getVacancyName());
            }
        }

        // 3. Разрешение Заказчика (Company) и Контакта (Person), если existingProjectId не указан
        Company customerCompany = null;
        Person customerContact = null;
        if (!hasExistingProject) {
            customerCompany = resolveCustomerCompany(
                    request.getCompanyId(),
                    request.getCompanyName(),
                    parsedData != null ? parsedData.getCompanyName() : null,
                    rawText
            );
            if (customerCompany == null) {
                return com.company.hunttech.dto.integration.ProjectVacancyResponseDto.error(
                        "Не удалось определить компанию заказчика (клиента) из описания вакансии или переданных параметров. Заполните поле companyName/companyId или укажите заказчика в тексте вакансии.",
                        correlationId, "CUSTOMER_NOT_FOUND");
            }

            customerContact = resolveCustomerContact(
                    request.getCustomerContact(),
                    rawText,
                    customerCompany
            );
            if (customerContact == null) {
                return com.company.hunttech.dto.integration.ProjectVacancyResponseDto.error(
                        "Не удалось найти контактное лицо со стороны заказчика в справочнике 'Люди'. Укажите ФИО или Telegram ответственного заказчика в описании вакансии или поле customerContact.",
                        correlationId, "CUSTOMER_CONTACT_NOT_FOUND");
            }
        }

        // 4. Внутренняя AI-генерация 4 артефактов вакансии (если отсутствуют в parsedData):
        // 1) Описание вакансии (STANDARDIZE_VACANCY)
        // 2) Чеклист (VACANCY_CHECKLIST)
        // 3) Карта поиска (VACANCY_SEARCH_MAP)
        // 4) План собеседования (VACANCY_INTERVIEW_PLAN)
        String standardizedDescription = (parsedData != null && StringUtils.isNotBlank(parsedData.getComment()))
                ? parsedData.getComment() : null;
        String checklist = (parsedData != null && StringUtils.isNotBlank(parsedData.getInterviewChecklist()))
                ? parsedData.getInterviewChecklist() : null;
        String searchMap = (parsedData != null && StringUtils.isNotBlank(parsedData.getSearchMap()))
                ? parsedData.getSearchMap() : null;
        String interviewPlan = (parsedData != null && StringUtils.isNotBlank(parsedData.getInterviewPlan()))
                ? parsedData.getInterviewPlan() : null;

        if (StringUtils.isNotBlank(rawText)) {
            if (standardizedDescription == null) {
                try {
                    log.info("Starting AI vacancy standardization for external vacancy [name='{}', correlationId={}]",
                            request.getVacancyName(), correlationId);
                    standardizedDescription = hrmAiService.standardizeVacancyDescription(rawText);
                } catch (Exception ex) {
                    log.warn("Failed to standardize vacancy description via AI [correlationId={}]: {}", correlationId, ex.getMessage(), ex);
                }
            }

            String textForArtifacts = StringUtils.isNotBlank(standardizedDescription) ? standardizedDescription : rawText;

            if (checklist == null) {
                try {
                    checklist = hrmAiService.generateChecklist(textForArtifacts);
                } catch (Exception ex) {
                    log.warn("Failed to generate vacancy checklist via AI [correlationId={}]: {}", correlationId, ex.getMessage(), ex);
                }
            }

            if (searchMap == null) {
                try {
                    searchMap = hrmAiService.generateSearchMap(textForArtifacts);
                } catch (Exception ex) {
                    log.warn("Failed to generate vacancy search map via AI [correlationId={}]: {}", correlationId, ex.getMessage(), ex);
                }
            }

            if (interviewPlan == null) {
                try {
                    interviewPlan = hrmAiService.generateInterviewPlan(textForArtifacts);
                } catch (Exception ex) {
                    log.warn("Failed to generate vacancy interview plan via AI [correlationId={}]: {}", correlationId, ex.getMessage(), ex);
                }
            }

            log.info("AI vacancy enrichment completed [correlationId={}]: desc={}, checklist={}, searchMap={}, interviewPlan={}",
                    correlationId,
                    standardizedDescription != null ? "OK" : "FALLBACK",
                    checklist != null ? "OK" : "SKIPPED",
                    searchMap != null ? "OK" : "SKIPPED",
                    interviewPlan != null ? "OK" : "SKIPPED");
        }

        // Предварительная подготовка AI-описания проекта ДО входа в synchronized блок
        String baseProjectName = null;
        String projectDescHtml = null;
        String projectShortDesc = null;
        String actPeriod = null;
        String formattedContact = null;
        String canonicalProjectName = null;

        if (!hasExistingProject) {
            baseProjectName = StringUtils.isNotBlank(request.getProjectName())
                    ? request.getProjectName().trim()
                    : (parsedData != null && StringUtils.isNotBlank(parsedData.getProjectName())
                        ? parsedData.getProjectName().trim()
                        : request.getVacancyName().trim());

            Project existingProjectPreCheck = findExistingProject(baseProjectName, customerContact, customerCompany);
            if (existingProjectPreCheck == null) {
                actPeriod = resolveActPeriod(request.getActPeriod(), rawText);
                formattedContact = formatContactForProjectName(customerContact);
                canonicalProjectName = buildCanonicalProjectName(customerCompany, baseProjectName, formattedContact, actPeriod);

                String projectDescRaw = generateProjectDescription(baseProjectName, rawText);
                projectDescHtml = MarkdownToHtmlUtils.toHtml(projectDescRaw);
                projectShortDesc = generateProjectShortDescription(baseProjectName, projectDescRaw, rawText);
            }
        }

        // Предварительная подготовка AI-перевода описания на английский язык ДО входа в synchronized блок
        String descForCommentPre = StringUtils.isNotBlank(standardizedDescription) ? standardizedDescription : rawText;
        String commentHtmlPre = MarkdownToHtmlUtils.toHtml(descForCommentPre);
        String commentEnHtml = translateVacancyDescriptionToEnglish(commentHtmlPre, parsedData, rawText);

        try {
            com.company.hunttech.dto.integration.ProjectVacancyResponseDto response;
            synchronized (projectVacancyLock) {
                if (idempotencyKey != null) {
                    com.company.hunttech.dto.integration.ProjectVacancyResponseDto cached = projectVacancyIdempotencyCache.get(idempotencyKey);
                    if (cached != null) {
                        log.info("Returning cached projectVacancy response inside lock for idempotencyKey: {} [correlationId={}]", idempotencyKey, correlationId);
                        return cached;
                    }
                }

                CommitContext commitContext = new CommitContext();

                // 5. Поиск или создание проекта
                Project project = null;
                if (hasExistingProject) {
                    UUID projectUuid = UUID.fromString(request.getExistingProjectId().trim());
                    project = dataManager.load(Project.class).id(projectUuid).optional().orElse(null);
                    if (project == null) {
                        return com.company.hunttech.dto.integration.ProjectVacancyResponseDto.error(
                                "Проект с указанным existingProjectId не найден: " + request.getExistingProjectId(),
                                correlationId, "NOT_FOUND");
                    }
                } else {
                    project = findExistingProject(baseProjectName, customerContact, customerCompany);

                    if (project == null) {
                        CompanyDepartament dept = resolveOrCreateDepartment(customerCompany, commitContext);

                        project = metadata.create(Project.class);
                        project.setProjectName(truncate(canonicalProjectName, 160));
                        project.setProjectDescription(projectDescHtml);
                        project.setShortDescription(truncate(projectShortDesc, 250));
                        project.setProjectOwner(customerContact);
                        project.setProjectDepartment(dept);
                        project.setProjectIsClosed(false);
                        project.setStartProjectDate(new Date());

                        if (StringUtils.isNotBlank(request.getProjectLogoBase64())) {
                            ImageValidationResult logoRes = validateAndDecodeLogo(request.getProjectLogoBase64());
                            if (logoRes.isValid()) {
                                project.setProjectLogoBlob(logoRes.bytes);
                            } else {
                                log.warn("Project logo provided in createProjectAndVacancy is invalid [correlationId={}]: {} ({})",
                                        correlationId, logoRes.errorMessage, logoRes.errorCode);
                            }
                        }

                        commitContext.addInstanceToCommit(project);
                        log.info("Creating new Project via corporate naming rule: '{}' (owner={}, dept={}) [correlationId={}]",
                                project.getProjectName(), customerContact.getId(), dept != null ? dept.getId() : null, correlationId);
                    } else {
                        if (StringUtils.isNotBlank(request.getProjectLogoBase64())) {
                            ImageValidationResult logoRes = validateAndDecodeLogo(request.getProjectLogoBase64());
                            if (logoRes.isValid()) {
                                project.setProjectLogoBlob(logoRes.bytes);
                                commitContext.addInstanceToCommit(project);
                            } else {
                                log.warn("Project logo provided in createProjectAndVacancy is invalid [correlationId={}]: {} ({})",
                                        correlationId, logoRes.errorMessage, logoRes.errorCode);
                            }
                        }
                        log.info("Reusing existing Project: '{}' (id={}) [correlationId={}]", project.getProjectName(), project.getId(), correlationId);
                    }
                }

                // 6. Разрешение связей для вакансии (Grade, City)
                Grade grade = null;
                if (StringUtils.isNotBlank(request.getGradeId())) {
                    try {
                        UUID gradeUuid = UUID.fromString(request.getGradeId().trim());
                        grade = dataManager.load(Grade.class).id(gradeUuid).optional().orElse(null);
                    } catch (IllegalArgumentException ex) {
                        log.warn("Invalid gradeId format [correlationId={}]: {}", correlationId, request.getGradeId(), ex);
                    }
                }
                if (grade == null && parsedData != null && StringUtils.isNotBlank(parsedData.getGradeName())) {
                    grade = findGrade(parsedData.getGradeName());
                }
                if (grade == null) {
                    String extractedGrade = extractGradeNameFromText(rawText);
                    if (extractedGrade != null) {
                        grade = findGrade(extractedGrade);
                    }
                }
                if (grade == null && (request.getVacancyName().toLowerCase(Locale.ROOT).contains("senior")
                        || request.getVacancyName().toLowerCase(Locale.ROOT).contains("архитектор")
                        || (positionType != null && StringUtils.trimToEmpty(positionType.getPositionRuName()).toLowerCase(Locale.ROOT).contains("архитектор")))) {
                    grade = findGrade("Senior");
                }

                // Определение формата удаленной работы (1=Удаленно, 0=Офис, 2=Гибрид)
                Integer remoteWork = request.getRemoteWork();
                if (remoteWork == null) {
                    remoteWork = isRemoteWork(null, rawText) ? 1 : 0;
                }

                // Разрешение города: для удаленки выставляется "Регионы РФ (МСК +/- 2 часа)"
                City city = resolveCity(remoteWork, request.getCityId(), request.getCityName(),
                        parsedData != null ? parsedData.getCityName() : null, rawText);

                // 7. Создание сущности OpenPosition и HTML-трансформация AI-артефактов
                OpenPosition openPosition = metadata.create(OpenPosition.class);
                openPosition.setProjectName(project);

                if (StringUtils.isNotBlank(request.getExternalId())) {
                    openPosition.setVacansyID(truncate(request.getExternalId().trim(), 16));
                } else {
                    openPosition.setVacansyID("EXT-" + Math.abs(UUID.randomUUID().hashCode() % 1000000));
                }

                String shortDesc = request.getShortDescription() != null && !request.getShortDescription().trim().isEmpty()
                        ? request.getShortDescription() : request.getVacancyName();
                openPosition.setShortDescription(truncate(shortDesc, 250));

                // Исходный текст сохраняется в rawDescription без изменений
                openPosition.setRawDescription(rawText);

                // Трансформация AI-артефактов из Markdown в HTML перед записью в сущность
                String descForComment = StringUtils.isNotBlank(standardizedDescription) ? standardizedDescription : rawText;
                String commentHtml = MarkdownToHtmlUtils.toHtml(descForComment);
                openPosition.setComment(commentHtml);

                if (StringUtils.isNotBlank(commentEnHtml)) {
                    openPosition.setCommentEn(commentEnHtml);
                }

                if (StringUtils.isNotBlank(checklist)) {
                    String checklistHtml = MarkdownToHtmlUtils.toHtml(checklist);
                    openPosition.setInterviewChecklist(checklistHtml);
                    openPosition.setExercise(checklistHtml);
                    openPosition.setNeedExercise(true);
                }

                if (StringUtils.isNotBlank(searchMap)) {
                    String searchMapHtml = MarkdownToHtmlUtils.toHtml(searchMap);
                    openPosition.setSearchMap(searchMapHtml);
                    openPosition.setMemoForInterview(searchMapHtml);
                    openPosition.setNeedMemoForInterview(true);
                }

                if (StringUtils.isNotBlank(interviewPlan)) {
                    String interviewPlanHtml = MarkdownToHtmlUtils.toHtml(interviewPlan);
                    openPosition.setInterviewPlan(interviewPlanHtml);
                    openPosition.setTemplateLetter(interviewPlanHtml);
                    openPosition.setNeedLetter(true);
                }

                openPosition.setRemoteWork(remoteWork);
                openPosition.setCommandCandidate(request.getCommandCandidate() != null ? request.getCommandCandidate() : 1);

                // Общий опыт работы: из текста либо junior=2, middle=3, senior/lead/architect=5
                int exp = request.getWorkExperience() != null ? request.getWorkExperience()
                        : resolveWorkExperience(rawText, request.getVacancyName(), grade, positionType);
                openPosition.setWorkExperience(exp);

                // Оформление (0=Аутстаффинг, 1=Рекрутинг) и расчет ставки из OutstaffingRates
                BigDecimal customerRate = request.getOutstaffingCost() != null ? request.getOutstaffingCost()
                        : (parsedData != null && parsedData.getOutstaffingCost() != null ? parsedData.getOutstaffingCost() : extractCustomerRate(rawText));
                int registrationForWork = resolveRegistrationForWork(rawText, customerRate, request.getSalaryMax());
                openPosition.setRegistrationForWork(registrationForWork);

                if (registrationForWork == 0) {
                    // Аутстаффинг
                    if (customerRate != null) {
                        openPosition.setOutstaffingCost(customerRate);
                        OutstaffingRates rateRow = findOutstaffingRate(customerRate);
                        if (rateRow != null) {
                            openPosition.setSalaryMin(rateRow.getMinSalary());
                            openPosition.setSalaryMax(rateRow.getMaxSalary());
                            openPosition.setSalaryIE(rateRow.getMaxIESalary());
                            openPosition.setSalaryCandidateRequest(false);
                            openPosition.setSalaryComment("Маржинальная ставка: " + customerRate + " \n"
                                    + "Зарплатное предложение min: " + rateRow.getMinSalary() + " \n"
                                    + "Зарплатное предложение max: " + rateRow.getMaxSalary() + " \n"
                                    + "Зарплатное предложение для ИП: " + rateRow.getMaxIESalary());
                        } else {
                            openPosition.setSalaryCandidateRequest(true);
                        }
                    } else {
                        openPosition.setOutstaffingCost(null);
                        openPosition.setSalaryCandidateRequest(true); // флаг "Ориентируемся на запрос кандидата"
                        openPosition.setSalaryMin(null);
                        openPosition.setSalaryMax(null);
                        openPosition.setSalaryIE(null);
                    }
                } else {
                    // Рекрутинг
                    openPosition.setSalaryCandidateRequest(false);
                    openPosition.setSalaryMin(request.getSalaryMin());
                    openPosition.setSalaryMax(request.getSalaryMax());
                    openPosition.setSalaryIE(null);
                    openPosition.setOutstaffingCost(null);
                }

                // Автор записи - "hunttech"
                ExtUser hunttechUser = resolveHunttechUser();
                if (hunttechUser != null) {
                    openPosition.setOwner(hunttechUser);
                }
                openPosition.setCreatedBy("hunttech");

                // Дата закрытия (если в тексте есть "резюме принимаются до...")
                Date closingDate = resolveClosingDate(rawText);
                if (closingDate != null) {
                    openPosition.setClosingDate(closingDate);
                }

                openPosition.setGrade(grade);
                openPosition.setCityPosition(city);
                openPosition.setPositionType(positionType);
                openPosition.setOpenClose(false);
                openPosition.setSignDraft(false);
                openPosition.setInternalProject(false);
                openPosition.setLastOpenDate(new Date());
                openPosition.setPriority(2); // NORMAL

                // ГЛАВНОЕ: алгоритм генерации названия вакансии из OpenPositionEdit (кнопка «Генерировать»)
                String generatedVacancyName = generateCanonicalVacancyName(grade, positionType, project, city);
                openPosition.setVacansyName(truncate(generatedVacancyName, 250));

                commitContext.addInstanceToCommit(openPosition);

                // Атомарный коммит всех сущностей в одной транзакции
                dataManager.commit(commitContext);

                log.info("Successfully created project and vacancy: projectId={}, vacancyId={}, vacancyName='{}', positionType='{}' [correlationId={}]",
                        project.getId(), openPosition.getId(), openPosition.getVacansyName(),
                        positionType != null ? positionType.getPositionRuName() : "NULL", correlationId);

                response = com.company.hunttech.dto.integration.ProjectVacancyResponseDto.ok(
                        project.getId().toString(),
                        openPosition.getId().toString(),
                        request.getExternalId(),
                        com.company.hunttech.dto.integration.ProjectVacancyResponseDto.STATUS_CREATED,
                        correlationId
                );

                cacheIfIdempotent(idempotencyKey, response);
                return response;
            }

        } catch (Exception e) {
            log.error("Failed to create project and vacancy for external API [correlationId={}]", correlationId, e);
            return com.company.hunttech.dto.integration.ProjectVacancyResponseDto.error(
                    "Ошибка при сохранении проекта и вакансии. Correlation ID: " + correlationId, correlationId, "SYSTEM_ERROR");
        }
    }

    private Position findBestMatchingPositionType(String positionName) {
        if (StringUtils.isBlank(positionName)) return null;
        String safeName = truncate(positionName.trim(), 80);
        log.info("Matching Position in hunttech_Position for '{}'", safeName);

        // 1. Поиск по точному совпадению наименования (RU или EN)
        List<Position> exactList = dataManager.load(Position.class)
                .query("select e from hunttech_Position e where (lower(e.positionRuName) = lower(:name) or lower(e.positionEnName) = lower(:name)) and (e.positionRuName not like '%(не использовать)%' and e.positionRuName not like '%дубль%')")
                .parameter("name", safeName)
                .maxResults(1)
                .list();
        if (!exactList.isEmpty()) {
            log.info("Found exact position match: '{}' (ID={})", exactList.get(0).getPositionRuName(), exactList.get(0).getId());
            return exactList.get(0);
        }

        // 2. Интеллектуальный поиск среди существующих активных должностей по схожести и токенам
        List<Position> allPositions = dataManager.load(Position.class)
                .query("select e from hunttech_Position e where (e.positionRuName not like '%(не использовать)%' and e.positionRuName not like '%дубль%')")
                .list();

        Position bestMatch = null;
        int bestScore = 0;
        String lowerTarget = safeName.toLowerCase(Locale.ROOT);
        Set<String> targetTokens = extractSignificantTokens(lowerTarget);

        for (Position pos : allPositions) {
            String ru = pos.getPositionRuName() != null ? pos.getPositionRuName().toLowerCase(Locale.ROOT) : "";
            String en = pos.getPositionEnName() != null ? pos.getPositionEnName().toLowerCase(Locale.ROOT) : "";

            int score = 0;
            if (!ru.isEmpty() && lowerTarget.contains(ru)) {
                score += 100 + ru.length();
            }
            if (!en.isEmpty() && lowerTarget.contains(en)) {
                score += 100 + en.length();
            }
            if (!ru.isEmpty() && ru.contains(lowerTarget)) {
                score += 50;
            }

            Set<String> candidateTokens = extractSignificantTokens(ru + " " + en);
            for (String token : targetTokens) {
                if (candidateTokens.contains(token)) {
                    score += 25;
                }
            }

            if (score > bestScore) {
                bestScore = score;
                bestMatch = pos;
            }
        }

        if (bestMatch != null && bestScore > 0) {
            log.info("Matched best Position: '{}' (ID={}, score={}) for '{}'",
                    bestMatch.getPositionRuName(), bestMatch.getId(), bestScore, safeName);
            return bestMatch;
        }

        // Если не удалось уверенно сопоставить должность, возвращаем null, чтобы не подставлять случайные должности
        return null;
    }

    private Company resolveCustomerCompany(String companyId, String companyName, String aiCompanyName, String rawText) {
        if (StringUtils.isNotBlank(companyId)) {
            try {
                UUID compUuid = UUID.fromString(companyId.trim());
                Company company = dataManager.load(Company.class).id(compUuid).optional().orElse(null);
                if (company != null) return company;
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid companyId format: {}", companyId);
            }
        }

        String candidateName = StringUtils.isNotBlank(companyName) ? companyName : aiCompanyName;
        if (StringUtils.isNotBlank(candidateName)) {
            String trimmed = candidateName.trim();
            // Точный поиск по comanyName или companyShortName
            List<Company> list = dataManager.load(Company.class)
                    .query("select c from hunttech_Company c where lower(c.comanyName) = lower(:n) or lower(c.companyShortName) = lower(:n)")
                    .parameter("n", trimmed)
                    .maxResults(1)
                    .list();
            if (!list.isEmpty()) return list.get(0);

            // Поиск с like
            list = dataManager.load(Company.class)
                    .query("select c from hunttech_Company c where lower(c.comanyName) like lower(:n) or lower(c.companyShortName) like lower(:n)")
                    .parameter("n", "%" + trimmed + "%")
                    .maxResults(1)
                    .list();
            if (!list.isEmpty()) return list.get(0);
        }

        // Поиск по тексту вакансии
        if (StringUtils.isNotBlank(rawText)) {
            // Проверка ключевых паттернов "Заказчик: ...", "Клиент: ...", "Компания: ..."
            Pattern pat = Pattern.compile("(?i)(?:заказчик|клиент|компания|работодатель)[:\\s]+([A-Za-zА-Яа-я0-9_\\-\\s]{2,40})");
            Matcher m = pat.matcher(rawText);
            if (m.find()) {
                String extracted = m.group(1).trim();
                List<Company> list = dataManager.load(Company.class)
                        .query("select c from hunttech_Company c where lower(c.comanyName) = lower(:n) or lower(c.companyShortName) = lower(:n) or lower(c.comanyName) like lower(:likeN) or lower(c.companyShortName) like lower(:likeN)")
                        .parameter("n", extracted)
                        .parameter("likeN", "%" + extracted + "%")
                        .maxResults(1)
                        .list();
                if (!list.isEmpty()) return list.get(0);
            }

            // Проверка известных компаний в тексте: SSP / ССП
            List<Company> sspList = dataManager.load(Company.class)
                    .query("select c from hunttech_Company c where lower(c.comanyName) = 'ссп' or lower(c.comanyName) = 'ssp' or lower(c.companyShortName) = 'ссп' or lower(c.companyShortName) = 'ssp'")
                    .list();
            for (Company c : sspList) {
                if (SSP_PATTERN.matcher(rawText).find()) {
                    return c;
                }
            }
        }

        return null;
    }

    private Person resolveCustomerContact(String explicitContact, String rawText, Company customerCompany) {
        // 1. По явно переданному контакту
        if (StringUtils.isNotBlank(explicitContact)) {
            String clean = explicitContact.trim();
            // По Telegram (@username или username)
            String tgLookup = clean.startsWith("@") ? clean.substring(1).trim() : clean;
            if (!tgLookup.contains(" ") && tgLookup.matches("[A-Za-z0-9_]{3,}")) {
                List<String> tgs = java.util.Arrays.asList(tgLookup.toLowerCase(Locale.ROOT), "@" + tgLookup.toLowerCase(Locale.ROOT));
                List<Person> list = dataManager.load(Person.class)
                        .query("select p from hunttech_Person p where lower(p.telegramName) in :tgs")
                        .parameter("tgs", tgs)
                        .maxResults(1)
                        .list();
                if (!list.isEmpty()) return list.get(0);
            }

            // По ФИО
            List<Person> list = dataManager.load(Person.class)
                    .query("select p from hunttech_Person p where lower(concat(p.secondName, ' ', p.firstName)) like lower(:fio) or lower(concat(p.firstName, ' ', p.secondName)) like lower(:fio)")
                    .parameter("fio", "%" + clean + "%")
                    .maxResults(1)
                    .list();
            if (!list.isEmpty()) return list.get(0);
        }

        // 2. Поиск Telegram ников в тексте вакансии (батчем во избежание N+1)
        if (StringUtils.isNotBlank(rawText)) {
            Pattern tgPattern = Pattern.compile("@([A-Za-z0-9_]{4,})");
            Matcher tgMatcher = tgPattern.matcher(rawText);
            Set<String> foundHandles = new HashSet<>();
            while (tgMatcher.find()) {
                String rawHandle = tgMatcher.group(1);
                foundHandles.add(rawHandle);
                foundHandles.add("@" + rawHandle);
                foundHandles.add(rawHandle.toLowerCase(Locale.ROOT));
                foundHandles.add("@" + rawHandle.toLowerCase(Locale.ROOT));
            }

            if (!foundHandles.isEmpty()) {
                List<Person> tgPersons = dataManager.load(Person.class)
                        .query("select p from hunttech_Person p where p.telegramName in :tgs")
                        .parameter("tgs", foundHandles)
                        .list();
                if (!tgPersons.isEmpty()) {
                    if (customerCompany != null) {
                        for (Person p : tgPersons) {
                            if (p.getCompanyDepartment() != null && p.getCompanyDepartment().getCompanyName() != null
                                    && customerCompany.getId().equals(p.getCompanyDepartment().getCompanyName().getId())) {
                                return p;
                            }
                        }
                    }
                    return tgPersons.get(0);
                }
            }

            // 3. Поиск упоминания людей из справочника в тексте вакансии (строго среди сотрудников компании)
            if (customerCompany != null) {
                List<Person> candidatePersons = dataManager.load(Person.class)
                        .query("select p from hunttech_Person p where p.companyDepartment.companyName.id = :compId")
                        .parameter("compId", customerCompany.getId())
                        .list();

                String lowerText = rawText.toLowerCase(Locale.ROOT);
                for (Person p : candidatePersons) {
                    String first = p.getFirstName() != null ? p.getFirstName().trim().toLowerCase(Locale.ROOT) : "";
                    String second = p.getSecondName() != null ? p.getSecondName().trim().toLowerCase(Locale.ROOT) : "";
                    String tg = p.getTelegramName() != null ? p.getTelegramName().replace("@", "").trim().toLowerCase(Locale.ROOT) : "";

                    if (!tg.isEmpty() && tg.length() >= 4 && lowerText.contains(tg)) {
                        return p;
                    }
                    if (second.length() >= 4 && Pattern.compile("(?i)\\b" + Pattern.quote(second) + "\\b").matcher(rawText).find()) {
                        if (first.length() >= 3) {
                            if (lowerText.contains(first)) {
                                return p;
                            }
                        } else {
                            return p;
                        }
                    }
                }
            }
        }

        return null;
    }

    private Project findExistingProject(String baseProjectName, Person owner, Company company) {
        if (StringUtils.isBlank(baseProjectName)) return null;
        String clean = cleanProjectTitle(baseProjectName);

        List<Project> list;
        // 1. По точному совпадению названия проекта среди открытых проектов компании
        if (company != null) {
            list = dataManager.load(Project.class)
                    .query("select p from hunttech_Project p where p.projectDepartment.companyName.id = :compId and lower(p.projectName) = lower(:name) and (p.projectIsClosed is null or p.projectIsClosed = false)")
                    .parameter("compId", company.getId())
                    .parameter("name", clean.toLowerCase(Locale.ROOT))
                    .maxResults(1)
                    .list();
            if (!list.isEmpty()) return list.get(0);
        } else {
            list = dataManager.load(Project.class)
                    .query("select p from hunttech_Project p where lower(p.projectName) = lower(:name) and (p.projectIsClosed is null or p.projectIsClosed = false)")
                    .parameter("name", clean.toLowerCase(Locale.ROOT))
                    .maxResults(1)
                    .list();
            if (!list.isEmpty()) return list.get(0);
        }

        // 2. Поиск по владельцу проекта и ключевой подстроке
        if (owner != null && StringUtils.isNotBlank(clean) && clean.length() >= 3) {
            list = dataManager.load(Project.class)
                    .query("select p from hunttech_Project p where p.projectOwner.id = :ownerId and (p.projectIsClosed is null or p.projectIsClosed = false) and lower(p.projectName) like lower(:p)")
                    .parameter("ownerId", owner.getId())
                    .parameter("p", "%" + clean + "%")
                    .maxResults(1)
                    .list();
            if (!list.isEmpty()) return list.get(0);
        }

        // 3. Поиск по компании и подстроке в открытых проектах
        if (company != null && StringUtils.isNotBlank(clean) && clean.length() >= 3) {
            list = dataManager.load(Project.class)
                    .query("select p from hunttech_Project p where p.projectDepartment.companyName.id = :compId and (p.projectIsClosed is null or p.projectIsClosed = false) and lower(p.projectName) like lower(:p)")
                    .parameter("compId", company.getId())
                    .parameter("p", "%" + clean + "%")
                    .maxResults(1)
                    .list();
            if (!list.isEmpty()) return list.get(0);
        }

        return null;
    }

    private String buildCanonicalProjectName(Company company, String rawProjectName, String formattedContact, String actPeriod) {
        String customerCode = company != null
                ? (StringUtils.isNotBlank(company.getCompanyShortName())
                    ? company.getCompanyShortName().trim()
                    : (company.getComanyName() != null ? company.getComanyName().trim() : "Клиент"))
                : "Клиент";
        String cleanTitle = cleanProjectTitle(rawProjectName);
        if (cleanTitle.isEmpty()) {
            cleanTitle = "Основной";
        }
        String contactPart = StringUtils.isNotBlank(formattedContact)
                ? "Проект " + formattedContact
                : "Проект заказчика";

        String period = StringUtils.isNotBlank(actPeriod) ? actPeriod.trim() : "2 месяца";

        // Шаблон: <ЗАКАЗЧИК> "Наименование проекта. Проект <контакт со стороны заказчика>" /Штат Hunttech ТК/ГПХ или ИП. Актирование <количество месяцев>"
        return String.format("%s \"%s. %s\" /Штат HuntTech ТК/ГПХ или ИП. Актирование %s/",
                customerCode, cleanTitle, contactPart, period);
    }

    private String cleanProjectTitle(String title) {
        if (StringUtils.isBlank(title)) return "";
        String s = title.replaceAll("[\"«»]", " ").trim();
        s = s.replaceAll("(?i)^ssp\\s+", "");
        s = s.replaceAll("(?i)/штат.*", "");
        s = s.replaceAll("(?i)\\.?[\\s]*проект.*", "");
        return s.replaceAll("\\s+", " ").trim();
    }

    private String resolveActPeriod(String explicitActPeriod, String rawText) {
        if (StringUtils.isNotBlank(explicitActPeriod)) {
            return explicitActPeriod.trim();
        }
        if (StringUtils.isNotBlank(rawText)) {
            Matcher m = Pattern.compile("(?i)актирован(?:ие|ия|ии)[^0-9a-zA-Zа-яА-Я]{0,10}(\\d+\\s*(?:месяц(?:а|ев)?|мес|дн(?:ей|я)?))").matcher(rawText);
            if (m.find()) {
                return m.group(1).trim();
            }
            if (Pattern.compile("(?i)месячное\\s+актирование").matcher(rawText).find()) {
                return "1 месяц";
            }
        }
        return "2 месяца";
    }

    private CompanyDepartament resolveOrCreateDepartment(Company company, CommitContext commitContext) {
        if (company == null) return null;
        List<CompanyDepartament> depts = dataManager.load(CompanyDepartament.class)
                .query("select d from hunttech_CompanyDepartament d where d.companyName.id = :compId")
                .parameter("compId", company.getId())
                .list();
        if (!depts.isEmpty()) {
            return depts.get(0);
        }
        CompanyDepartament dept = metadata.create(CompanyDepartament.class);
        dept.setDepartamentRuName("Основной");
        dept.setCompanyName(company);
        commitContext.addInstanceToCommit(dept);
        return dept;
    }

    private String generateProjectDescription(String projectName, String rawText) {
        if (StringUtils.isBlank(rawText)) return "";
        try {
            log.info("Generating project description via AI [projectName='{}']", projectName);
            Map<String, Object> ctx = new HashMap<>();
            ctx.put("projectName", projectName);
            ctx.put("sourceFileName", "");
            ctx.put("sourceText", rawText);
            AiExecutionResult result = aiExecutionService.executeText("PROJECT_DESCRIPTION_GENERATE", ctx);
            if (result != null && StringUtils.isNotBlank(result.getText())) {
                return result.getText().trim();
            }
        } catch (Exception ex) {
            log.warn("Failed to generate project description via AI for '{}': {}", projectName, ex.getMessage());
        }
        return "Проект: " + projectName + "\n\n" + rawText;
    }

    private String generateProjectShortDescription(String projectName, String fullDescription, String rawText) {
        try {
            Map<String, Object> ctx = new HashMap<>();
            ctx.put("projectName", projectName);
            ctx.put("sourceText", StringUtils.isNotBlank(fullDescription) ? fullDescription : rawText);
            AiExecutionResult result = aiExecutionService.executeText("PROJECT_SHORT_DESCRIPTION_GENERATE", ctx);
            if (result != null && StringUtils.isNotBlank(result.getText())) {
                return result.getText().trim();
            }
        } catch (Exception ex) {
            log.warn("Failed to generate project short description via AI for '{}': {}", projectName, ex.getMessage());
        }
        String fallback = StringUtils.isNotBlank(fullDescription) ? fullDescription : rawText;
        return truncate(fallback.replaceAll("\\s+", " ").trim(), 250);
    }

    private String formatContactForProjectName(Person person) {
        if (person == null) return "";
        String first = StringUtils.trimToEmpty(person.getFirstName());
        String second = StringUtils.trimToEmpty(person.getSecondName());
        if (first.isEmpty() && second.isEmpty()) return "";
        if (first.isEmpty()) return declineRuSurname(second);
        if (second.isEmpty()) return declineRuFirstName(first);
        return (declineRuFirstName(first) + " " + declineRuSurname(second)).trim();
    }

    private String declineRuFirstName(String name) {
        if (StringUtils.isBlank(name)) return "";
        String trimmed = name.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.endsWith("ка") || lower.endsWith("га") || lower.endsWith("ха") || lower.endsWith("ша") || lower.endsWith("жа")) {
            return trimmed.substring(0, trimmed.length() - 1) + "и";
        }
        if (lower.endsWith("а")) {
            return trimmed.substring(0, trimmed.length() - 1) + "ы";
        }
        if (lower.endsWith("я")) {
            return trimmed.substring(0, trimmed.length() - 1) + "и";
        }
        if (lower.endsWith("ел")) {
            return trimmed.substring(0, trimmed.length() - 2) + "ла";
        }
        if (lower.endsWith("й")) {
            return trimmed.substring(0, trimmed.length() - 1) + "я";
        }
        if (lower.matches(".*[бвгджзклмнпрстфхцчшщ]$")) {
            return trimmed + "а";
        }
        return trimmed;
    }

    private String declineRuSurname(String surname) {
        if (StringUtils.isBlank(surname)) return "";
        String trimmed = surname.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.endsWith("ая")) {
            return trimmed.substring(0, trimmed.length() - 2) + "ой";
        }
        if (lower.endsWith("ва") || lower.endsWith("на")) {
            return trimmed.substring(0, trimmed.length() - 1) + "ой";
        }
        if (lower.endsWith("ов") || lower.endsWith("ев") || lower.endsWith("ин") || lower.endsWith("ын")) {
            return trimmed + "а";
        }
        if (lower.endsWith("ий") || lower.endsWith("ый")) {
            return trimmed.substring(0, trimmed.length() - 2) + "ого";
        }
        if (lower.endsWith("да")) {
            return trimmed.substring(0, trimmed.length() - 1) + "ы";
        }
        return trimmed;
    }

    private Grade findGrade(String gradeName) {
        if (StringUtils.isBlank(gradeName)) return null;
        String clean = gradeName.trim();
        List<Grade> exact = dataManager.load(Grade.class)
                .query("select e from hunttech_Grade e where lower(e.gradeName) = lower(:g) and lower(e.gradeName) not like 'testgrade%' and lower(e.gradeName) not like 'grade-%'")
                .parameter("g", clean)
                .maxResults(1)
                .list();
        if (!exact.isEmpty()) return exact.get(0);

        List<Grade> list = dataManager.load(Grade.class)
                .query("select e from hunttech_Grade e where lower(e.gradeName) like lower(:g) and lower(e.gradeName) not like 'testgrade%' and lower(e.gradeName) not like 'grade-%'")
                .parameter("g", "%" + clean + "%")
                .maxResults(1)
                .list();
        return list.isEmpty() ? null : list.get(0);
    }

    private City findCity(String cityName) {
        if (StringUtils.isBlank(cityName)) return null;
        String clean = cityName.trim();
        List<City> list = dataManager.load(City.class)
                .query("select c from hunttech_City c where lower(c.cityRuName) = lower(:n) or lower(c.cityEngName) = lower(:n)")
                .parameter("n", clean)
                .maxResults(1)
                .list();
        if (!list.isEmpty()) return list.get(0);
        list = dataManager.load(City.class)
                .query("select c from hunttech_City c where lower(c.cityRuName) like lower(:n)")
                .parameter("n", "%" + clean + "%")
                .maxResults(1)
                .list();
        return list.isEmpty() ? null : list.get(0);
    }

    private Set<String> extractSignificantTokens(String text) {
        if (text == null) return Collections.emptySet();
        Set<String> tokens = new HashSet<>();
        for (String part : text.toLowerCase(Locale.ROOT).split("[^a-zA-Zа-яА-Я0-9]+")) {
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

    @Override
    public CandidateCvResponseDto createCandidateCV(CandidateCvCreateRequestDto request) {
        if (request == null) {
            return CandidateCvResponseDto.error("Тело запроса отсутствует", null, "VALIDATION_ERROR");
        }

        String correlationId = request.getCorrelationId();
        String idempotencyKey = StringUtils.trimToNull(request.getIdempotencyKey());

        if (idempotencyKey != null) {
            CandidateCvResponseDto cached = candidateCvIdempotencyCache.get(idempotencyKey);
            if (cached != null) {
                log.info("Returning cached candidateCV response for idempotencyKey: {} [correlationId={}]", idempotencyKey, correlationId);
                return cached;
            }
        }

        if (StringUtils.isBlank(request.getCandidateId())) {
            return CandidateCvResponseDto.error("Идентификатор кандидата (candidateId) обязателен", correlationId, "VALIDATION_ERROR");
        }

        try {
            CandidateCvResponseDto response;
            synchronized (candidateCvLock) {
                if (idempotencyKey != null) {
                    CandidateCvResponseDto cached = candidateCvIdempotencyCache.get(idempotencyKey);
                    if (cached != null) {
                        return cached;
                    }
                }

                UUID candidateUuid;
                try {
                    candidateUuid = UUID.fromString(request.getCandidateId().trim());
                } catch (IllegalArgumentException ex) {
                    return CandidateCvResponseDto.error("Некорректный UUID кандидата: " + request.getCandidateId(), correlationId, "VALIDATION_ERROR");
                }

                JobCandidate candidate = dataManager.load(JobCandidate.class).id(candidateUuid).view("jobCandidate-dedup-view").optional().orElse(null);
                if (candidate == null) {
                    return CandidateCvResponseDto.error("Кандидат с указанным candidateId не найден: " + request.getCandidateId(), correlationId, "NOT_FOUND");
                }

                OpenPosition toVacancy = null;
                if (StringUtils.isNotBlank(request.getVacancyId())) {
                    try {
                        UUID vacUuid = UUID.fromString(request.getVacancyId().trim());
                        toVacancy = dataManager.load(OpenPosition.class).id(vacUuid).view("openPosition-view").optional().orElse(null);
                    } catch (IllegalArgumentException ex) {
                        log.warn("Invalid vacancyId format [correlationId={}]: {}", correlationId, request.getVacancyId(), ex);
                    }
                }

                Position resumePosition = null;
                if (StringUtils.isNotBlank(request.getPositionId())) {
                    try {
                        UUID posUuid = UUID.fromString(request.getPositionId().trim());
                        resumePosition = dataManager.load(Position.class).id(posUuid).optional().orElse(null);
                    } catch (IllegalArgumentException ex) {
                        log.warn("Invalid positionId format [correlationId={}]: {}", correlationId, request.getPositionId(), ex);
                    }
                }

                CandidateCV cv = metadata.create(CandidateCV.class);
                cv.setCandidate(candidate);
                cv.setDatePost(new Date());
                cv.setTextCV(request.getTextCv());
                cv.setLinkOriginalCv(truncate(request.getResumeUrl(), 255));
                cv.setLetter(request.getCoverLetter());
                cv.setToVacancy(toVacancy);
                cv.setResumePosition(resumePosition);

                dataManager.commit(cv);

                log.info("Successfully created CandidateCV id={} for candidateId={} [correlationId={}]", cv.getId(), candidate.getId(), correlationId);
                response = CandidateCvResponseDto.ok(
                        cv.getId().toString(),
                        candidate.getId().toString(),
                        request.getExternalId(),
                        CandidateCvResponseDto.STATUS_CREATED,
                        correlationId
                );

                cacheIfIdempotent(idempotencyKey, response);
                return response;
            }
        } catch (Exception e) {
            log.error("Failed to create CandidateCV [correlationId={}]", correlationId, e);
            return CandidateCvResponseDto.error("Ошибка при сохранении резюме. Correlation ID: " + correlationId, correlationId, "SYSTEM_ERROR");
        }
    }

    @Override
    public InteractionResponseDto createInteraction(InteractionCreateRequestDto request) {
        if (request == null) {
            return InteractionResponseDto.error("Тело запроса отсутствует", null, "VALIDATION_ERROR");
        }

        String correlationId = request.getCorrelationId();
        String idempotencyKey = StringUtils.trimToNull(request.getIdempotencyKey());

        if (idempotencyKey != null) {
            InteractionResponseDto cached = interactionIdempotencyCache.get(idempotencyKey);
            if (cached != null) {
                log.info("Returning cached interaction response for idempotencyKey: {} [correlationId={}]", idempotencyKey, correlationId);
                return cached;
            }
        }

        if (StringUtils.isBlank(request.getCandidateId())) {
            return InteractionResponseDto.error("Идентификатор кандидата (candidateId) обязателен", correlationId, "VALIDATION_ERROR");
        }
        if (StringUtils.isBlank(request.getVacancyId())) {
            return InteractionResponseDto.error("Идентификатор вакансии (vacancyId) обязателен", correlationId, "VALIDATION_ERROR");
        }

        try {
            InteractionResponseDto response;
            synchronized (interactionLock) {
                if (idempotencyKey != null) {
                    InteractionResponseDto cached = interactionIdempotencyCache.get(idempotencyKey);
                    if (cached != null) {
                        return cached;
                    }
                }

                UUID candidateUuid;
                UUID vacancyUuid;
                try {
                    candidateUuid = UUID.fromString(request.getCandidateId().trim());
                    vacancyUuid = UUID.fromString(request.getVacancyId().trim());
                } catch (IllegalArgumentException ex) {
                    return InteractionResponseDto.error("Некорректный формат UUID для candidateId или vacancyId", correlationId, "VALIDATION_ERROR");
                }

                JobCandidate candidate = dataManager.load(JobCandidate.class).id(candidateUuid).view("jobCandidate-dedup-view").optional().orElse(null);
                if (candidate == null) {
                    return InteractionResponseDto.error("Кандидат с указанным candidateId не найден: " + request.getCandidateId(), correlationId, "NOT_FOUND");
                }

                OpenPosition vacancy = dataManager.load(OpenPosition.class).id(vacancyUuid).view("openPosition-view").optional().orElse(null);
                if (vacancy == null) {
                    return InteractionResponseDto.error("Вакансия с указанным vacancyId не найдена: " + request.getVacancyId(), correlationId, "NOT_FOUND");
                }

                Iteraction iteractionType = null;
                if (StringUtils.isNotBlank(request.getInteractionTypeId())) {
                    try {
                        UUID typeUuid = UUID.fromString(request.getInteractionTypeId().trim());
                        iteractionType = dataManager.load(Iteraction.class).id(typeUuid).optional().orElse(null);
                    } catch (IllegalArgumentException ex) {
                        log.warn("Invalid interactionTypeId format [correlationId={}]: {}", correlationId, request.getInteractionTypeId(), ex);
                    }
                }
                if (iteractionType == null) {
                    iteractionType = dataManager.load(Iteraction.class)
                            .query("select e from hunttech_Iteraction e order by e.createTs asc")
                            .maxResults(1)
                            .optional()
                            .orElse(null);
                }

                ExtUser recruiter = null;
                if (StringUtils.isNotBlank(request.getRecruiterId())) {
                    try {
                        UUID recUuid = UUID.fromString(request.getRecruiterId().trim());
                        recruiter = dataManager.load(ExtUser.class).id(recUuid).optional().orElse(null);
                    } catch (IllegalArgumentException ex) {
                        log.warn("Invalid recruiterId format [correlationId={}]: {}", correlationId, request.getRecruiterId(), ex);
                    }
                }
                if (recruiter == null) {
                    recruiter = vacancy.getOwner();
                }
                if (recruiter == null && userSessionSource != null && userSessionSource.checkCurrentUserSession()) {
                    recruiter = (ExtUser) userSessionSource.getUserSession().getUser();
                }

                BigDecimal nextNum = calculateNextInteractionNumber();

                IteractionList interaction = metadata.create(IteractionList.class);
                interaction.setCandidate(candidate);
                interaction.setVacancy(vacancy);
                interaction.setIteractionType(iteractionType);
                interaction.setRecrutier(recruiter);
                String comment = StringUtils.isNotBlank(request.getComment())
                        ? request.getComment().trim()
                        : "Взаимодействие создано из внешней системы";
                interaction.setComment(comment);
                interaction.setCommunicationMethod(truncate(request.getCommunicationMethod(), 80));
                interaction.setRating(request.getRating());
                interaction.setNumberIteraction(nextNum);


                dataManager.commit(interaction);

                log.info("Successfully created IteractionList id={} (num={}) for candidateId={}, vacancyId={} [correlationId={}]",
                        interaction.getId(), nextNum, candidate.getId(), vacancy.getId(), correlationId);

                response = InteractionResponseDto.ok(
                        interaction.getId().toString(),
                        candidate.getId().toString(),
                        vacancy.getId().toString(),
                        nextNum,
                        request.getExternalId(),
                        InteractionResponseDto.STATUS_CREATED,
                        correlationId
                );

                cacheIfIdempotent(idempotencyKey, response);
                return response;
            }
        } catch (Exception e) {
            log.error("Failed to create IteractionList [correlationId={}]", correlationId, e);
            return InteractionResponseDto.error("Ошибка при сохранении взаимодействия. Correlation ID: " + correlationId, correlationId, "SYSTEM_ERROR");
        }
    }

    @Override
    public CandidateCompositeResponseDto createCandidateWithDetails(CandidateCompositeCreateRequestDto request) {
        if (request == null) {
            return CandidateCompositeResponseDto.error("Тело запроса отсутствует", null, "VALIDATION_ERROR");
        }

        String correlationId = request.getCorrelationId();
        String idempotencyKey = StringUtils.trimToNull(request.getIdempotencyKey());

        if (idempotencyKey != null) {
            CandidateCompositeResponseDto cached = candidateCompositeIdempotencyCache.get(idempotencyKey);
            if (cached != null) {
                log.info("Returning cached candidateComposite response for idempotencyKey: {} [correlationId={}]", idempotencyKey, correlationId);
                return cached;
            }
        }

        if (StringUtils.isBlank(request.getFirstName()) || StringUtils.isBlank(request.getSecondName())) {
            return CandidateCompositeResponseDto.error("Имя (firstName) и Фамилия (secondName) кандидата обязательны для заполнения", correlationId, "VALIDATION_ERROR");
        }

        try {
            CandidateCompositeResponseDto response;
            synchronized (candidateCompositeLock) {
                if (idempotencyKey != null) {
                    CandidateCompositeResponseDto cached = candidateCompositeIdempotencyCache.get(idempotencyKey);
                    if (cached != null) {
                        return cached;
                    }
                }

                CommitContext commitContext = new CommitContext();
                JobCandidate candidate = null;
                String status = CandidateCompositeResponseDto.STATUS_CREATED;

                // 1. Дедупликация по телефону (точный номер)
                String rawPhone = StringUtils.trimToNull(request.getPhone());
                if (rawPhone == null) {
                    rawPhone = StringUtils.trimToNull(request.getMobilePhone());
                }
                if (rawPhone != null) {
                    candidate = dataManager.load(JobCandidate.class)
                            .query("select c from hunttech_JobCandidate c where c.phone = :p or c.mobilePhone = :p")
                            .parameter("p", rawPhone)
                            .view("jobCandidate-dedup-view")
                            .optional()
                            .orElse(null);
                }

                // 2. Дедупликация по email
                if (candidate == null && StringUtils.isNotBlank(request.getEmail())) {
                    String cleanEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
                    candidate = dataManager.load(JobCandidate.class)
                            .query("select c from hunttech_JobCandidate c where lower(c.email) = :email")
                            .parameter("email", cleanEmail)
                            .view("jobCandidate-dedup-view")
                            .optional()
                            .orElse(null);
                }

                // 3. Дедупликация по ФИО
                if (candidate == null) {
                    String secName = request.getSecondName().trim().toLowerCase(Locale.ROOT);
                    String firName = request.getFirstName().trim().toLowerCase(Locale.ROOT);
                    candidate = dataManager.load(JobCandidate.class)
                            .query("select c from hunttech_JobCandidate c where lower(c.secondName) = :sec and lower(c.firstName) = :fir")
                            .parameter("sec", secName)
                            .parameter("fir", firName)
                            .view("jobCandidate-dedup-view")
                            .optional()
                            .orElse(null);
                }

                if (candidate != null) {
                    status = CandidateCompositeResponseDto.STATUS_EXISTING_FOUND;
                    log.info("Candidate deduplicated: found id={} for {} {} [correlationId={}]", candidate.getId(), candidate.getSecondName(), candidate.getFirstName(), correlationId);

                    // Обогащение существующей карточки кандидата переданными новыми реквизитами
                    boolean modified = false;
                    if (candidate.getEmail() == null && StringUtils.isNotBlank(request.getEmail())) {
                        candidate.setEmail(truncate(request.getEmail(), 50));
                        modified = true;
                    }
                    if (candidate.getPhone() == null && StringUtils.isNotBlank(request.getPhone())) {
                        candidate.setPhone(truncate(request.getPhone(), 18));
                        modified = true;
                    }
                    if (candidate.getMobilePhone() == null && StringUtils.isNotBlank(request.getMobilePhone())) {
                        candidate.setMobilePhone(truncate(request.getMobilePhone(), 18));
                        modified = true;
                    }
                    if (candidate.getTelegramName() == null && StringUtils.isNotBlank(request.getTelegramName())) {
                        candidate.setTelegramName(truncate(request.getTelegramName(), 30));
                        modified = true;
                    }
                    if (candidate.getSkypeName() == null && StringUtils.isNotBlank(request.getSkypeName())) {
                        candidate.setSkypeName(truncate(request.getSkypeName(), 30));
                        modified = true;
                    }
                    if (candidate.getCityOfResidence() == null && StringUtils.isNotBlank(request.getCityId())) {
                        try {
                            UUID cityUuid = UUID.fromString(request.getCityId().trim());
                            City city = dataManager.load(City.class).id(cityUuid).optional().orElse(null);
                            if (city != null) {
                                candidate.setCityOfResidence(city);
                                modified = true;
                            }
                        } catch (IllegalArgumentException ignored) {}
                    }
                    if (candidate.getPersonPosition() == null && StringUtils.isNotBlank(request.getPositionId())) {
                        try {
                            UUID posUuid = UUID.fromString(request.getPositionId().trim());
                            Position pos = dataManager.load(Position.class).id(posUuid).optional().orElse(null);
                            if (pos != null) {
                                candidate.setPersonPosition(pos);
                                modified = true;
                            }
                        } catch (IllegalArgumentException ignored) {}
                    }
                    if (candidate.getCurrentCompany() == null && StringUtils.isNotBlank(request.getCompanyId())) {
                        try {
                            UUID compUuid = UUID.fromString(request.getCompanyId().trim());
                            Company comp = dataManager.load(Company.class).id(compUuid).optional().orElse(null);
                            if (comp != null) {
                                candidate.setCurrentCompany(comp);
                                modified = true;
                            }
                        } catch (IllegalArgumentException ignored) {}
                    }
                    if (modified) {
                        commitContext.addInstanceToCommit(candidate);
                    }
                } else {

                    candidate = metadata.create(JobCandidate.class);
                    candidate.setFirstName(truncate(request.getFirstName(), 80));
                    candidate.setSecondName(truncate(request.getSecondName(), 80));
                    candidate.setMiddleName(truncate(request.getMiddleName(), 80));

                    StringBuilder fn = new StringBuilder();
                    fn.append(candidate.getSecondName()).append(" ").append(candidate.getFirstName());
                    if (StringUtils.isNotBlank(candidate.getMiddleName())) {
                        fn.append(" ").append(candidate.getMiddleName());
                    }
                    candidate.setFullName(truncate(fn.toString(), 160));

                    candidate.setPhone(truncate(request.getPhone(), 18));
                    candidate.setMobilePhone(truncate(request.getMobilePhone(), 18));
                    candidate.setEmail(truncate(request.getEmail(), 50));
                    candidate.setTelegramName(truncate(request.getTelegramName(), 30));
                    candidate.setSkypeName(truncate(request.getSkypeName(), 30));
                    candidate.setBirdhDate(request.getBirthDate());
                    candidate.setBlockCandidate(false);

                    if (StringUtils.isNotBlank(request.getCityId())) {
                        try {
                            UUID cityUuid = UUID.fromString(request.getCityId().trim());
                            City city = dataManager.load(City.class).id(cityUuid).optional().orElse(null);
                            candidate.setCityOfResidence(city);
                        } catch (IllegalArgumentException ex) {
                            log.warn("Invalid cityId format [correlationId={}]: {}", correlationId, request.getCityId(), ex);
                        }
                    }

                    if (StringUtils.isNotBlank(request.getPositionId())) {
                        try {
                            UUID posUuid = UUID.fromString(request.getPositionId().trim());
                            Position pos = dataManager.load(Position.class).id(posUuid).optional().orElse(null);
                            candidate.setPersonPosition(pos);
                        } catch (IllegalArgumentException ex) {
                            log.warn("Invalid positionId format [correlationId={}]: {}", correlationId, request.getPositionId(), ex);
                        }
                    }

                    if (StringUtils.isNotBlank(request.getCompanyId())) {
                        try {
                            UUID compUuid = UUID.fromString(request.getCompanyId().trim());
                            Company comp = dataManager.load(Company.class).id(compUuid).optional().orElse(null);
                            candidate.setCurrentCompany(comp);
                        } catch (IllegalArgumentException ex) {
                            log.warn("Invalid companyId format [correlationId={}]: {}", correlationId, request.getCompanyId(), ex);
                        }
                    }

                    commitContext.addInstanceToCommit(candidate);
                }

                // 4. Опциональное создание резюме
                CandidateCV cv = null;
                boolean hasCv = StringUtils.isNotBlank(request.getCvText()) || StringUtils.isNotBlank(request.getCvUrl());
                if (hasCv) {
                    cv = metadata.create(CandidateCV.class);
                    cv.setCandidate(candidate);
                    cv.setDatePost(new Date());
                    cv.setTextCV(request.getCvText());
                    cv.setLinkOriginalCv(truncate(request.getCvUrl(), 255));
                    cv.setLetter(request.getCoverLetter());
                    if (candidate.getPersonPosition() != null) {
                        cv.setResumePosition(candidate.getPersonPosition());
                    }
                    commitContext.addInstanceToCommit(cv);
                }

                // 5. Опциональное создание взаимодействия при наличии vacancyId
                IteractionList interaction = null;
                if (StringUtils.isNotBlank(request.getVacancyId())) {
                    try {
                        UUID vacUuid = UUID.fromString(request.getVacancyId().trim());
                        OpenPosition vacancy = dataManager.load(OpenPosition.class).id(vacUuid).view("openPosition-view").optional().orElse(null);
                        if (vacancy != null) {
                            if (cv != null) {
                                cv.setToVacancy(vacancy);
                            }

                            Iteraction iteractionType = null;
                            if (StringUtils.isNotBlank(request.getInteractionTypeId())) {
                                try {
                                    UUID typeUuid = UUID.fromString(request.getInteractionTypeId().trim());
                                    iteractionType = dataManager.load(Iteraction.class).id(typeUuid).optional().orElse(null);
                                } catch (IllegalArgumentException ex) {
                                    log.warn("Invalid interactionTypeId format [correlationId={}]: {}", correlationId, request.getInteractionTypeId(), ex);
                                }
                            }
                            if (iteractionType == null) {
                                iteractionType = dataManager.load(Iteraction.class)
                                        .query("select e from hunttech_Iteraction e order by e.createTs asc")
                                        .maxResults(1)
                                        .optional()
                                        .orElse(null);
                            }

                            ExtUser recruiter = vacancy.getOwner();
                            if (recruiter == null && userSessionSource != null && userSessionSource.checkCurrentUserSession()) {
                                recruiter = (ExtUser) userSessionSource.getUserSession().getUser();
                            }

                            BigDecimal nextNum = calculateNextInteractionNumber();

                            interaction = metadata.create(IteractionList.class);
                            interaction.setCandidate(candidate);
                            interaction.setVacancy(vacancy);
                            interaction.setIteractionType(iteractionType);
                            interaction.setRecrutier(recruiter);
                            interaction.setDateIteraction(new Date());
                            String comment = StringUtils.isNotBlank(request.getInteractionComment())
                                    ? request.getInteractionComment()
                                    : "Добавление кандидата из внешней системы";
                            interaction.setComment(comment);
                            interaction.setCommunicationMethod(truncate(request.getCommunicationMethod(), 80));
                            interaction.setRating(request.getRating());
                            interaction.setNumberIteraction(nextNum);

                            commitContext.addInstanceToCommit(interaction);
                        }
                    } catch (IllegalArgumentException ex) {
                        log.warn("Invalid vacancyId format [correlationId={}]: {}", correlationId, request.getVacancyId(), ex);
                    }
                }

                // Коммит всех связанных сущностей атомарно
                dataManager.commit(commitContext);

                String candidateIdStr = candidate.getId().toString();
                String cvIdStr = cv != null ? cv.getId().toString() : null;
                String interactionIdStr = interaction != null ? interaction.getId().toString() : null;

                log.info("Successfully processed composite candidate: candidateId={}, cvId={}, interactionId={}, status={} [correlationId={}]",
                        candidateIdStr, cvIdStr, interactionIdStr, status, correlationId);

                response = CandidateCompositeResponseDto.ok(
                        candidateIdStr,
                        cvIdStr,
                        interactionIdStr,
                        request.getExternalId(),
                        status,
                        correlationId
                );

                cacheIfIdempotent(idempotencyKey, response);
                return response;
            }
        } catch (Exception e) {
            log.error("Failed to create candidate composite [correlationId={}]", correlationId, e);
            return CandidateCompositeResponseDto.error("Ошибка при сохранении кандидата. Correlation ID: " + correlationId, correlationId, "SYSTEM_ERROR");
        }
    }

    private BigDecimal calculateNextInteractionNumber() {
        synchronized (interactionNumberLock) {
            try {
                BigDecimal maxNum = dataManager
                        .loadValue("select max(e.numberIteraction) from hunttech_IteractionList e", BigDecimal.class)
                        .optional()
                        .orElse(BigDecimal.ZERO);
                return maxNum.add(BigDecimal.ONE);
            } catch (Exception e) {
                log.warn("Error calculating next interaction number: {}", e.getMessage());
                return BigDecimal.ONE;
            }
        }
    }


    private String cleanPhone(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("\\D+", "");
        return digits.isEmpty() ? null : digits;
    }

    private void cacheIfIdempotent(String idempotencyKey, CompanyResponseDto response) {
        if (idempotencyKey != null && response != null && response.isSuccess()) {
            cacheResponse(idempotencyCache, idempotencyKey, response);
        }
    }

    private void cacheIfIdempotent(String idempotencyKey, ProjectVacancyResponseDto response) {
        if (idempotencyKey != null && response != null && response.isSuccess()) {
            cacheResponse(projectVacancyIdempotencyCache, idempotencyKey, response);
        }
    }

    private void cacheIfIdempotent(String idempotencyKey, CandidateCvResponseDto response) {
        if (idempotencyKey != null && response != null && response.isSuccess()) {
            cacheResponse(candidateCvIdempotencyCache, idempotencyKey, response);
        }
    }

    private void cacheIfIdempotent(String idempotencyKey, InteractionResponseDto response) {
        if (idempotencyKey != null && response != null && response.isSuccess()) {
            cacheResponse(interactionIdempotencyCache, idempotencyKey, response);
        }
    }

    private void cacheIfIdempotent(String idempotencyKey, CandidateCompositeResponseDto response) {
        if (idempotencyKey != null && response != null && response.isSuccess()) {
            cacheResponse(candidateCompositeIdempotencyCache, idempotencyKey, response);
        }
    }

    private <T> void cacheResponse(Map<String, T> cache, String idempotencyKey, T response) {
        if (cache.size() > 5000) {
            cache.clear();
        }
        cache.put(idempotencyKey, response);
    }

    private ExtUser resolveHunttechUser() {
        if (cachedHunttechUser != null) {
            return cachedHunttechUser;
        }
        ExtUser user = dataManager.load(ExtUser.class)
                .query("select u from sec$User u where u.login = :login")
                .parameter("login", "hunttech")
                .optional()
                .orElse(null);
        if (user == null) {
            log.warn("System user with login 'hunttech' not found in sec$User! Vacancy owner will not be set.");
        } else {
            cachedHunttechUser = user;
        }
        return user;
    }

    private City resolveRemoteCity() {
        List<City> list = dataManager.load(City.class)
                .query("select c from hunttech_City c where c.cityRuName = :name")
                .parameter("name", "Регионы РФ (МСК +/- 2 часа)")
                .maxResults(1)
                .list();
        if (!list.isEmpty()) return list.get(0);

        list = dataManager.load(City.class)
                .query("select c from hunttech_City c where lower(c.cityRuName) like lower(:p)")
                .parameter("p", "%регионы рф%2 часа%")
                .maxResults(1)
                .list();
        if (!list.isEmpty()) return list.get(0);

        list = dataManager.load(City.class)
                .query("select c from hunttech_City c where lower(c.cityRuName) like lower(:p)")
                .parameter("p", "%регионы рф%")
                .maxResults(1)
                .list();
        return list.isEmpty() ? null : list.get(0);
    }

    private boolean isRemoteWork(Integer remoteWork, String rawText) {
        if (remoteWork != null && remoteWork == 1) return true;
        if (StringUtils.isBlank(rawText)) return false;
        String lower = rawText.toLowerCase(Locale.ROOT);
        return lower.contains("удален")
                || lower.contains("remote")
                || lower.contains("локация рф")
                || lower.contains("формат работы удаленно");
    }

    private City resolveCity(Integer remoteWork, String cityId, String cityName, String parsedCityName, String rawText) {
        if (StringUtils.isNotBlank(cityId)) {
            try {
                UUID cityUuid = UUID.fromString(cityId.trim());
                City city = dataManager.load(City.class).id(cityUuid).optional().orElse(null);
                if (city != null) return city;
            } catch (IllegalArgumentException ignored) {}
        }

        boolean remote = isRemoteWork(remoteWork, rawText);
        if (remote) {
            City remoteCity = resolveRemoteCity();
            if (remoteCity != null) return remoteCity;
        }

        String candidate = StringUtils.isNotBlank(cityName) ? cityName : parsedCityName;
        if (StringUtils.isNotBlank(candidate)) {
            City found = findCity(candidate);
            if (found != null) return found;
        }

        return null;
    }

    private String extractGradeNameFromText(String text) {
        if (StringUtils.isBlank(text)) return null;
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("senior+") || lower.contains("сеньор+") || lower.contains("senior plus")) return "Senior+";
        if (lower.contains("senior") || lower.contains("сеньор") || lower.contains("сениор")) return "Senior";
        if (lower.contains("lead") || lower.contains("лид") || lower.contains("тимлид") || lower.contains("руководитель")) return "Lead";
        if (lower.contains("middle+") || lower.contains("мидл+") || lower.contains("миддл+")) return "Middle+";
        if (lower.contains("middle") || lower.contains("мидл") || lower.contains("миддл")) return "Middle";
        if (lower.contains("junior") || lower.contains("джуниор") || lower.contains("джун")) return "Junior";
        return null;
    }

    private int resolveRegistrationForWork(String rawText, BigDecimal customerRate, BigDecimal salaryOffer) {
        String lower = StringUtils.isNotBlank(rawText) ? rawText.toLowerCase(Locale.ROOT) : "";

        // Если указана ставка в час / T&M / рейт -> аутстаффинг
        if (lower.contains("t&m") || lower.contains("т&м") || lower.contains("в час") || lower.contains("руб/час")
                || lower.contains("руб./час") || lower.contains("руб/ч") || lower.contains("р/час")
                || lower.contains("ставка заказчика") || lower.contains("почасов") || lower.contains("аутстаф")) {
            return 0; // Аутстаффинг
        }

        // Если указано четкое зарплатное предложение (оклад за месяц) без почасовой ставки
        if (lower.contains("руб/мес") || lower.contains("в месяц") || lower.contains("оклад")
                || lower.contains("зарплатное предложение") || Pattern.compile("(?iu)\\b(net|gross)\\b").matcher(lower).find()) {
            return 1; // Рекрутинг
        }

        // Если задано зарплатное предложение (оклад) и нет ставки в час -> рекрутинг
        if (salaryOffer != null && salaryOffer.compareTo(new BigDecimal("50000")) >= 0 && customerRate == null) {
            return 1; // Рекрутинг
        }

        return 0; // аутстаффинг по умолчанию, если не указано иное
    }

    private BigDecimal extractCustomerRate(String rawText) {
        if (StringUtils.isBlank(rawText)) return null;
        Pattern p = Pattern.compile("(?iu)(?:ставка|рейт|t&m|т&м|ограничение по ставке)[^0-9\n\r]{0,35}(\\d[\\d\\s]{2,5})(?!\\s*000\\s*руб\\/мес)(?:\\s*(?:руб|р|₽)?\\s*(?:\\/|\\s*в\\s*)?(?:час|ч)?)");
        Matcher m = p.matcher(rawText);
        if (m.find()) {
            String numStr = m.group(1).replaceAll("\\s+", "");
            try {
                BigDecimal val = new BigDecimal(numStr);
                if (val.compareTo(new BigDecimal("500")) >= 0 && val.compareTo(new BigDecimal("20000")) <= 0) {
                    return val;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private OutstaffingRates findOutstaffingRate(BigDecimal customerRate) {
        if (customerRate == null) return null;
        List<OutstaffingRates> list = dataManager.load(OutstaffingRates.class)
                .query("select e from hunttech_OutstaffingRates e where e.rate <= :rate and e.minSalary is not null order by e.rate desc")
                .parameter("rate", customerRate)
                .maxResults(1)
                .list();
        return list.isEmpty() ? null : list.get(0);
    }

    private Date resolveClosingDate(String rawText) {
        if (StringUtils.isBlank(rawText)) return null;
        Pattern pattern = Pattern.compile("(?iu)(?:резюме\\s+принимаются\\s+до|прием\\s+резюме\\s+до|подача\\s+до|дедлайн[:\\s]+|срок\\s+до)\\s*([0-9]{1,2}[.\\/-][0-9]{1,2}[.\\/-][0-9]{2,4}|[0-9]{1,2}\\s+[а-яёА-ЯЁ]+(?:\\s+[0-9]{4})?)");
        Matcher m = pattern.matcher(rawText);
        if (m.find()) {
            String dateStr = m.group(1).trim();
            Date parsed = parseDateString(dateStr);
            if (parsed != null) return parsed;
        }
        return null;
    }

    private Date parseDateString(String dateStr) {
        if (StringUtils.isBlank(dateStr)) return null;
        String clean = dateStr.trim();
        String[] patterns = new String[]{"dd.MM.yyyy", "dd.MM.yy", "yyyy-MM-dd", "dd/MM/yyyy", "dd-MM-yyyy"};
        for (String p : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(p);
                sdf.setLenient(false);
                return sdf.parse(clean);
            } catch (Exception ignored) {}
        }

        Pattern monthPattern = Pattern.compile("(?iu)^([0-9]{1,2})\\s+([а-яёА-ЯЁ]+)(?:\\s+([0-9]{4}))?");
        Matcher mm = monthPattern.matcher(clean);
        if (mm.find()) {
            try {
                int day = Integer.parseInt(mm.group(1));
                String monthName = mm.group(2).toLowerCase(Locale.ROOT);
                int year = mm.group(3) != null ? Integer.parseInt(mm.group(3)) : Calendar.getInstance().get(Calendar.YEAR);
                int month = parseRuMonth(monthName);
                if (month >= 0) {
                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.YEAR, year);
                    cal.set(Calendar.MONTH, month);
                    cal.set(Calendar.DAY_OF_MONTH, day);
                    cal.set(Calendar.HOUR_OF_DAY, 23);
                    cal.set(Calendar.MINUTE, 59);
                    cal.set(Calendar.SECOND, 59);
                    return cal.getTime();
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private int parseRuMonth(String monthName) {
        if (monthName.startsWith("янв")) return 0;
        if (monthName.startsWith("фев")) return 1;
        if (monthName.startsWith("мар")) return 2;
        if (monthName.startsWith("апр")) return 3;
        if (monthName.startsWith("май") || monthName.startsWith("мае") || monthName.startsWith("мая")) return 4;
        if (monthName.startsWith("июн")) return 5;
        if (monthName.startsWith("июл")) return 6;
        if (monthName.startsWith("авг")) return 7;
        if (monthName.startsWith("сен")) return 8;
        if (monthName.startsWith("окт")) return 9;
        if (monthName.startsWith("ноя")) return 10;
        if (monthName.startsWith("дек")) return 11;
        return -1;
    }

    private int resolveWorkExperience(String rawText, String vacancyName, Grade grade, Position positionType) {
        if (StringUtils.isNotBlank(rawText)) {
            Pattern p = Pattern.compile("(?iu)(?:опыт(?:\\s+работы)?|стаж|коммерческий опыт)[^0-9\n\r]{0,25}(?:от|более|>)?\\s*([1-9]|1[0-5])\\s*(?:лет|года|год|\\+)");
            Matcher m = p.matcher(rawText);
            if (m.find()) {
                try {
                    return Integer.parseInt(m.group(1));
                } catch (Exception ignored) {}
            }
        }

        String textToAnalyze = "";
        if (StringUtils.isNotBlank(vacancyName)) {
            textToAnalyze += " " + vacancyName.toLowerCase(Locale.ROOT);
        }
        if (grade != null && StringUtils.isNotBlank(grade.getGradeName())) {
            textToAnalyze += " " + grade.getGradeName().toLowerCase(Locale.ROOT);
        }
        if (positionType != null) {
            textToAnalyze += " " + StringUtils.trimToEmpty(positionType.getPositionRuName()).toLowerCase(Locale.ROOT);
            textToAnalyze += " " + StringUtils.trimToEmpty(positionType.getPositionEnName()).toLowerCase(Locale.ROOT);
        }
        if (StringUtils.isNotBlank(rawText)) {
            textToAnalyze += " " + rawText.toLowerCase(Locale.ROOT);
        }

        if (textToAnalyze.contains("architect") || textToAnalyze.contains("архитектор")
                || textToAnalyze.contains("senior") || textToAnalyze.contains("сеньор") || textToAnalyze.contains("сениор")
                || textToAnalyze.contains("lead") || textToAnalyze.contains("лид")
                || textToAnalyze.contains("руководитель")) {
            return 5;
        }

        if (textToAnalyze.contains("junior") || textToAnalyze.contains("джуниор") || textToAnalyze.contains("джун")) {
            return 2;
        }

        return 3;
    }

    private String translateVacancyDescriptionToEnglish(String commentHtml, SmartOpenPositionParsedData parsedData, String rawText) {
        if (StringUtils.isNotBlank(commentHtml)) {
            try {
                String prompt = "Translate the following Russian job vacancy description into professional English. "
                        + "Keep all HTML tags intact (such as <h3>, <p>, <ul>, <li>, <strong>, etc.) and preserve exact technical terms and structure. "
                        + "Return ONLY the translated HTML content without markdown code blocks, backticks, or any conversational preamble.\n\n"
                        + commentHtml;
                Map<String, Object> params = new HashMap<>();
                params.put("prompt", prompt);
                params.put("text", commentHtml);
                AiExecutionResult result = aiExecutionService.executeText("TEXT_SMART_FORMAT_HTML", params);
                if (result != null && StringUtils.isNotBlank(result.getText())) {
                    String translated = result.getText().trim();
                    if (translated.startsWith("```")) {
                        translated = translated.replaceAll("(?s)^```[a-zA-Z]*\\s*", "").replaceAll("```\\s*$", "").trim();
                    }
                    if (translated.length() > 50) {
                        String lowerTrans = translated.toLowerCase(Locale.ROOT);
                        if (lowerTrans.contains("<p>") || lowerTrans.contains("<h3>") || lowerTrans.contains("<ul>") || lowerTrans.contains("<div>")) {
                            return MarkdownToHtmlUtils.sanitizeDangerousHtml(translated);
                        }
                        return MarkdownToHtmlUtils.toHtml(translated);
                    }
                }
            } catch (Exception ex) {
                log.warn("AI translation of vacancy description failed, falling back: {}", ex.getMessage());
            }
        }

        if (parsedData != null && StringUtils.isNotBlank(parsedData.getCommentEn())) {
            return MarkdownToHtmlUtils.toHtml(parsedData.getCommentEn());
        }

        return null;
    }

    private String generateCanonicalVacancyName(Grade grade, Position positionType, Project project, City city) {
        StringBuilder sb = new StringBuilder();

        if (grade != null && StringUtils.isNotBlank(grade.getGradeName())) {
            sb.append(grade.getGradeName().trim()).append(" ");
        }

        if (positionType != null) {
            String ru = StringUtils.trimToEmpty(positionType.getPositionRuName());
            String en = StringUtils.trimToEmpty(positionType.getPositionEnName());
            if (StringUtils.isNotBlank(ru) && StringUtils.isNotBlank(en)) {
                sb.append(ru).append(" / ").append(en);
            } else if (StringUtils.isNotBlank(ru)) {
                sb.append(ru);
            } else if (StringUtils.isNotBlank(en)) {
                sb.append(en);
            }
        }

        boolean hasProject = project != null && StringUtils.isNotBlank(project.getProjectName());
        boolean hasCity = city != null && StringUtils.isNotBlank(city.getCityRuName());

        if (hasProject || hasCity) {
            sb.append(" (");
            if (hasProject) {
                sb.append(project.getProjectName().trim());
                if (hasCity) {
                    sb.append(", ").append(city.getCityRuName().trim());
                }
            } else {
                sb.append(city.getCityRuName().trim());
            }
            sb.append(")");
        }

        return sb.toString().trim();
    }

    @Override
    public ProjectLogoResponseDto uploadProjectLogo(ProjectLogoUploadRequestDto request) {
        if (request == null) {
            return ProjectLogoResponseDto.error("Тело запроса отсутствует", null, "VALIDATION_ERROR");
        }

        String correlationId = request.getCorrelationId();
        String idempotencyKey = StringUtils.trimToNull(request.getIdempotencyKey());

        // Проверка идемпотентности по Idempotency-Key
        if (idempotencyKey != null) {
            ProjectLogoResponseDto cached = projectLogoIdempotencyCache.get(idempotencyKey);
            if (cached != null) {
                log.info("Returning cached response for idempotencyKey: {} [correlationId={}]", idempotencyKey, correlationId);
                return cached;
            }
        }

        if (StringUtils.isBlank(request.getLogoBase64())) {
            return ProjectLogoResponseDto.error("Поле logoBase64 обязательно для заполнения", correlationId, "VALIDATION_ERROR");
        }

        if (StringUtils.isNotBlank(request.getContentType())) {
            String ct = request.getContentType().trim().toLowerCase(Locale.ROOT);
            if (!ct.startsWith("image/")) {
                return ProjectLogoResponseDto.error("Недопустимый contentType: ожидается image/*, получено: " + request.getContentType(), correlationId, "VALIDATION_ERROR");
            }
        }

        ImageValidationResult imageResult = validateAndDecodeLogo(request.getLogoBase64());
        if (!imageResult.isValid()) {
            return ProjectLogoResponseDto.error(imageResult.errorMessage, correlationId, imageResult.errorCode);
        }
        byte[] logoBytes = imageResult.bytes;

        try {
            ProjectLogoResponseDto response;
            synchronized (projectVacancyLock) {
                if (idempotencyKey != null) {
                    ProjectLogoResponseDto cached = projectLogoIdempotencyCache.get(idempotencyKey);
                    if (cached != null) {
                        return cached;
                    }
                }

                Project targetProject = null;

                // 1. Поиск по projectId
                if (StringUtils.isNotBlank(request.getProjectId())) {
                    try {
                        UUID pUuid = UUID.fromString(request.getProjectId().trim());
                        targetProject = dataManager.load(Project.class).id(pUuid).view("project-logo-upload-view").optional().orElse(null);
                    } catch (IllegalArgumentException ex) {
                        log.warn("Invalid projectId UUID format [correlationId={}]: {}", correlationId, request.getProjectId());
                    }
                }

                // 2. Поиск по vacancyId
                if (targetProject == null && StringUtils.isNotBlank(request.getVacancyId())) {
                    try {
                        UUID vUuid = UUID.fromString(request.getVacancyId().trim());
                        OpenPosition pos = dataManager.load(OpenPosition.class).id(vUuid).view("openPosition-view").optional().orElse(null);
                        if (pos != null && pos.getProjectName() != null) {
                            targetProject = dataManager.load(Project.class).id(pos.getProjectName().getId()).view("project-logo-upload-view").optional().orElse(null);
                        }
                    } catch (IllegalArgumentException ex) {
                        log.warn("Invalid vacancyId UUID format [correlationId={}]: {}", correlationId, request.getVacancyId());
                    }
                }

                // 3. Поиск по externalId вакансии (с дедупликацией проектов и проверкой неоднозначности)
                if (targetProject == null && StringUtils.isNotBlank(request.getExternalId())) {
                    List<OpenPosition> list = dataManager.load(OpenPosition.class)
                            .query("select p from hunttech_OpenPosition p where p.vacansyID = :extId and p.deleteTs is null")
                            .parameter("extId", request.getExternalId().trim())
                            .view("openPosition-view")
                            .list();
                    Set<UUID> projectIds = list.stream()
                            .map(OpenPosition::getProjectName)
                            .filter(Objects::nonNull)
                            .map(Project::getId)
                            .collect(Collectors.toSet());
                    if (projectIds.size() > 1) {
                        return ProjectLogoResponseDto.error("Найдено несколько вакансий с externalId '" + request.getExternalId() + "' в разных проектах. Укажите projectId или vacancyId.", correlationId, "AMBIGUOUS_EXTERNAL_ID");
                    } else if (projectIds.size() == 1) {
                        UUID pId = projectIds.iterator().next();
                        targetProject = dataManager.load(Project.class).id(pId).view("project-logo-upload-view").optional().orElse(null);
                    }
                }

                // 4. Поиск по projectName (точное совпадение с контролем неоднозначности)
                if (targetProject == null && StringUtils.isNotBlank(request.getProjectName())) {
                    List<Project> list = dataManager.load(Project.class)
                            .query("select p from hunttech_Project p where lower(p.projectName) = lower(:name) and p.deleteTs is null")
                            .parameter("name", request.getProjectName().trim())
                            .view("project-logo-upload-view")
                            .maxResults(2)
                            .list();
                    if (list.size() > 1) {
                        return ProjectLogoResponseDto.error("Найдено несколько проектов с наименованием '" + request.getProjectName() + "'. Укажите projectId или vacancyId.", correlationId, "AMBIGUOUS_PROJECT_NAME");
                    }
                    if (!list.isEmpty()) {
                        targetProject = list.get(0);
                    }
                }

                if (targetProject == null) {
                    return ProjectLogoResponseDto.error("Проект для привязки логотипа не найден по переданным критериям (projectId, vacancyId, externalId или projectName)", correlationId, "PROJECT_NOT_FOUND");
                }

                targetProject.setProjectLogoBlob(logoBytes);
                dataManager.commit(targetProject);

                log.info("Project logo BLOB successfully uploaded for project '{}' (id={}, size={} bytes) [correlationId={}]",
                        targetProject.getProjectName(), targetProject.getId(), logoBytes.length, correlationId);

                response = ProjectLogoResponseDto.success(
                        targetProject.getId().toString(),
                        targetProject.getProjectName(),
                        logoBytes.length,
                        correlationId
                );

                if (idempotencyKey != null) {
                    cacheResponse(projectLogoIdempotencyCache, idempotencyKey, response);
                }
            }
            return response;
        } catch (Exception ex) {
            log.error("Failed to upload project logo [correlationId={}]: {}", correlationId, ex.getMessage(), ex);
            return ProjectLogoResponseDto.error("Ошибка сохранения логотипа проекта. Correlation ID: " + correlationId, correlationId, "INTERNAL_ERROR");
        }
    }

    private static class ImageValidationResult {
        final byte[] bytes;
        final String errorCode;
        final String errorMessage;

        private ImageValidationResult(byte[] bytes, String errorCode, String errorMessage) {
            this.bytes = bytes;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }

        static ImageValidationResult success(byte[] bytes) {
            return new ImageValidationResult(bytes, null, null);
        }

        static ImageValidationResult error(String errorCode, String errorMessage) {
            return new ImageValidationResult(null, errorCode, errorMessage);
        }

        boolean isValid() {
            return bytes != null;
        }
    }

    private ImageValidationResult validateAndDecodeLogo(String base64) {
        if (StringUtils.isBlank(base64)) {
            return ImageValidationResult.error("VALIDATION_ERROR", "Поле с логотипом не должно быть пустым");
        }
        if (base64.length() > MAX_BASE64_LOGO_CHARS) {
            return ImageValidationResult.error("PAYLOAD_TOO_LARGE", "Размер Base64 строки превышает допустимый лимит (~7 МБ)");
        }
        String cleaned = base64.trim();
        if (cleaned.startsWith("data:") && cleaned.contains(";base64,")) {
            cleaned = cleaned.substring(cleaned.indexOf(";base64,") + 8).trim();
        }
        cleaned = cleaned.replaceAll("\\s+", "");

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(cleaned);
        } catch (IllegalArgumentException ex) {
            return ImageValidationResult.error("VALIDATION_ERROR", "Не удалось декодировать изображение из Base64: некорректная Base64 строка");
        }

        if (bytes == null || bytes.length == 0) {
            return ImageValidationResult.error("VALIDATION_ERROR", "Пустые данные изображения после декодирования Base64");
        }

        if (bytes.length > MAX_LOGO_SIZE_BYTES) {
            return ImageValidationResult.error("PAYLOAD_TOO_LARGE", "Размер логотипа превышает максимально допустимый лимит (5 МБ)");
        }

        if (bytes.length < 4) {
            return ImageValidationResult.error("INVALID_IMAGE_FORMAT", "Недопустимый формат файла: слишком короткий заголовок");
        }

        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (iis == null) {
                return ImageValidationResult.error("INVALID_IMAGE_FORMAT", "Загружаемый файл не является поддерживаемым изображением");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                return ImageValidationResult.error("INVALID_IMAGE_FORMAT", "Загружаемый файл не является поддерживаемым изображением (PNG, JPEG, GIF, WEBP)");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION) {
                    return ImageValidationResult.error("IMAGE_DIMENSION_EXCEEDED", "Габариты изображения (" + width + "x" + height + " px) превышают максимально допустимые " + MAX_IMAGE_DIMENSION + "x" + MAX_IMAGE_DIMENSION + " px");
                }
            } finally {
                reader.dispose();
            }
        } catch (Exception ex) {
            log.warn("Image format validation failed: {}", ex.getMessage());
            return ImageValidationResult.error("INVALID_IMAGE_FORMAT", "Ошибка чтения формата изображения: " + ex.getMessage());
        }

        return ImageValidationResult.success(bytes);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}



