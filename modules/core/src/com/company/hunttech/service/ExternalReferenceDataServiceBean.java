package com.company.hunttech.service;

import com.company.hunttech.dto.integration.ReferenceFilterDto;
import com.company.hunttech.dto.integration.ReferenceItemDto;
import com.company.hunttech.dto.integration.ReferenceListResponseDto;
import com.company.hunttech.entity.City;
import com.company.hunttech.entity.Country;
import com.company.hunttech.entity.Grade;
import com.company.hunttech.entity.Iteraction;
import com.company.hunttech.entity.Position;
import com.company.hunttech.entity.SkillTree;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FluentLoader;
import com.haulmont.cuba.core.global.FluentValueLoader;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service(ExternalReferenceDataService.NAME)
public class ExternalReferenceDataServiceBean implements ExternalReferenceDataService {

    private static final Logger log = LoggerFactory.getLogger(ExternalReferenceDataServiceBean.class);
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 500;
    private static final int MAX_OFFSET = 10000;

    @Inject
    private DataManager dataManager;

    @Override
    public ReferenceListResponseDto getCities(ReferenceFilterDto filter) {
        String correlationId = filter != null ? filter.getCorrelationId() : null;
        String paginationError = validatePagination(filter);
        if (paginationError != null) {
            return ReferenceListResponseDto.error(paginationError, correlationId, "VALIDATION_ERROR");
        }

        try {
            int limit = resolveLimit(filter);
            int offset = resolveOffset(filter);
            String search = filter != null ? filter.getSearch() : null;

            StringBuilder countJpql = new StringBuilder("select count(e) from hunttech_City e where 1=1 ");
            StringBuilder selectJpql = new StringBuilder("select e from hunttech_City e where 1=1 ");

            if (StringUtils.isNotBlank(search)) {
                String searchClause = "and (lower(e.cityRuName) like :search escape '\\' or lower(e.cityEngName) like :search escape '\\') ";
                countJpql.append(searchClause);
                selectJpql.append(searchClause);
            }
            selectJpql.append("order by e.cityRuName asc");

            FluentValueLoader<Long> countLoader = dataManager.loadValue(countJpql.toString(), Long.class);
            FluentLoader.ByQuery<City, UUID> selectLoader = dataManager.load(City.class)
                    .query(selectJpql.toString())
                    .firstResult(offset)
                    .maxResults(limit);

            if (StringUtils.isNotBlank(search)) {
                String pattern = "%" + escapeLikePattern(search) + "%";
                countLoader.parameter("search", pattern);
                selectLoader.parameter("search", pattern);
            }

            Long total = countLoader.optional().orElse(0L);
            List<City> cities = selectLoader.list();

            List<ReferenceItemDto> items = new ArrayList<>();
            for (City city : cities) {
                items.add(new ReferenceItemDto(
                        city.getId().toString(),
                        city.getFiasId(),
                        city.getCityRuName(),
                        null,
                        true,
                        city.getCityEngName()
                ));
            }

            return ReferenceListResponseDto.ok(items, total != null ? total.intValue() : items.size(), correlationId);
        } catch (Exception e) {
            log.error("Failed to fetch cities for external API [correlationId={}]", correlationId, e);
            return ReferenceListResponseDto.error("Ошибка при получении списка городов. Correlation ID: " + correlationId, correlationId, "SYSTEM_ERROR");
        }
    }

