package com.company.hunttech.web.screens.llmchat;

import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.DataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Резолвер человекочитаемых наименований сущностей HRM (вакансии, кандидаты, резюме, компании)
 * по их техническим идентификаторам (UUID) с in-memory кэшированием.
 */
public class HrmEntityNameResolver {
    private static final Logger log = LoggerFactory.getLogger(HrmEntityNameResolver.class);

    private static final long NEGATIVE_CACHE_TTL_MS = 60_000L; // 60 секунд TTL для ненайденных сущностей

    public static class EntityInfo {
        private final UUID id;
        private final String entityType; // vacancy, candidate, company, cv, interaction
        private final String displayName; // "Вакансия Middle System Analyst / Системный аналитик"
        private final String cubaScreen;  // hunttech_OpenPosition.edit
        private final String icon;        // 💼, 👤, etc.

        public EntityInfo(UUID id, String entityType, String displayName, String cubaScreen, String icon) {
            this.id = id;
            this.entityType = entityType;
            this.displayName = displayName;
            this.cubaScreen = cubaScreen;
            this.icon = icon;
        }

        public UUID getId() {
            return id;
        }

        public String getEntityType() {
            return entityType;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getCubaScreen() {
            return cubaScreen;
        }

        public String getIcon() {
            return icon;
        }

        public String getCubaUrl() {
            return "#main/0/" + cubaScreen + (id != null ? "?id=" + id : "");
        }

        public String getHrmUrl() {
            return "hrm://" + entityType + "/" + (id != null ? id : "");
        }
    }

    private static final Map<UUID, EntityInfo> CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> NOT_FOUND_CACHE = new ConcurrentHashMap<>();

    public static void registerEntity(UUID id, String entityType, String displayName, String cubaScreen, String icon) {
        if (id != null && displayName != null) {
            CACHE.put(id, new EntityInfo(id, entityType, displayName, cubaScreen, icon));
            NOT_FOUND_CACHE.remove(id);
        }
    }

    public static void clearCache() {
        CACHE.clear();
        NOT_FOUND_CACHE.clear();
    }

    public static EntityInfo resolve(String idStr) {
        return resolve(null, idStr);
    }

