package com.company.hunttech.core;

import com.company.hunttech.HunttechTestContainer;
import com.company.hunttech.TestEntityTracker;
import com.company.hunttech.entity.City;
import com.company.hunttech.entity.Grade;
import com.company.hunttech.entity.OpenPosition;
import com.company.hunttech.entity.Position;
import com.company.hunttech.entity.Project;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Transaction;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.core.global.ViewBuilder;
import com.haulmont.cuba.core.global.ViewRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Защита OpenPositionEdit от ошибок detached object (Unfetched Attribute Access /
 * LazyInitializationException / ValidationException при commit detached-сущности).
 *
 * Моделирует ровно те сценарии, которые выполняет контроллер OpenPositionEdit:
 * 1) открытие формы — сущность загружается с view {@code openPosition-edit-view};
 * 2) FK-граф (grade/positionType/cityPosition/projectName/owner) читается из view;
 * 3) LOB-поля (comment, commentEn, exercise, memoForInterview, templateLetter)
 *    отсутствуют в edit-view — контроллер догружает их через {@code reload(ViewBuilder)}
 *    (loadMainTabLobs/loadExerciseLob/loadMemoForInterviewLob/loadTemplateLetterLob);
 * 4) коллекции laborAgreement/skillsList догружаются reload + dataContext.merge
 *    (ensureLaborAgreementLoadedOnEntity/ensureSkillsListLoadedOnEntity);
 * 5) commit detached-объекта после изменения полей.
 *
 * Редизайн 2026-08-05 перемещал компоненты между визуальными контейнерами — bindings
 * не менялись, но этот тест фиксирует, что detached-контракт формы сохранён.
 */
public class OpenPositionEditDetachedObjectTest {

    @ClassRule
    public static HunttechTestContainer cont = HunttechTestContainer.Common.INSTANCE;

    private DataManager dataManager;
    private Persistence persistence;
    private TestEntityTracker tracker;

    @Before
    public void setUp() {
        dataManager = AppBeans.get(DataManager.class);
        persistence = AppBeans.get(Persistence.class);
        tracker = new TestEntityTracker(dataManager);
    }

    @After
    public void tearDown() {
        tracker.cleanup();
    }

    @Test
    public void openPositionEditViewProvidesEveryFormField() {
        OpenPosition position = createPositionWithReferences();

        // Форма открывается с detached openPosition-edit-view.
        OpenPosition detached = dataManager.load(OpenPosition.class)
                .id(position.getId())
                .view("openPosition-edit-view")
                .one();

        // Скалярные поля формы (блоки «Идентификаторы и статус», «Настройки вакансии»,
        // «Проект и локация», «Количество персонала», «Заработная плата»).
        assertEquals(position.getVacansyName(), detached.getVacansyName());
        assertEquals(position.getVacansyID(), detached.getVacansyID());
        assertEquals(position.getSignDraft(), detached.getSignDraft());
        assertEquals(position.getOpenClose(), detached.getOpenClose());
        assertEquals(position.getPriority(), detached.getPriority());
        assertEquals(position.getClosingDate(), detached.getClosingDate());
        assertEquals(position.getRemoteWork(), detached.getRemoteWork());
        assertEquals(position.getRemoteComment(), detached.getRemoteComment());
        assertEquals(position.getCommandCandidate(), detached.getCommandCandidate());
        assertEquals(position.getCommandExperience(), detached.getCommandExperience());
        assertEquals(position.getInternalProject(), detached.getInternalProject());
        assertEquals(position.getNumberPosition(), detached.getNumberPosition());
        assertEquals(position.getMore10NumberPosition(), detached.getMore10NumberPosition());
        assertEquals(position.getWorkExperience(), detached.getWorkExperience());
        assertEquals(position.getSalaryMin(), detached.getSalaryMin());
        assertEquals(position.getSalaryMax(), detached.getSalaryMax());
        assertEquals(position.getSalaryIE(), detached.getSalaryIE());
        assertEquals(position.getSalaryComment(), detached.getSalaryComment());
        assertEquals(position.getPriorityComment(), detached.getPriorityComment());
    }

    @Test
    public void fkGraphAccessibleFromEditViewWithoutLazyErrors() {
        OpenPosition position = createPositionWithReferences();

        OpenPosition detached = dataManager.load(OpenPosition.class)
                .id(position.getId())
                .view("openPosition-edit-view")
                .one();

        // FK-граф: sidebar и поля формы читают display-значения вложенных сущностей.
        assertNotNull("edit-view обязан содержать grade", detached.getGrade());
        assertEquals(position.getGrade().getGradeName(), detached.getGrade().getGradeName());

        assertNotNull("edit-view обязан содержать positionType", detached.getPositionType());
        assertEquals(position.getPositionType().getPositionRuName(),
                detached.getPositionType().getPositionRuName());

        assertNotNull("edit-view обязан содержать cityPosition", detached.getCityPosition());
        assertEquals(position.getCityPosition().getCityRuName(),
                detached.getCityPosition().getCityRuName());

        assertNotNull("edit-view обязан содержать projectName", detached.getProjectName());
        assertEquals(position.getProjectName().getProjectName(),
                detached.getProjectName().getProjectName());

        // owner может быть null (форма обрабатывает пустые связи), доступ не должен бросать.
        detached.getOwner();
    }

