package com.company.hunttech.service;

import com.company.hunttech.dto.integration.CompanyCreateRequestDto;
import com.company.hunttech.dto.integration.CompanyResponseDto;
import com.company.hunttech.entity.City;
import com.company.hunttech.entity.Company;
import com.company.hunttech.entity.Country;
import com.company.hunttech.entity.Region;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service(ExternalIntegrationService.NAME)
public class ExternalIntegrationServiceBean implements ExternalIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(ExternalIntegrationServiceBean.class);

    // In-memory кэш ответов для идемпотентности запросов в рамках процесса (BL-2026-030/036)
    private final Map<String, CompanyResponseDto> idempotencyCache = new ConcurrentHashMap<>();
    private final Map<String, com.company.hunttech.dto.integration.ProjectVacancyResponseDto> projectVacancyIdempotencyCache = new ConcurrentHashMap<>();
    private final Object companyCreateLock = new Object();
    private final Object projectVacancyLock = new Object();

    @Inject
    private DataManager dataManager;

    @Inject
    private Metadata metadata;

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
                openPosition.setComment(request.getComment() != null ? request.getComment() : "");
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

    private void cacheIfIdempotent(String idempotencyKey, CompanyResponseDto response) {
        if (idempotencyKey != null && response != null && response.isSuccess()) {
            cacheResponse(idempotencyCache, idempotencyKey, response);
        }
    }

    private void cacheIfIdempotent(String idempotencyKey, com.company.hunttech.dto.integration.ProjectVacancyResponseDto response) {
        if (idempotencyKey != null && response != null && response.isSuccess()) {
            cacheResponse(projectVacancyIdempotencyCache, idempotencyKey, response);
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