    @Override
    public ReferenceListResponseDto getPositions(ReferenceFilterDto filter) {
        String correlationId = filter != null ? filter.getCorrelationId() : null;
        String paginationError = validatePagination(filter);
        if (paginationError != null) {
            return ReferenceListResponseDto.error(paginationError, correlationId, "VALIDATION_ERROR");
        }

        try {
            int limit = resolveLimit(filter);
            int offset = resolveOffset(filter);
            String search = filter != null ? filter.getSearch() : null;

            StringBuilder countJpql = new StringBuilder("select count(e) from hunttech_Position e where 1=1 ");
            StringBuilder selectJpql = new StringBuilder("select e from hunttech_Position e where 1=1 ");

            if (StringUtils.isNotBlank(search)) {
                String searchClause = "and (lower(e.positionRuName) like :search escape '\\' or lower(e.positionEnName) like :search escape '\\') ";
                countJpql.append(searchClause);
                selectJpql.append(searchClause);
            }
            selectJpql.append("order by e.positionRuName asc");

            FluentValueLoader<Long> countLoader = dataManager.loadValue(countJpql.toString(), Long.class);
            FluentLoader.ByQuery<Position, UUID> selectLoader = dataManager.load(Position.class)
                    .query(selectJpql.toString())
                    .firstResult(offset)
                    .maxResults(limit);

            if (StringUtils.isNotBlank(search)) {
                String pattern = "%" + escapeLikePattern(search) + "%";
                countLoader.parameter("search", pattern);
                selectLoader.parameter("search", pattern);
            }

            Long total = countLoader.optional().orElse(0L);
            List<Position> positions = selectLoader.list();

            List<ReferenceItemDto> items = new ArrayList<>();
            for (Position pos : positions) {
                items.add(new ReferenceItemDto(
                        pos.getId().toString(),
                        null,
                        pos.getPositionRuName(),
                        null,
                        true,
                        pos.getPositionEnName()
                ));
            }

            return ReferenceListResponseDto.ok(items, total != null ? total.intValue() : items.size(), correlationId);
        } catch (Exception e) {
            log.error("Failed to fetch positions for external API [correlationId={}]", correlationId, e);
            return ReferenceListResponseDto.error("Ошибка при получении списка должностей. Correlation ID: " + correlationId, correlationId, "SYSTEM_ERROR");
        }
    }

    @Override
    public ReferenceListResponseDto getGrades(ReferenceFilterDto filter) {
        String correlationId = filter != null ? filter.getCorrelationId() : null;
        String paginationError = validatePagination(filter);
        if (paginationError != null) {
            return ReferenceListResponseDto.error(paginationError, correlationId, "VALIDATION_ERROR");
        }

        try {
            int limit = resolveLimit(filter);
            int offset = resolveOffset(filter);
            String search = filter != null ? filter.getSearch() : null;

            StringBuilder countJpql = new StringBuilder("select count(e) from hunttech_Grade e where 1=1 ");
            StringBuilder selectJpql = new StringBuilder("select e from hunttech_Grade e where 1=1 ");

            if (StringUtils.isNotBlank(search)) {
                String searchClause = "and lower(e.gradeName) like :search escape '\\' ";
                countJpql.append(searchClause);
                selectJpql.append(searchClause);
            }
            selectJpql.append("order by e.gradeName asc");

            FluentValueLoader<Long> countLoader = dataManager.loadValue(countJpql.toString(), Long.class);
            FluentLoader.ByQuery<Grade, UUID> selectLoader = dataManager.load(Grade.class)
                    .query(selectJpql.toString())
                    .firstResult(offset)
                    .maxResults(limit);

            if (StringUtils.isNotBlank(search)) {
                String pattern = "%" + escapeLikePattern(search) + "%";
                countLoader.parameter("search", pattern);
                selectLoader.parameter("search", pattern);
            }

            Long total = countLoader.optional().orElse(0L);
            List<Grade> grades = selectLoader.list();

            List<ReferenceItemDto> items = new ArrayList<>();
            for (Grade grade : grades) {
                items.add(new ReferenceItemDto(
                        grade.getId().toString(),
                        null,
                        grade.getGradeName(),
                        null,
                        true,
                        null
                ));
            }

            return ReferenceListResponseDto.ok(items, total != null ? total.intValue() : items.size(), correlationId);
        } catch (Exception e) {
            log.error("Failed to fetch grades for external API [correlationId={}]", correlationId, e);
            return ReferenceListResponseDto.error("Ошибка при получении списка грейдов. Correlation ID: " + correlationId, correlationId, "SYSTEM_ERROR");
        }
    }