    @Test
    public void lazyLobFieldsAreNotInEditViewAndReloadPatternWorks() {
        OpenPosition position = createPositionWithReferences();
        String commentValue = "Комментарий RU " + UUID.randomUUID();
        String commentEnValue = "Comment EN " + UUID.randomUUID();
        String exerciseValue = "Тестовое задание " + UUID.randomUUID();
        String memoValue = "Памятка " + UUID.randomUUID();
        String letterValue = "Шаблон письма " + UUID.randomUUID();
        fillLobs(position, commentValue, commentEnValue, exerciseValue, memoValue, letterValue);
        dataManager.commit(position, "openPosition-edit-view");

        // Свежая транзакция с честным fetch plan view (вне L1-кэша DataManager).
        OpenPosition detached;
        try (Transaction tx = persistence.createTransaction()) {
            EntityManager em = persistence.getEntityManager();
            detached = em.find(OpenPosition.class, position.getId(), "openPosition-edit-view");
            tx.commit();
        }

        // Статическая проверка состава openPosition-edit-view: LOB-поля комментариев,
        // тестового задания, памятки и шаблона письма в view НЕ входят — контроллер
        // догружает их lazy через reload(ViewBuilder) (loadMainTabLobs/loadExerciseLob/
        // loadMemoForInterviewLob/loadTemplateLetterLob).
        View editView = AppBeans.get(ViewRepository.class)
                .getView(OpenPosition.class, "openPosition-edit-view");
        assertFalse("comment не должен входить в edit-view", editView.containsProperty("comment"));
        assertFalse("commentEn не должен входить в edit-view", editView.containsProperty("commentEn"));
        assertFalse("exercise не должен входить в edit-view", editView.containsProperty("exercise"));
        assertFalse("memoForInterview не должен входить в edit-view",
                editView.containsProperty("memoForInterview"));
        assertFalse("templateLetter не должен входить в edit-view",
                editView.containsProperty("templateLetter"));
        assertTrue("rawDescription обязан входить в edit-view",
                editView.containsProperty("rawDescription"));
        assertTrue("salaryMin обязан входить в edit-view",
                editView.containsProperty("salaryMin"));

        // Паттерн loadMainTabLobs(): reload(ViewBuilder) + setter обратно на edited entity.
        OpenPosition withComment = dataManager.reload(detached, ViewBuilder.of(OpenPosition.class)
                .add("comment").add("commentEn").build());
        detached.setComment(withComment.getComment());
        detached.setCommentEn(withComment.getCommentEn());
        assertEquals(commentValue, detached.getComment());
        assertEquals(commentEnValue, detached.getCommentEn());

        // Паттерн loadExerciseLob() / loadMemoForInterviewLob() / loadTemplateLetterLob().
        OpenPosition withExercise = dataManager.reload(detached, ViewBuilder.of(OpenPosition.class)
                .add("exercise").build());
        detached.setExercise(withExercise.getExercise());
        assertEquals(exerciseValue, detached.getExercise());

        OpenPosition withMemo = dataManager.reload(detached, ViewBuilder.of(OpenPosition.class)
                .add("memoForInterview").build());
        detached.setMemoForInterview(withMemo.getMemoForInterview());
        assertEquals(memoValue, detached.getMemoForInterview());

        OpenPosition withLetter = dataManager.reload(detached, ViewBuilder.of(OpenPosition.class)
                .add("templateLetter").build());
        detached.setTemplateLetter(withLetter.getTemplateLetter());
        assertEquals(letterValue, detached.getTemplateLetter());

        // После догрузки commit detached не должен давать ValidationException.
        dataManager.commit(detached);
    }

    @Test
    public void laborAgreementAndSkillsReloadMergePatternWorks() {
        OpenPosition position = createPositionWithReferences();

        OpenPosition detached = dataManager.load(OpenPosition.class)
                .id(position.getId())
                .view("openPosition-edit-view")
                .one();

        // Коллекции laborAgreement/skillsList не входят в edit-view (статическая
        // проверка состава view; контроллер догружает их reload + merge).
        View editView = AppBeans.get(ViewRepository.class)
                .getView(OpenPosition.class, "openPosition-edit-view");
        assertFalse("laborAgreement не должен входить в edit-view",
                editView.containsProperty("laborAgreement"));
        assertFalse("skillsList не должен входить в edit-view",
                editView.containsProperty("skillsList"));

        // Паттерн ensureLaborAgreementLoadedOnEntity(): reload + merge (в тесте без
        // DataContext — проверяем, что reload с tab-view выполняется без ошибок).
        OpenPosition withLabor = dataManager.reload(detached, ViewBuilder.of(OpenPosition.class)
                .add("laborAgreement", "laborAgreement-openPosition-tab-view")
                .build());
        assertNotNull("laborAgreement обязан быть загружен после reload",
                withLabor.getLaborAgreement());

        // Паттерн ensureSkillsListLoadedOnEntity().
        OpenPosition withSkills = dataManager.reload(detached, ViewBuilder.of(OpenPosition.class)
                .add("skillsList", "skillTree-openPosition-tab-view")
                .build());
        assertNotNull("skillsList обязан быть загружен после reload",
                withSkills.getSkillsList());
    }

