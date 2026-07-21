package com.company.hunttech.core.ai;

import com.company.hunttech.entity.*;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.MetadataTools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Реестр экстракторов данных для заполнения {{placeholders}} в промптах.
 * Ключ: "EntityClass.placeholderName" → функция извлечения.
 *
 * Использует @Autowired + Spring @PostConstruct для гарантии инжекта
 * до вызова init() — javax @PostConstruct в CUBA 7.3 срабатывает раньше @Inject.
 */
@Component("hunttech_EntityDataExtractors")
public class EntityDataExtractors {

    @Autowired
    private DataManager dataManager;

    @Autowired
    private MetadataTools metadataTools;

    private final Map<String, EntityDataExtractor> registry = new HashMap<>();

    @PostConstruct
    public void init() {
        // ── CandidateCV ──
        reg("CandidateCV", "resumeText", e -> ((CandidateCV) e).getTextCV());
        reg("CandidateCV", "candidateName", e -> {
            CandidateCV cv = (CandidateCV) e;
            return cv.getCandidate() != null
                    ? metadataTools.getInstanceName(cv.getCandidate()) : "";
        });
        reg("CandidateCV", "vacancyDescription", e -> {
            CandidateCV cv = (CandidateCV) e;
            return cv.getToVacancy() != null && cv.getToVacancy().getShortDescription() != null
                    ? cv.getToVacancy().getShortDescription() : "";
        });
        reg("CandidateCV", "positionName", e -> {
            CandidateCV cv = (CandidateCV) e;
            return cv.getToVacancy() != null ? cv.getToVacancy().getVacansyName() : "";
        });

        // ── OpenPosition ──
        reg("OpenPosition", "vacancyDescription",
                e -> nonNull(((OpenPosition) e).getShortDescription()));
        reg("OpenPosition", "vacancyRequirements",
                e -> nonNull(((OpenPosition) e).getComment()));
        reg("OpenPosition", "companyName", e -> {
            OpenPosition op = (OpenPosition) e;
            if (op.getProjectName() == null) return "";
            CompanyDepartament dept = op.getProjectName().getProjectDepartment();
            if (dept == null || dept.getCompanyName() == null) return "";
            return dept.getCompanyName().getComanyName();
        });
        reg("OpenPosition", "projectName", e -> {
            OpenPosition op = (OpenPosition) e;
            return op.getProjectName() != null ? op.getProjectName().getProjectName() : "";
        });

        // ── IteractionList ──
        reg("IteractionList", "interactionType",
                e -> ((IteractionList) e).getIteractionType() != null
                        ? ((IteractionList) e).getIteractionType().getIterationName() : "");
        reg("IteractionList", "comment", e -> nonNull(((IteractionList) e).getComment()));
        reg("IteractionList", "dateIteraction",
                e -> Objects.toString(((IteractionList) e).getDateIteraction(), ""));
        reg("IteractionList", "recrutierName",
                e -> nonNull(((IteractionList) e).getRecrutierName()));
        reg("IteractionList", "candidateName", e -> {
            IteractionList il = (IteractionList) e;
            return il.getCandidate() != null
                    ? metadataTools.getInstanceName(il.getCandidate()) : "";
        });
        reg("IteractionList", "candidateHistory", e -> {
            IteractionList il = (IteractionList) e;
            if (il.getCandidate() == null) return "";
            List<IteractionList> history = dataManager.load(IteractionList.class)
                    .query("select e from hunttech_IteractionList e "
                            + "where e.candidate = :c order by e.dateIteraction desc")
                    .parameter("c", il.getCandidate())
                    .maxResults(20)
                    .view("_minimal")
                    .list();
            return history.stream()
                    .map(i -> String.format("%s | %s | %s | %s",
                            i.getDateIteraction(),
                            i.getIteractionType() != null
                                    ? i.getIteractionType().getIterationName() : "-",
                            i.getRecrutierName(),
                            nonNull(i.getComment())))
                    .collect(Collectors.joining("\n"));
        });

        // ── JobCandidate ──
        reg("JobCandidate", "fullName",
                e -> metadataTools.getInstanceName((JobCandidate) e));
        reg("JobCandidate", "email", e -> nonNull(((JobCandidate) e).getEmail()));
        reg("JobCandidate", "phone", e -> nonNull(((JobCandidate) e).getPhone()));
    }

    private void reg(String entityClass, String placeholder, EntityDataExtractor fn) {
        registry.put(entityClass + "." + placeholder, fn);
    }

    /**
     * Извлекает значение placeholder-а. Имя класса сущности очищается от
     * Hibernate-прокси-суффиксов ($$EnhancerByCGLIB$$...).
     */
    public String extract(Entity entity, String placeholder) {
        String simpleName = entity.getClass().getSimpleName().replaceAll("\\$.*", "");
        EntityDataExtractor fn = registry.get(simpleName + "." + placeholder);
        return fn != null ? fn.apply(entity) : "{{" + placeholder + "}}";
    }

    private static String nonNull(String s) {
        return s != null ? s : "";
    }
}
