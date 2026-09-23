package com.company.hunttech.service;

import com.company.hunttech.dto.integration.CandidateCompositeCreateRequestDto;
import com.company.hunttech.dto.integration.CandidateCompositeResponseDto;
import com.company.hunttech.dto.integration.CandidateCvCreateRequestDto;
import com.company.hunttech.dto.integration.CandidateCvResponseDto;
import com.company.hunttech.dto.integration.CompanyCreateRequestDto;
import com.company.hunttech.dto.integration.CompanyResponseDto;
import com.company.hunttech.dto.integration.InteractionCreateRequestDto;
import com.company.hunttech.dto.integration.InteractionResponseDto;
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

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service(ExternalIntegrationService.NAME)
public class ExternalIntegrationServiceBean implements ExternalIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(ExternalIntegrationServiceBean.class);

    // In-memory кэш ответов для идемпотентности запросов в рамках процесса (BL-2026-030/036)
    private final Map<String, CompanyResponseDto> idempotencyCache = new ConcurrentHashMap<>();
    private final Map<String, ProjectVacancyResponseDto> projectVacancyIdempotencyCache = new ConcurrentHashMap<>();
    private final Map<String, CandidateCvResponseDto> candidateCvIdempotencyCache = new ConcurrentHashMap<>();
    private final Map<String, InteractionResponseDto> interactionIdempotencyCache = new ConcurrentHashMap<>();
    private final Map<String, CandidateCompositeResponseDto> candidateCompositeIdempotencyCache = new ConcurrentHashMap<>();

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
        boolean hasProjectName = StringUtils.isNotBlank(request.getProjectName());

        if (!hasExistingProject && !hasProjectName) {
            return com.company.hunttech.dto.integration.ProjectVacancyResponseDto.error(
                    "Необходимо указать existingProjectId или projectName для привязки вакансии к проекту", correlationId, "VALIDATION_ERROR");
        }

        // Предварительная валидация existingProjectId (если задан) до обращения к LLM
        if (hasExistingProject) {
            try {
                UUID projectUuid = UUID.fromString(request.getExistingProjectId().trim());
                com.company.hunttech.entity.Project existingProj = dataManager.load(com.company.hunttech.entity.Project.class)
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

        // Оригинал вакансии для сохранения в rawDescription и генерации AI-артефактов
        String rawText = StringUtils.isNotBlank(request.getComment())
                ? request.getComment().trim()
                : StringUtils.trimToEmpty(request.getShortDescription());

        // Внутренняя AI-генерация согласно существующим в базе промптам:
        // 1) "Описание вакансии" (STANDARDIZE_VACANCY)
        // 2) "Чеклист" (VACANCY_CHECKLIST)
        // 3) "Карта поиска" (VACANCY_SEARCH_MAP)
        // 4) "План собеседования" (VACANCY_INTERVIEW_PLAN)
        // Выполняется ДО входа в synchronized блок, исключая блокировку потоков долгими LLM-вызовами
        String standardizedDescription = null;
        String checklist = null;
        String searchMap = null;
        String interviewPlan = null;

        if (StringUtils.isNotBlank(rawText)) {
            try {
                log.info("Starting AI vacancy standardization for external vacancy [name='{}', correlationId={}]",
                        request.getVacancyName(), correlationId);
                standardizedDescription = hrmAiService.standardizeVacancyDescription(rawText);
            } catch (Exception ex) {
                log.warn("Failed to standardize vacancy description via AI [correlationId={}]: {}", correlationId, ex.getMessage(), ex);
            }

            String textForArtifacts = StringUtils.isNotBlank(standardizedDescription) ? standardizedDescription : rawText;

            try {
                checklist = hrmAiService.generateChecklist(textForArtifacts);
            } catch (Exception ex) {
                log.warn("Failed to generate vacancy checklist via AI [correlationId={}]: {}", correlationId, ex.getMessage(), ex);
            }

            try {
                searchMap = hrmAiService.generateSearchMap(textForArtifacts);
            } catch (Exception ex) {
                log.warn("Failed to generate vacancy search map via AI [correlationId={}]: {}", correlationId, ex.getMessage(), ex);
            }

            try {
                interviewPlan = hrmAiService.generateInterviewPlan(textForArtifacts);
            } catch (Exception ex) {
                log.warn("Failed to generate vacancy interview plan via AI [correlationId={}]: {}", correlationId, ex.getMessage(), ex);
            }

            log.info("AI vacancy enrichment completed [correlationId={}]: desc={}, checklist={}, searchMap={}, interviewPlan={}",
                    correlationId,
                    standardizedDescription != null ? "OK" : "FALLBACK",
                    checklist != null ? "OK" : "SKIPPED",
                    searchMap != null ? "OK" : "SKIPPED",
                    interviewPlan != null ? "OK" : "SKIPPED");
        }

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

                com.haulmont.cuba.core.global.CommitContext commitContext = new com.haulmont.cuba.core.global.CommitContext();

                // 1. Поиск или создание проекта
                com.company.hunttech.entity.Project project = null;
                if (hasExistingProject) {
                    try {
                        UUID projectUuid = UUID.fromString(request.getExistingProjectId().trim());
                        project = dataManager.load(com.company.hunttech.entity.Project.class).id(projectUuid).optional().orElse(null);
                        if (project == null) {
                            return com.company.hunttech.dto.integration.ProjectVacancyResponseDto.error(
                                    "Проект с указанным existingProjectId не найден: " + request.getExistingProjectId(),
                                    correlationId, "NOT_FOUND");
                        }
                    } catch (IllegalArgumentException ex) {
                        return com.company.hunttech.dto.integration.ProjectVacancyResponseDto.error(
                                "Некорректный формат UUID для existingProjectId: " + request.getExistingProjectId(),
                                correlationId, "VALIDATION_ERROR");
                    }
                } else {
                    String cleanProjectName = request.getProjectName().trim();
                    String lookupName = truncate(cleanProjectName, 160);
                    // Поиск существующего открытого проекта по имени
                    project = dataManager.load(com.company.hunttech.entity.Project.class)
                            .query("select e from hunttech_Project e where lower(e.projectName) = :name and (e.projectIsClosed is null or e.projectIsClosed = false)")
                            .parameter("name", lookupName.toLowerCase(Locale.ROOT))
                            .optional()
                            .orElse(null);

                    if (project == null) {
                        project = metadata.create(com.company.hunttech.entity.Project.class);
                        project.setProjectName(truncate(cleanProjectName, 160));
                        project.setProjectDescription(request.getProjectDescription());
                        project.setShortDescription(truncate(request.getProjectDescription(), 250));
                        project.setProjectIsClosed(false);
                        project.setStartProjectDate(new java.util.Date());

                        // Привязка компании/департамента при наличии companyId
                        if (StringUtils.isNotBlank(request.getCompanyId())) {
                            try {
                                UUID compUuid = UUID.fromString(request.getCompanyId().trim());
                                Company company = dataManager.load(Company.class).id(compUuid).optional().orElse(null);
                                if (company != null) {
                                    com.company.hunttech.entity.CompanyDepartament dept = dataManager.load(com.company.hunttech.entity.CompanyDepartament.class)
                                            .query("select d from hunttech_CompanyDepartament d where d.companyName.id = :compId")
                                            .parameter("compId", compUuid)
                                            .optional()
                                            .orElse(null);

                                    if (dept == null) {
                                        dept = metadata.create(com.company.hunttech.entity.CompanyDepartament.class);
                                        dept.setDepartamentRuName("Основной");
                                        dept.setCompanyName(company);
                                        commitContext.addInstanceToCommit(dept);
                                    }
                                    project.setProjectDepartment(dept);
                                }
                            } catch (IllegalArgumentException ex) {
                                log.warn("Invalid companyId format [correlationId={}]: {}", correlationId, request.getCompanyId(), ex);
                            }
                        }

                        commitContext.addInstanceToCommit(project);
                    }
                }

                // 2. Разрешение связей для вакансии (Grade, City, Position)
                com.company.hunttech.entity.Grade grade = null;
                if (StringUtils.isNotBlank(request.getGradeId())) {
                    try {
                        UUID gradeUuid = UUID.fromString(request.getGradeId().trim());
                        grade = dataManager.load(com.company.hunttech.entity.Grade.class).id(gradeUuid).optional().orElse(null);
                    } catch (IllegalArgumentException ex) {
                        log.warn("Invalid gradeId format [correlationId={}]: {}", correlationId, request.getGradeId(), ex);
                    }
                }

                com.company.hunttech.entity.City city = null;
                if (StringUtils.isNotBlank(request.getCityId())) {
                    try {
                        UUID cityUuid = UUID.fromString(request.getCityId().trim());
                        city = dataManager.load(com.company.hunttech.entity.City.class).id(cityUuid).optional().orElse(null);
                    } catch (IllegalArgumentException ex) {
                        log.warn("Invalid cityId format [correlationId={}]: {}", correlationId, request.getCityId(), ex);
                    }
                }

                com.company.hunttech.entity.Position positionType = null;
                if (StringUtils.isNotBlank(request.getPositionTypeId())) {
                    try {
                        UUID posUuid = UUID.fromString(request.getPositionTypeId().trim());
                        positionType = dataManager.load(com.company.hunttech.entity.Position.class).id(posUuid).optional().orElse(null);
                    } catch (IllegalArgumentException ex) {
                        log.warn("Invalid positionTypeId format [correlationId={}]: {}", correlationId, request.getPositionTypeId(), ex);
                    }
                }

                // 3. Создание сущности OpenPosition
                com.company.hunttech.entity.OpenPosition openPosition = metadata.create(com.company.hunttech.entity.OpenPosition.class);
                openPosition.setVacansyName(truncate(request.getVacancyName().trim(), 250));
                openPosition.setProjectName(project);

                if (StringUtils.isNotBlank(request.getExternalId())) {
                    openPosition.setVacansyID(truncate(request.getExternalId().trim(), 16));
                } else {
                    openPosition.setVacansyID("EXT-" + Math.abs(UUID.randomUUID().hashCode() % 1000000));
                }

                String shortDesc = request.getShortDescription() != null && !request.getShortDescription().trim().isEmpty()
                        ? request.getShortDescription() : request.getVacancyName();
                openPosition.setShortDescription(truncate(shortDesc, 250));

                openPosition.setRawDescription(rawText);

                // Запись стандартизированного описания (или оригинала при сбое AI) в comment
                openPosition.setComment(StringUtils.isNotBlank(standardizedDescription) ? standardizedDescription : rawText);

                // Запись чеклиста требований (синхронно в interviewChecklist и exercise)
                if (StringUtils.isNotBlank(checklist)) {
                    openPosition.setInterviewChecklist(checklist);
                    openPosition.setExercise(checklist);
                    openPosition.setNeedExercise(true);
                }

                // Запись карты поиска (синхронно в searchMap и memoForInterview)
                if (StringUtils.isNotBlank(searchMap)) {
                    openPosition.setSearchMap(searchMap);
                    openPosition.setMemoForInterview(searchMap);
                    openPosition.setNeedMemoForInterview(true);
                }

                // Запись плана собеседования (синхронно в interviewPlan и templateLetter)
                if (StringUtils.isNotBlank(interviewPlan)) {
                    openPosition.setInterviewPlan(interviewPlan);
                    openPosition.setTemplateLetter(interviewPlan);
                    openPosition.setNeedLetter(true);
                }

                openPosition.setRemoteWork(request.getRemoteWork() != null ? request.getRemoteWork() : 1);
                openPosition.setCommandCandidate(request.getCommandCandidate() != null ? request.getCommandCandidate() : 1);
                openPosition.setWorkExperience(request.getWorkExperience() != null ? request.getWorkExperience() : 1);
                openPosition.setSalaryMin(request.getSalaryMin());
                openPosition.setSalaryMax(request.getSalaryMax());
                openPosition.setGrade(grade);
                openPosition.setCityPosition(city);
                openPosition.setPositionType(positionType);
                openPosition.setOpenClose(false);
                openPosition.setSignDraft(false);
                openPosition.setInternalProject(false);
                openPosition.setLastOpenDate(new java.util.Date());
                openPosition.setPriority(2); // NORMAL

                commitContext.addInstanceToCommit(openPosition);

                // Атомарный коммит всех сущностей в одной транзакции
                dataManager.commit(commitContext);

                log.info("Successfully created project and vacancy: projectId={}, vacancyId={}, vacancyName='{}' [correlationId={}]",
                        project.getId(), openPosition.getId(), openPosition.getVacansyName(), correlationId);

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

                JobCandidate candidate = dataManager.load(JobCandidate.class).id(candidateUuid).optional().orElse(null);
                if (candidate == null) {
                    return CandidateCvResponseDto.error("Кандидат с указанным candidateId не найден: " + request.getCandidateId(), correlationId, "NOT_FOUND");
                }

                OpenPosition toVacancy = null;
                if (StringUtils.isNotBlank(request.getVacancyId())) {
                    try {
                        UUID vacUuid = UUID.fromString(request.getVacancyId().trim());
                        toVacancy = dataManager.load(OpenPosition.class).id(vacUuid).optional().orElse(null);
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

                JobCandidate candidate = dataManager.load(JobCandidate.class).id(candidateUuid).optional().orElse(null);
                if (candidate == null) {
                    return InteractionResponseDto.error("Кандидат с указанным candidateId не найден: " + request.getCandidateId(), correlationId, "NOT_FOUND");
                }

                OpenPosition vacancy = dataManager.load(OpenPosition.class).id(vacancyUuid).optional().orElse(null);
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
                            .optional()
                            .orElse(null);
                }

                // 2. Дедупликация по email
                if (candidate == null && StringUtils.isNotBlank(request.getEmail())) {
                    String cleanEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
                    candidate = dataManager.load(JobCandidate.class)
                            .query("select c from hunttech_JobCandidate c where lower(c.email) = :email")
                            .parameter("email", cleanEmail)
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
                        OpenPosition vacancy = dataManager.load(OpenPosition.class).id(vacUuid).optional().orElse(null);
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

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}