    @Test
    public void modifyDetachedPositionAndCommit() {
        OpenPosition position = createPositionWithReferences();
        UUID id = position.getId();

        OpenPosition detached = dataManager.load(OpenPosition.class)
                .id(id)
                .view("openPosition-edit-view")
                .one();
        detached.setSalaryMin(new BigDecimal("150000"));
        detached.setSalaryMax(new BigDecimal("250000"));
        detached.setPriorityComment("Обновлено в detached-тесте " + UUID.randomUUID());
        dataManager.commit(detached);

        OpenPosition reloaded = dataManager.load(OpenPosition.class)
                .id(id)
                .view("openPosition-edit-view")
                .one();
        assertEquals(new BigDecimal("150000"), reloaded.getSalaryMin());
        assertEquals(new BigDecimal("250000"), reloaded.getSalaryMax());
        assertEquals(detached.getPriorityComment(), reloaded.getPriorityComment());
    }

    @Test
    public void emptyReferencesAreHandledGracefully() {
        // Новая вакансия (create): FK пустые — sidebar/поля читают null-связи.
        OpenPosition position = dataManager.create(OpenPosition.class);
        position.setVacansyName("Детached-тест без FK " + UUID.randomUUID());
        position.setRemoteWork(0);
        position.setCommandCandidate(0);
        position.setWorkExperience(0);
        position.setInternalProject(false);
        position.setVacansyID("DT-" + Math.abs(UUID.randomUUID().hashCode() % 900000));

        // projectName — @NotNull (optional=false): пустые связи допустимы для остальных FK.
        Project project = dataManager.create(Project.class);
        project.setProjectName("Project-empty-" + UUID.randomUUID());
        tracker.track(dataManager.commit(project));
        position.setProjectName(project);

        tracker.track(dataManager.commit(position, "openPosition-edit-view"));

        OpenPosition detached = dataManager.load(OpenPosition.class)
                .id(position.getId())
                .view("openPosition-edit-view")
                .one();

        // Доступ к пустым FK и коллекциям не должен бросать.
        detached.getGrade();
        detached.getPositionType();
        detached.getCityPosition();
        detached.getProjectName();
        detached.getParentOpenPosition();
        detached.getOwner();
        assertTrue("vacansyName сохранён", detached.getVacansyName().startsWith("Детached"));
    }

    private OpenPosition createPositionWithReferences() {
        Grade grade = dataManager.create(Grade.class);
        grade.setGradeName("Grade-" + UUID.randomUUID());
        tracker.track(dataManager.commit(grade));

        Position type = dataManager.create(Position.class);
        type.setPositionRuName("Position-" + UUID.randomUUID());
        tracker.track(dataManager.commit(type));

        City city = dataManager.create(City.class);
        city.setCityRuName("City-" + UUID.randomUUID());
        city.setCityPhoneCode(String.valueOf(Math.abs(UUID.randomUUID().hashCode() % 90000) + 10000));
        tracker.track(dataManager.commit(city));

        Project project = dataManager.create(Project.class);
        project.setProjectName("Project-" + UUID.randomUUID());
        tracker.track(dataManager.commit(project));

        OpenPosition position = dataManager.create(OpenPosition.class);
        position.setVacansyName("Senior Java " + UUID.randomUUID());
        position.setVacansyID("VP-" + Math.abs(UUID.randomUUID().hashCode() % 900000));
        position.setRemoteWork(0);
        position.setCommandCandidate(0);
        position.setWorkExperience(0);
        position.setInternalProject(false);
        position.setSignDraft(true);
        position.setOpenClose(true);
        position.setPriority(1);
        position.setGrade(grade);
        position.setPositionType(type);
        position.setCityPosition(city);
        position.setProjectName(project);
        return tracker.track(dataManager.commit(position, "openPosition-edit-view"));
    }

    private void fillLobs(OpenPosition position, String comment, String commentEn,
                          String exercise, String memo, String letter) {
        position.setComment(comment);
        position.setCommentEn(commentEn);
        position.setExercise(exercise);
        position.setMemoForInterview(memo);
        position.setTemplateLetter(letter);
    }
}