    @Override
    public ReferenceListResponseDto getInteractionTypes(ReferenceFilterDto filter) {
        String correlationId = filter != null ? filter.getCorrelationId() : null;
        String paginationError = validatePagination(filter);
        if (paginationError != null) {
            return ReferenceListResponseDto.error(paginationError, correlationId, "VALIDATION_ERROR");
        }

        try {
            int limit = resolveLimit(filter);
            int offset = resolveOffset(filter);
            String search = filter != null ? filter.getSearch() : null;

            StringBuilder countJpql = new StringBuilder("select count(e) from hunttech_Iteraction e where 1=1 ");
            StringBuilder selectJpql = new StringBuilder("select e from hunttech_Iteraction e where 1=1 ");

            if (StringUtils.isNotBlank(search)) {
                String searchClause = "and (lower(e.iterationName) like :search escape '\\' or lower(e.number) like :search escape '\\') ";
                countJpql.append(searchClause);
                selectJpql.append(searchClause);
            }
            selectJpql.append("order by e.number asc, e.iterationName asc");

            FluentValueLoader<Long> countLoader = dataManager.loadValue(countJpql.toString(), Long.class);
            FluentLoader.ByQuery<Iteraction, UUID> selectLoader = dataManager.load(Iteraction.class)
                    .query(selectJpql.toString())
                    .firstResult(offset)
                    .maxResults(limit);

            if (StringUtils.isNotBlank(search)) {
                String pattern = "%" + escapeLikePattern(search) + "%";
                countLoader.parameter("search", pattern);
                selectLoader.parameter("search", pattern);
            }

            Long total = countLoader.optional().orElse(0L);
            List<Iteraction> interactions = selectLoader.list();

            List<ReferenceItemDto> items = new ArrayList<>();
            for (Iteraction it : interactions) {
                items.add(new ReferenceItemDto(
                        it.getId().toString(),
                        it.getNumber(),
                        it.getIterationName(),
                        it.getIteractionTree() != null ? it.getIteractionTree().getId().toString() : null,
                        true,
                        it.getCallButtonText()
                ));
            }

            return ReferenceListResponseDto.ok(items, total != null ? total.intValue() : items.size(), correlationId);
        } catch (Exception e) {
            log.error("Failed to fetch interaction types for external API [correlationId={}]", correlationId, e);
            return ReferenceListResponseDto.error("Ошибка при получении типов взаимодействий. Correlation ID: " + correlationId, correlationId, "SYSTEM_ERROR");
        }
    }

    @Override
    public ReferenceListResponseDto getSkills(ReferenceFilterDto filter) {
        String correlationId = filter != null ? filter.getCorrelationId() : null;
        String paginationError = validatePagination(filter);
        if (paginationError != null) {
            return ReferenceListResponseDto.error(paginationError, correlationId, "VALIDATION_ERROR");
        }

        try {
            int limit = resolveLimit(filter);
            int offset = resolveOffset(filter);
            String search = filter != null ? filter.getSearch() : null;
            String parentIdStr = filter != null ? filter.getParentId() : null;

            StringBuilder countJpql = new StringBuilder("select count(e) from hunttech_SkillTree e where 1=1 ");
            StringBuilder selectJpql = new StringBuilder("select e from hunttech_SkillTree e where 1=1 ");

            if (StringUtils.isNotBlank(search)) {
                String searchClause = "and lower(e.skillName) like :search escape '\\' ";
                countJpql.append(searchClause);
                selectJpql.append(searchClause);
            }

            UUID parentId = null;
            if (StringUtils.isNotBlank(parentIdStr)) {
                try {
                    parentId = UUID.fromString(parentIdStr.trim());
                    String parentClause = "and e.skillTree.id = :parentId ";
                    countJpql.append(parentClause);
                    selectJpql.append(parentClause);
                } catch (IllegalArgumentException ex) {
                    log.warn("Invalid parentId format received for skills [correlationId={}]: {}", correlationId, parentIdStr, ex);
                    return ReferenceListResponseDto.error("Неверный формат parentId: ожидался валидный UUID", correlationId, "VALIDATION_ERROR");
                }
            }

            selectJpql.append("order by e.skillName asc");

            FluentValueLoader<Long> countLoader = dataManager.loadValue(countJpql.toString(), Long.class);
            FluentLoader.ByQuery<SkillTree, UUID> selectLoader = dataManager.load(SkillTree.class)
                    .query(selectJpql.toString())
                    .firstResult(offset)
                    .maxResults(limit);

            if (StringUtils.isNotBlank(search)) {
                String pattern = "%" + escapeLikePattern(search) + "%";
                countLoader.parameter("search", pattern);
                selectLoader.parameter("search", pattern);
            }
            if (parentId != null) {
                countLoader.parameter("parentId", parentId);
                selectLoader.parameter("parentId", parentId);
            }

            Long total = countLoader.optional().orElse(0L);
            List<SkillTree> skills = selectLoader.list();

            List<ReferenceItemDto> items = new ArrayList<>();
            for (SkillTree skill : skills) {
                items.add(new ReferenceItemDto(
                        skill.getId().toString(),
                        null,
                        skill.getSkillName(),
                        skill.getSkillTree() != null ? skill.getSkillTree().getId().toString() : null,
                        true,
                        skill.getComment()
                ));
            }

            return ReferenceListResponseDto.ok(items, total != null ? total.intValue() : items.size(), correlationId);
        } catch (Exception e) {
            log.error("Failed to fetch skills for external API [correlationId={}]", correlationId, e);
            return ReferenceListResponseDto.error("Ошибка при получении дерева навыков. Correlation ID: " + correlationId, correlationId, "SYSTEM_ERROR");
        }
    }

