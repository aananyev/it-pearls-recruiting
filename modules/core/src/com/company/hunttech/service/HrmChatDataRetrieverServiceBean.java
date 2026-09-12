package com.company.hunttech.service;

import com.company.hunttech.dto.HrmDataContextSnapshot;
import com.company.hunttech.entity.CandidateCV;
import com.company.hunttech.entity.CandidateSkill;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.entity.OpenPosition;
import com.company.hunttech.entity.IteractionList;
import com.company.hunttech.entity.SkillTree;
import com.haulmont.cuba.core.entity.KeyValueEntity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.Security;
import com.haulmont.cuba.security.entity.EntityOp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Реализация сервиса извлечения среза данных HRM HuntTech в строгом режиме чтения (READ-ONLY).
 */
@Service(HrmChatDataRetrieverService.NAME)
public class HrmChatDataRetrieverServiceBean implements HrmChatDataRetrieverService {
    private static final Logger log = LoggerFactory.getLogger(HrmChatDataRetrieverServiceBean.class);

    private static final int DEFAULT_MAX_ENTITIES = 5;
    private static final int MAX_ENTITIES_HARD_CAP = 10;
    private static final int MAX_CONTEXT_LENGTH = 3500;

    private static final Pattern NON_WORD_CHARS = Pattern.compile("[^a-zA-Zа-яА-ЯёЁ0-9_#+\\-\\s]");
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "и", "в", "во", "не", "что", "он", "на", "я", "с", "со", "как", "а", "то", "все",
            "она", "так", "его", "но", "да", "ты", "к", "у", "же", "вы", "за", "бы", "по",
            "только", "ее", "мне", "было", "вот", "от", "меня", "еще", "нет", "о", "из", "ему",
            "теперь", "когда", "даже", "ну", "вдруг", "ли", "если", "уже", "или", "ни", "быть",
            "был", "него", "до", "вас", "нибудь", "опять", "уж", "вам", "ведь", "там", "потом",
            "себя", "ничего", "ей", "может", "они", "тут", "где", "есть", "надо", "ней", "для",
            "мы", "тебя", "их", "чем", "была", "сам", "чтоб", "без", "будто", "чего", "раз",
            "тоже", "себе", "под", "будет", "ж", "тогда", "кто", "этот", "того", "потому",
            "этого", "какой", "совсем", "ним", "здесь", "этом", "один", "почти", "мой", "тем",
            "чтобы", "нее", "сейчас", "были", "куда", "зачем", "всех", "никогда", "можно",
            "при", "наконец", "два", "об", "другой", "хоть", "после", "над", "больше", "тот",
            "через", "эти", "нас", "про", "всего", "них", "какая", "много", "разве", "три",
            "эту", "моя", "впрочем", "хорошо", "свою", "этой", "перед", "иногда", "лучше", "чуть",
            "том", "нельзя", "такой", "им", "более", "всегда", "кстати", "очень", "сделать",
            "пожалуйста", "подскажи", "скажи", "найди", "покажи", "объясни", "расскажи"
    ));

    @Inject
    private DataManager dataManager;
    @Inject
    private Security security;
    @Inject
    private Metadata metadata;

    @Override
    public HrmDataContextSnapshot retrieveContextForMessage(String userMessage) {
        return retrieveContextForMessage(userMessage, DEFAULT_MAX_ENTITIES);
    }

    @Override
    public HrmDataContextSnapshot retrieveContextForMessage(String userMessage, int maxEntitiesPerType) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return HrmDataContextSnapshot.empty();
        }
        int effectiveLimit = Math.min(Math.max(1, maxEntitiesPerType), MAX_ENTITIES_HARD_CAP);
        List<String> keywords = extractKeywords(userMessage);

        if (keywords.isEmpty() && !looksLikeGeneralHrmInquiry(userMessage)) {
            return HrmDataContextSnapshot.empty();
        }

        try {
            List<OpenPosition> vacancies = searchVacancies(keywords, effectiveLimit);
            List<JobCandidate> candidates = searchCandidates(keywords, effectiveLimit);
            List<IteractionList> interactions = searchInteractions(keywords, candidates, vacancies, effectiveLimit);
            List<CandidateCV> resumes = searchResumes(keywords, candidates, effectiveLimit);

            if (vacancies.isEmpty() && candidates.isEmpty() && interactions.isEmpty() && resumes.isEmpty()) {
                return HrmDataContextSnapshot.empty();
            }

            int totalCount = vacancies.size() + candidates.size() + interactions.size() + resumes.size();
            String formattedContext = formatSnapshotMarkdown(vacancies, candidates, interactions, resumes);

            return new HrmDataContextSnapshot(
                    formattedContext,
                    totalCount,
                    !vacancies.isEmpty(),
                    !candidates.isEmpty(),
                    !interactions.isEmpty(),
                    !resumes.isEmpty()
            );
        } catch (Exception e) {
            log.warn("Не удалось извлечь контекст данных HRM для чата: {}", e.getMessage(), e);
            return HrmDataContextSnapshot.empty();
        }
    }

    private boolean looksLikeGeneralHrmInquiry(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("ваканси")
                || lower.contains("кандидат")
                || lower.contains("резюме")
                || lower.contains("взаимодейств")
                || lower.contains("собеседован")
                || lower.contains("интервью")
                || lower.contains("оффер");
    }

    private List<String> extractKeywords(String text) {
        String clean = NON_WORD_CHARS.matcher(text).replaceAll(" ");
        String[] tokens = clean.split("\\s+");
        return Arrays.stream(tokens)
                .map(String::trim)
                .filter(t -> t.length() >= 2)
                .map(t -> t.toLowerCase(Locale.ROOT))
                .filter(t -> !STOP_WORDS.contains(t))
                .distinct()
                .limit(6)
                .collect(Collectors.toList());
    }

    private List<OpenPosition> searchVacancies(List<String> keywords, int limit) {
        if (!security.isEntityOpPermitted(metadata.getClassNN(OpenPosition.class), EntityOp.READ)) {
            return Collections.emptyList();
        }

        StringBuilder jpql = new StringBuilder("select e from hunttech_OpenPosition e where e.deleteTs is null ");
        Map<String, Object> params = new HashMap<>();

        if (!keywords.isEmpty()) {
            jpql.append("and (");
            for (int i = 0; i < keywords.size(); i++) {
                String paramName = "k" + i;
                if (i > 0) {
                    jpql.append(" or ");
                }
                jpql.append("(lower(e.vacansyName) like :").append(paramName)
                        .append(" or lower(e.shortDescription) like :").append(paramName)
                        .append(" or lower(e.cityPosition.cityRuName) like :").append(paramName)
                        .append(" or lower(e.projectName.projectName) like :").append(paramName)
                        .append(" or lower(e.positionType.positionRuName) like :").append(paramName)
                        .append(" or lower(e.positionType.positionEnName) like :").append(paramName)
                        .append(" or exists (select s from e.skillsList s where s.deleteTs is null and lower(s.skillName) like :").append(paramName).append(")")
                        .append(")");
                params.put(paramName, "%" + keywords.get(i) + "%");
            }
            jpql.append(") ");
        }

        jpql.append("order by e.openClose desc, e.lastOpenDate desc, e.createTs desc");

        com.haulmont.cuba.core.global.FluentLoader.ByQuery<OpenPosition, UUID> loader = dataManager.load(OpenPosition.class)
                .query(jpql.toString())
                .view("openPosition-llm-view")
                .maxResults(limit);
        params.forEach(loader::parameter);

        return loader.list();
    }

    private List<JobCandidate> searchCandidates(List<String> keywords, int limit) {
        if (!security.isEntityOpPermitted(metadata.getClassNN(JobCandidate.class), EntityOp.READ)) {
            return Collections.emptyList();
        }

        StringBuilder jpql = new StringBuilder("select e from hunttech_JobCandidate e where e.deleteTs is null ");
        Map<String, Object> params = new HashMap<>();

        if (!keywords.isEmpty()) {
            jpql.append("and (");
            for (int i = 0; i < keywords.size(); i++) {
                String paramName = "ck" + i;
                if (i > 0) {
                    jpql.append(" or ");
                }
                jpql.append("(lower(e.fullName) like :").append(paramName)
                        .append(" or lower(e.secondName) like :").append(paramName)
                        .append(" or lower(e.firstName) like :").append(paramName)
                        .append(" or lower(e.middleName) like :").append(paramName)
                        .append(" or lower(e.personPosition.positionRuName) like :").append(paramName)
                        .append(" or lower(e.personPosition.positionEnName) like :").append(paramName)
                        .append(" or lower(e.cityOfResidence.cityRuName) like :").append(paramName)
                        .append(" or lower(e.currentCompany.comanyName) like :").append(paramName)
                        .append(" or lower(e.skillTree.skillName) like :").append(paramName)
                        .append(" or exists (select cs from hunttech_CandidateSkill cs where cs.candidate = e and cs.deleteTs is null and lower(cs.skill.skillName) like :").append(paramName).append(")")
                        .append(")");
                params.put(paramName, "%" + keywords.get(i) + "%");
            }
            jpql.append(") ");
        }

        jpql.append("order by e.createTs desc");

        com.haulmont.cuba.core.global.FluentLoader.ByQuery<JobCandidate, UUID> loader = dataManager.load(JobCandidate.class)
                .query(jpql.toString())
                .view("jobCandidate-llm-view")
                .maxResults(limit);
        params.forEach(loader::parameter);

        return loader.list();
    }

    private List<IteractionList> searchInteractions(List<String> keywords,
                                                    List<JobCandidate> candidates,
                                                    List<OpenPosition> vacancies,
                                                    int limit) {
        if (!security.isEntityOpPermitted(metadata.getClassNN(IteractionList.class), EntityOp.READ)) {
            return Collections.emptyList();
        }

        StringBuilder jpql = new StringBuilder("select e from hunttech_IteractionList e where e.deleteTs is null ");
        Map<String, Object> params = new HashMap<>();

        List<String> conditions = new ArrayList<>();
        if (candidates != null && !candidates.isEmpty()) {
            conditions.add("e.candidate in :candidates");
            params.put("candidates", candidates);
        }
        if (vacancies != null && !vacancies.isEmpty()) {
            conditions.add("e.vacancy in :vacancies");
            params.put("vacancies", vacancies);
        }
        if (!keywords.isEmpty()) {
            StringBuilder kwClause = new StringBuilder("(");
            for (int i = 0; i < keywords.size(); i++) {
                String paramName = "ik" + i;
                if (i > 0) {
                    kwClause.append(" or ");
                }
                kwClause.append("(lower(e.comment) like :").append(paramName)
                        .append(" or lower(e.candidate.fullName) like :").append(paramName)
                        .append(" or lower(e.vacancy.vacansyName) like :").append(paramName)
                        .append(" or lower(e.iteractionType.iterationName) like :").append(paramName).append(")");
                params.put(paramName, "%" + keywords.get(i) + "%");
            }
            kwClause.append(")");
            conditions.add(kwClause.toString());
        }

        if (conditions.isEmpty()) {
            return Collections.emptyList();
        }

        jpql.append("and (").append(String.join(" or ", conditions)).append(") ");
        jpql.append("order by e.dateIteraction desc, e.createTs desc");

        com.haulmont.cuba.core.global.FluentLoader.ByQuery<IteractionList, UUID> loader = dataManager.load(IteractionList.class)
                .query(jpql.toString())
                .view("iteractionList-llm-view")
                .maxResults(limit);
        params.forEach(loader::parameter);

        return loader.list();
    }

    private List<CandidateCV> searchResumes(List<String> keywords, List<JobCandidate> candidates, int limit) {
        if (!security.isEntityOpPermitted(metadata.getClassNN(CandidateCV.class), EntityOp.READ)) {
            return Collections.emptyList();
        }

        StringBuilder jpql = new StringBuilder("select e from hunttech_CandidateCV e where e.deleteTs is null ");
        Map<String, Object> params = new HashMap<>();

        if (candidates != null && !candidates.isEmpty()) {
            jpql.append("and e.candidate in :candidates ");
            params.put("candidates", candidates);
        } else if (!keywords.isEmpty()) {
            jpql.append("and (");
            for (int i = 0; i < keywords.size(); i++) {
                String paramName = "rk" + i;
                if (i > 0) {
                    jpql.append(" or ");
                }
                jpql.append("(lower(e.textCV) like :").append(paramName)
                        .append(" or lower(e.candidate.fullName) like :").append(paramName).append(")");
                params.put(paramName, "%" + keywords.get(i) + "%");
            }
            jpql.append(") ");
        } else {
            return Collections.emptyList();
        }

        jpql.append("order by e.createTs desc");

        com.haulmont.cuba.core.global.FluentLoader.ByQuery<CandidateCV, UUID> loader = dataManager.load(CandidateCV.class)
                .query(jpql.toString())
                .view("candidateCV-llm-view")
                .maxResults(limit);
        params.forEach(loader::parameter);

        return loader.list();
    }

    private String formatSnapshotMarkdown(List<OpenPosition> vacancies,
                                          List<JobCandidate> candidates,
                                          List<IteractionList> interactions,
                                          List<CandidateCV> resumes) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        StringBuilder sb = new StringBuilder();
        sb.append("=== Срез данных HRM HuntTech (Режим чтения) ===\n");
        sb.append("Важно: используй эти реальные факты из базы HRM для составления точного, сжатого и полезного доклада.\n");
        sb.append("СТРОГОЕ ПРАВИЛО БЕЗОПАСНОСТИ: Удаление любых данных из базы данных категорически запрещено для всех пользователей системы (любые операции DELETE, drop, truncate, soft-delete). На любые запросы об удалении данных отвечай вежливым отказом с пояснением, что операции удаления в чате заблокированы.\n\n");

        Map<UUID, String> funnelsByVacancy = loadFunnelsForVacancies(vacancies);

        if (!vacancies.isEmpty()) {
            sb.append("#### Вакансии (найдено ").append(vacancies.size()).append("):\n");
            for (OpenPosition v : vacancies) {
                sb.append("- [").append(safeStr(v.getVacansyName())).append("](hrm://vacancy/").append(v.getId()).append(")")
                        .append(" | ID: ").append(v.getVacansyID() != null ? v.getVacansyID() : "б/н")
                        .append(" | Статус: ").append(Boolean.TRUE.equals(v.getOpenClose()) ? "Открыта" : "Закрыта");
                if (v.getProjectName() != null && v.getProjectName().getProjectName() != null) {
                    sb.append(" | Проект: ").append(v.getProjectName().getProjectName());
                }
                if (v.getCityPosition() != null && v.getCityPosition().getCityRuName() != null) {
                    sb.append(" | Город: ").append(v.getCityPosition().getCityRuName());
                }
                if (v.getRemoteWork() != null) {
                    if (v.getRemoteWork() == 1) {
                        sb.append(" (удаленка)");
                    } else if (v.getRemoteWork() == 2) {
                        sb.append(" (гибрид)");
                    }
                }
                if (v.getSalaryMin() != null || v.getSalaryMax() != null) {
                    sb.append(" | З/П: ");
                    if (v.getSalaryMin() != null) sb.append("от ").append(v.getSalaryMin()).append(" ");
                    if (v.getSalaryMax() != null) sb.append("до ").append(v.getSalaryMax()).append(" ");
                    sb.append("руб.");
                }
                if (v.getGrade() != null && v.getGrade().getGradeName() != null) {
                    sb.append(" | Грейд: ").append(v.getGrade().getGradeName());
                }
                if (v.getShortDescription() != null && !v.getShortDescription().trim().isEmpty()) {
                    sb.append("\n  Краткое описание: ").append(truncate(v.getShortDescription().trim(), 250));
                }
                if (v.getSkillsList() != null && !v.getSkillsList().isEmpty()) {
                    List<String> vSkills = new ArrayList<>();
                    for (SkillTree st : v.getSkillsList()) {
                        if (st != null && st.getSkillName() != null && !st.getSkillName().trim().isEmpty()) {
                            vSkills.add(st.getSkillName().trim());
                        }
                    }
                    if (!vSkills.isEmpty()) {
                        sb.append(" | Требуемые навыки: ").append(String.join(", ", vSkills.subList(0, Math.min(vSkills.size(), 6))));
                    }
                }
                String funnel = funnelsByVacancy.get(v.getId());
                if (funnel != null) {
                    sb.append("\n  ").append(funnel);
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        if (!candidates.isEmpty()) {
            sb.append("#### Кандидаты (найдено ").append(candidates.size()).append("):\n");
            for (JobCandidate c : candidates) {
                sb.append("- [").append(safeStr(c.getFullName())).append("](hrm://candidate/").append(c.getId()).append(")");
                if (c.getPersonPosition() != null) {
                    String pos = c.getPersonPosition().getPositionRuName() != null
                            ? c.getPersonPosition().getPositionRuName()
                            : c.getPersonPosition().getPositionEnName();
                    if (pos != null) sb.append(" | Должность: ").append(pos);
                }
                if (c.getCityOfResidence() != null && c.getCityOfResidence().getCityRuName() != null) {
                    sb.append(" | Город: ").append(c.getCityOfResidence().getCityRuName());
                }
                if (c.getCurrentCompany() != null && c.getCurrentCompany().getComanyName() != null) {
                    sb.append(" | Тек. компания: ").append(c.getCurrentCompany().getComanyName());
                }
                List<String> skills = new ArrayList<>();
                if (c.getSkillTree() != null && c.getSkillTree().getSkillName() != null) {
                    skills.add(c.getSkillTree().getSkillName().trim() + " (основной)");
                }
                if (c.getCandidateSkills() != null) {
                    for (CandidateSkill cs : c.getCandidateSkills()) {
                        if (cs != null && cs.getSkill() != null && cs.getSkill().getSkillName() != null) {
                            String sName = cs.getSkill().getSkillName().trim();
                            if (!skills.contains(sName) && !skills.contains(sName + " (основной)")) {
                                skills.add(sName);
                            }
                        }
                    }
                }
                if (!skills.isEmpty()) {
                    sb.append(" | Навыки: ").append(String.join(", ", skills.subList(0, Math.min(skills.size(), 8))));
                }
                if (c.getEmail() != null && !c.getEmail().trim().isEmpty()) {
                    sb.append(" | Email: ").append(c.getEmail().trim());
                }
                if (c.getPhone() != null && !c.getPhone().trim().isEmpty()) {
                    sb.append(" | Тел: ").append(c.getPhone().trim());
                } else if (c.getMobilePhone() != null && !c.getMobilePhone().trim().isEmpty()) {
                    sb.append(" | Тел: ").append(c.getMobilePhone().trim());
                }
                if (c.getTelegramName() != null && !c.getTelegramName().trim().isEmpty()) {
                    sb.append(" | TG: @").append(c.getTelegramName().trim().replace("@", ""));
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        if (!interactions.isEmpty()) {
            sb.append("#### Взаимодействия и этапы (найдено ").append(interactions.size()).append("):\n");
            for (IteractionList it : interactions) {
                String dateStr = it.getDateIteraction() != null ? dateFormat.format(it.getDateIteraction()) : "б/д";
                String typeStr = (it.getIteractionType() != null && it.getIteractionType().getIterationName() != null)
                        ? it.getIteractionType().getIterationName().trim() : "Событие";
                String candStr = (it.getCandidate() != null && it.getCandidate().getFullName() != null)
                        ? it.getCandidate().getFullName().trim() : "Кандидат";
                String vacStr = (it.getVacancy() != null && it.getVacancy().getVacansyName() != null)
                        ? it.getVacancy().getVacansyName().trim() : null;

                sb.append("- ").append(dateStr).append(" | [").append(typeStr).append("](hrm://interaction/").append(it.getId()).append(")")
                        .append(" | ").append(candStr);
                if (vacStr != null) {
                    sb.append(" (по вакансии: ").append(vacStr).append(")");
                }
                if (it.getRating() != null) {
                    sb.append(" | Оценка: ").append(it.getRating() + 1).append("/5");
                }
                if (it.getComment() != null && !it.getComment().trim().isEmpty()) {
                    sb.append("\n  Комментарий: ").append(truncate(it.getComment().trim(), 200));
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        List<CandidateCV> validResumes = new ArrayList<>();
        for (CandidateCV cv : resumes) {
            if (cv.getTextCV() != null && !cv.getTextCV().trim().isEmpty()) {
                validResumes.add(cv);
            }
        }
        if (!validResumes.isEmpty()) {
            sb.append("#### Резюме и опыт кандидатов:\n");
            for (CandidateCV cv : validResumes) {
                String candName = (cv.getCandidate() != null && cv.getCandidate().getFullName() != null)
                        ? cv.getCandidate().getFullName().trim() : "Кандидат";
                sb.append("- **Резюме: ").append(candName).append("**\n")
                        .append("  Выдержка: ").append(truncate(cleanCvText(cv.getTextCV().trim()), 350))
                        .append("\n");
            }
            sb.append("\n");
        }

        if (sb.length() > MAX_CONTEXT_LENGTH) {
            int cut = MAX_CONTEXT_LENGTH;
            if (Character.isHighSurrogate(sb.charAt(cut - 1))) {
                cut--;
            }
            return sb.substring(0, cut) + "\n...[часть контекста усечена для соблюдения квоты]...";
        }
        return sb.toString();
    }

    private String safeStr(String str) {
        return str != null ? str.trim() : "Без названия";
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        if (text.length() <= max) return text;
        int cut = max;
        if (Character.isHighSurrogate(text.charAt(cut - 1))) {
            cut--;
        }
        return text.substring(0, cut) + "...";
    }

    private String cleanCvText(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("\\r?\\n+", " ").replaceAll("\\s{2,}", " ");
    }

    private Map<UUID, String> loadFunnelsForVacancies(List<OpenPosition> vacancies) {
        if (vacancies == null || vacancies.isEmpty()
                || !security.isEntityOpPermitted(metadata.getClassNN(IteractionList.class), EntityOp.READ)) {
            return Collections.emptyMap();
        }
        try {
            List<KeyValueEntity> rows = dataManager.loadValues(
                    "select e.vacancy.id, e.iteractionType.iterationName, count(e) from hunttech_IteractionList e " +
                    "where e.vacancy in :vacancies and e.deleteTs is null " +
                    "group by e.vacancy.id, e.iteractionType.iterationName " +
                    "order by e.vacancy.id, count(e) desc")
                    .parameter("vacancies", vacancies)
                    .properties("vacancyId", "stageName", "stageCount")
                    .list();
            if (rows.isEmpty()) {
                return Collections.emptyMap();
            }

            Map<UUID, List<String>> stagePartsByVac = new LinkedHashMap<>();
            Map<UUID, Long> totalByVac = new HashMap<>();

            for (KeyValueEntity row : rows) {
                UUID vacId = row.getValue("vacancyId");
                String stage = row.getValue("stageName");
                Long count = row.getValue("stageCount");
                if (vacId != null && count != null) {
                    totalByVac.put(vacId, totalByVac.getOrDefault(vacId, 0L) + count);
                    List<String> list = stagePartsByVac.computeIfAbsent(vacId, k -> new ArrayList<>());
                    if (list.size() < 5) {
                        list.add((stage != null ? stage : "Этап") + ": " + count);
                    }
                }
            }

            Map<UUID, String> result = new HashMap<>();
            for (Map.Entry<UUID, List<String>> entry : stagePartsByVac.entrySet()) {
                UUID vacId = entry.getKey();
                long total = totalByVac.getOrDefault(vacId, 0L);
                result.put(vacId, "Воронка (всего " + total + "): " + String.join(", ", entry.getValue()));
            }
            return result;
        } catch (Exception e) {
            log.warn("Не удалось рассчитать пакетную воронку по вакансиям", e);
            return Collections.emptyMap();
        }
    }
}