    public static EntityInfo resolve(String entityTypeHint, String idStr) {
        if (idStr == null || idStr.trim().isEmpty()) {
            return null;
        }
        try {
            return resolve(entityTypeHint, UUID.fromString(idStr.trim()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static EntityInfo resolve(UUID id) {
        return resolve(null, id);
    }

    public static EntityInfo resolve(String entityTypeHint, UUID id) {
        if (id == null) {
            return null;
        }
        EntityInfo cached = CACHE.get(id);
        if (cached != null) {
            return cached;
        }

        Long notFoundTime = NOT_FOUND_CACHE.get(id);
        if (notFoundTime != null) {
            if (System.currentTimeMillis() - notFoundTime < NEGATIVE_CACHE_TTL_MS) {
                return null;
            }
            NOT_FOUND_CACHE.remove(id);
        }

        DataManager dataManager = getDataManager();
        if (dataManager == null) {
            return null;
        }

        // Атомарное вычисление для исключения дублирующих параллельных запросов в БД
        return CACHE.computeIfAbsent(id, key -> {
            try {
                EntityInfo info = queryEntity(dataManager, entityTypeHint, key);
                if (info != null) {
                    return info;
                } else {
                    NOT_FOUND_CACHE.put(key, System.currentTimeMillis());
                    return null;
                }
            } catch (Exception e) {
                log.debug("Не удалось разрешить имя сущности для id={}: {}", key, e.getMessage());
                return null;
            }
        });
    }

    private static DataManager getDataManager() {
        try {
            if (AppBeans.containsBean(DataManager.NAME)) {
                return AppBeans.get(DataManager.class);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static EntityInfo queryEntity(DataManager dm, String typeHint, UUID id) {
        // 1. Вакансия
        if (typeHint == null || "vacancy".equalsIgnoreCase(typeHint) || "hunttech_OpenPosition".equalsIgnoreCase(typeHint)) {
            try {
                String name = dm.loadValue("select e.vacansyName from hunttech_OpenPosition e where e.id = :id and e.deleteTs is null", String.class)
                        .parameter("id", id)
                        .optional().orElse(null);
                if (name != null && !name.trim().isEmpty()) {
                    String clean = formatVacancyName(name);
                    return new EntityInfo(id, "vacancy", clean, "hunttech_OpenPosition.edit", "💼");
                }
            } catch (Exception e) {
                log.debug("Ошибка запроса hunttech_OpenPosition для id={}: {}", id, e.getMessage());
            }
        }

        // 2. Кандидат
        if (typeHint == null || "candidate".equalsIgnoreCase(typeHint) || "hunttech_JobCandidate".equalsIgnoreCase(typeHint)) {
            try {
                String name = dm.loadValue("select e.fullName from hunttech_JobCandidate e where e.id = :id and e.deleteTs is null", String.class)
                        .parameter("id", id)
                        .optional().orElse(null);
                if (name != null && !name.trim().isEmpty()) {
                    String clean = formatCandidateName(name);
                    return new EntityInfo(id, "candidate", clean, "hunttech_JobCandidate.edit", "👤");
                }
            } catch (Exception e) {
                log.debug("Ошибка запроса hunttech_JobCandidate для id={}: {}", id, e.getMessage());
            }
        }

        // 3. Резюме
        if (typeHint == null || "cv".equalsIgnoreCase(typeHint) || "hunttech_CandidateCV".equalsIgnoreCase(typeHint)) {
            try {
                String candName = dm.loadValue("select e.candidate.fullName from hunttech_CandidateCV e where e.id = :id and e.deleteTs is null", String.class)
                        .parameter("id", id)
                        .optional().orElse(null);
                if (candName != null && !candName.trim().isEmpty()) {
                    return new EntityInfo(id, "cv", "Резюме кандидата " + candName.trim(), "hunttech_CandidateCV.edit", "📄");
                }
            } catch (Exception e) {
                log.debug("Ошибка запроса hunttech_CandidateCV для id={}: {}", id, e.getMessage());
            }
        }

        // 4. Взаимодействие
        if (typeHint == null || "interaction".equalsIgnoreCase(typeHint) || "hunttech_IteractionList".equalsIgnoreCase(typeHint)) {
            try {
                String itName = dm.loadValue("select e.iteractionType.iterationName from hunttech_IteractionList e where e.id = :id and e.deleteTs is null", String.class)
                        .parameter("id", id)
                        .optional().orElse(null);
                if (itName != null && !itName.trim().isEmpty()) {
                    return new EntityInfo(id, "interaction", "Взаимодействие: " + itName.trim(), "hunttech_IteractionList.edit", "📋");
                }
            } catch (Exception e) {
                log.debug("Ошибка запроса hunttech_IteractionList для id={}: {}", id, e.getMessage());
            }
        }

        // 5. Компания
        if (typeHint == null || "company".equalsIgnoreCase(typeHint) || "hunttech_Company".equalsIgnoreCase(typeHint)) {
            try {
                String name = dm.loadValue("select e.comanyName from hunttech_Company e where e.id = :id and e.deleteTs is null", String.class)
                        .parameter("id", id)
                        .optional().orElse(null);
                if (name != null && !name.trim().isEmpty()) {
                    String clean = formatCompanyName(name);
                    return new EntityInfo(id, "company", clean, "hunttech_Company.edit", "🏢");
                }
            } catch (Exception e) {
                log.debug("Ошибка запроса hunttech_Company для id={}: {}", id, e.getMessage());
            }
        }

        // 6. Должность / Позиция
        if (typeHint == null || "position".equalsIgnoreCase(typeHint) || "hunttech_Position".equalsIgnoreCase(typeHint)) {
            try {
                String name = dm.loadValue("select e.positionRuName from hunttech_Position e where e.id = :id and e.deleteTs is null", String.class)
                        .parameter("id", id)
                        .optional().orElse(null);
                if (name != null && !name.trim().isEmpty()) {
                    return new EntityInfo(id, "position", "Должность " + name.trim(), "hunttech_Position.edit", "🏷️");
                }
            } catch (Exception e) {
                log.debug("Ошибка запроса hunttech_Position для id={}: {}", id, e.getMessage());
            }
        }

        // 7. Компетенция / Навык
        if (typeHint == null || "skill".equalsIgnoreCase(typeHint) || "hunttech_SkillTree".equalsIgnoreCase(typeHint)) {
            try {
                String name = dm.loadValue("select e.skillName from hunttech_SkillTree e where e.id = :id and e.deleteTs is null", String.class)
                        .parameter("id", id)
                        .optional().orElse(null);
                if (name != null && !name.trim().isEmpty()) {
                    return new EntityInfo(id, "skill", "Навык " + name.trim(), "hunttech_SkillTree.edit", "💡");
                }
            } catch (Exception e) {
                log.debug("Ошибка запроса hunttech_SkillTree для id={}: {}", id, e.getMessage());
            }
        }

        // 8. Проект
        if (typeHint == null || "project".equalsIgnoreCase(typeHint) || "hunttech_Project".equalsIgnoreCase(typeHint)) {
            try {
                String name = dm.loadValue("select e.projectName from hunttech_Project e where e.id = :id and e.deleteTs is null", String.class)
                        .parameter("id", id)
                        .optional().orElse(null);
                if (name != null && !name.trim().isEmpty()) {
                    return new EntityInfo(id, "project", "Проект " + name.trim(), "hunttech_Project.edit", "📁");
                }
            } catch (Exception e) {
                log.debug("Ошибка запроса hunttech_Project для id={}: {}", id, e.getMessage());
            }
        }

        // 9. Пользователь
        if (typeHint == null || "user".equalsIgnoreCase(typeHint) || "hunttech_ExtUser".equalsIgnoreCase(typeHint) || "sec$User".equalsIgnoreCase(typeHint)) {
            try {
                String name = dm.loadValue("select e.name from hunttech_ExtUser e where e.id = :id and e.deleteTs is null", String.class)
                        .parameter("id", id)
                        .optional().orElse(null);
                if (name != null && !name.trim().isEmpty()) {
                    return new EntityInfo(id, "user", "Пользователь " + name.trim(), "hunttech_ExtUser.edit", "👤");
                }
            } catch (Exception e) {
                log.debug("Ошибка запроса hunttech_ExtUser для id={}: {}", id, e.getMessage());
            }
        }

        // 10. Город
        if (typeHint == null || "city".equalsIgnoreCase(typeHint) || "hunttech_City".equalsIgnoreCase(typeHint)) {
            try {
                String name = dm.loadValue("select e.cityName from hunttech_City e where e.id = :id and e.deleteTs is null", String.class)
                        .parameter("id", id)
                        .optional().orElse(null);
                if (name != null && !name.trim().isEmpty()) {
                    return new EntityInfo(id, "city", "Город " + name.trim(), "hunttech_City.edit", "📍");
                }
            } catch (Exception e) {
                log.debug("Ошибка запроса hunttech_City для id={}: {}", id, e.getMessage());
            }
        }

        return null;
    }

    public static String formatVacancyName(String raw) {
        if (raw == null) return "Вакансия";
        String clean = raw.trim();
        int idx = clean.indexOf(" (");
        if (idx > 0) {
            clean = clean.substring(0, idx).trim();
        }
        if (!clean.toLowerCase().startsWith("вакансия")) {
            clean = "Вакансия " + clean;
        }
        return clean;
    }

    public static String formatCandidateName(String raw) {
        if (raw == null) return "Кандидат";
        String clean = raw.trim();
        if (!clean.toLowerCase().startsWith("кандидат")) {
            clean = "Кандидат " + clean;
        }
        return clean;
    }

    public static String formatCompanyName(String raw) {
        if (raw == null) return "Компания";
        String clean = raw.trim();
        if (!clean.toLowerCase().startsWith("компания")) {
            clean = "Компания " + clean;
        }
        return clean;
    }
}
