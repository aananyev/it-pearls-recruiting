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
    private final Object companyCreateLock = new Object();

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

    private void cacheIfIdempotent(String idempotencyKey, CompanyResponseDto response) {
        if (idempotencyKey != null && response != null && response.isSuccess()) {
            // Ограничение размера кэша для защиты от утечки памяти
            if (idempotencyCache.size() > 5000) {
                idempotencyCache.clear();
            }
            idempotencyCache.put(idempotencyKey, response);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