    @Override
    public ReferenceListResponseDto getCountries(ReferenceFilterDto filter) {
        String correlationId = filter != null ? filter.getCorrelationId() : null;
        String paginationError = validatePagination(filter);
        if (paginationError != null) {
            return ReferenceListResponseDto.error(paginationError, correlationId, "VALIDATION_ERROR");
        }

        try {
            int limit = resolveLimit(filter);
            int offset = resolveOffset(filter);
            String search = filter != null ? filter.getSearch() : null;

            StringBuilder countJpql = new StringBuilder("select count(e) from hunttech_Country e where 1=1 ");
            StringBuilder selectJpql = new StringBuilder("select e from hunttech_Country e where 1=1 ");

            if (StringUtils.isNotBlank(search)) {
                String searchClause = "and (lower(e.countryRuName) like :search escape '\\' or lower(e.countryEngName) like :search escape '\\') ";
                countJpql.append(searchClause);
                selectJpql.append(searchClause);
            }
            selectJpql.append("order by e.countryRuName asc");

            FluentValueLoader<Long> countLoader = dataManager.loadValue(countJpql.toString(), Long.class);
            FluentLoader.ByQuery<Country, UUID> selectLoader = dataManager.load(Country.class)
                    .query(selectJpql.toString())
                    .firstResult(offset)
                    .maxResults(limit);

            if (StringUtils.isNotBlank(search)) {
                String pattern = "%" + escapeLikePattern(search) + "%";
                countLoader.parameter("search", pattern);
                selectLoader.parameter("search", pattern);
            }

            Long total = countLoader.optional().orElse(0L);
            List<Country> countries = selectLoader.list();

            List<ReferenceItemDto> items = new ArrayList<>();
            for (Country country : countries) {
                items.add(new ReferenceItemDto(
                        country.getId().toString(),
                        country.getAlpha3Code() != null ? country.getAlpha3Code() : country.getCountryShortName(),
                        country.getCountryRuName(),
                        null,
                        true,
                        country.getCountryEngName()
                ));
            }

            return ReferenceListResponseDto.ok(items, total != null ? total.intValue() : items.size(), correlationId);
        } catch (Exception e) {
            log.error("Failed to fetch countries for external API [correlationId={}]", correlationId, e);
            return ReferenceListResponseDto.error("Ошибка при получении списка стран. Correlation ID: " + correlationId, correlationId, "SYSTEM_ERROR");
        }
    }

    private String validatePagination(ReferenceFilterDto filter) {
        if (filter == null) {
            return null;
        }
        if (filter.getLimit() != null && filter.getLimit() <= 0) {
            return "Некорректный параметр limit: значение должно быть больше 0";
        }
        if (filter.getOffset() != null && filter.getOffset() < 0) {
            return "Некорректный параметр offset: значение не может быть отрицательным";
        }
        if (filter.getOffset() != null && filter.getOffset() > MAX_OFFSET) {
            return "Некорректный параметр offset: превышено максимальное допустимое смещение (" + MAX_OFFSET + ")";
        }
        return null;
    }

    private String escapeLikePattern(String value) {
        return value.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private int resolveLimit(ReferenceFilterDto filter) {
        if (filter == null || filter.getLimit() == null) {
            return DEFAULT_LIMIT;
        }
        return Math.min(filter.getLimit(), MAX_LIMIT);
    }

    private int resolveOffset(ReferenceFilterDto filter) {
        if (filter == null || filter.getOffset() == null) {
            return 0;
        }
        return filter.getOffset();
    }
}
