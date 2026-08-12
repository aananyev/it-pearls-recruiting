package com.company.hunttech.core;

import com.company.hunttech.HunttechTestContainer;
import com.company.hunttech.TestEntityTracker;
import com.company.hunttech.entity.Company;
import com.company.hunttech.entity.CompanyDepartament;
import com.company.hunttech.entity.Person;
import com.company.hunttech.entity.Project;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.ViewBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Защита ProjectEdit от ошибок detached object (Unfetched Attribute Access /
 * LazyInitializationException / ValidationException при commit detached-сущности).
 *
 * Моделирует ровно те сценарии, которые выполняет контроллер ProjectEdit:
 * 1) открытие формы — проект загружается с view {@code project-edit-view};
 * 2) FK-граф (projectTree/projectDepartment/projectOwner) читается из view;
 * 3) LOB-поля (projectDescription, templateLetter) отсутствуют в edit-view —
 *    контроллер догружает их через {@code reload(ViewBuilder)} при первом
 *    открытии вкладок (loadProjectDescription/loadTemplateLetter);
 * 4) commit detached-объекта после изменения полей.
 */
public class ProjectDetachedObjectTest {

    @ClassRule
    public static HunttechTestContainer cont = HunttechTestContainer.Common.INSTANCE;

    private DataManager dataManager;
    private TestEntityTracker tracker;

    @Before
    public void setUp() {
        dataManager = AppBeans.get(DataManager.class);
        tracker = new TestEntityTracker(dataManager);
    }

    @After
    public void tearDown() {
        tracker.cleanup();
    }

    @Test
    public void projectEditViewProvidesEveryFormField() {
        Project project = createProject();

        // Форма открывается с detached project-edit-view.
        Project detached = dataManager.load(Project.class)
                .id(project.getId())
                .view("project-edit-view")
                .one();

        assertEquals(project.getProjectName(), detached.getProjectName());
        assertEquals(project.getProjectIsClosed(), detached.getProjectIsClosed());
        assertEquals(project.getDefaultProject(), detached.getDefaultProject());
        assertEquals(project.getStartProjectDate(), detached.getStartProjectDate());
        assertEquals(project.getEndProjectDate(), detached.getEndProjectDate());
        assertEquals(project.getGeneralChat(), detached.getGeneralChat());
        assertEquals(project.getChatForCV(), detached.getChatForCV());
    }

    @Test
    public void fkGraphAccessibleFromEditViewWithoutLazyErrors() {
        Project project = createProjectWithReferences();

        Project detached = dataManager.load(Project.class)
                .id(project.getId())
                .view("project-edit-view")
                .one();

        // projectTree: project-tree-picker-view несёт projectName вложенного проекта.
        assertNotNull(detached.getProjectTree());
        assertEquals(project.getProjectTree().getProjectName(),
                detached.getProjectTree().getProjectName());
        // projectDepartment: companyDepartament-picker-view несёт departamentRuName.
        assertNotNull(detached.getProjectDepartment());
        assertEquals(project.getProjectDepartment().getDepartamentRuName(),
                detached.getProjectDepartment().getDepartamentRuName());
        // projectOwner: person-owner-view несёт secondName/firstName владельца.
        assertNotNull(detached.getProjectOwner());
        assertEquals(project.getProjectOwner().getSecondName(),
                detached.getProjectOwner().getSecondName());
    }

    @Test
    public void lobFieldsReloadedLikeControllerDoes() {
        Project project = createProject();

        // Контроллер открывает форму с project-edit-view (LOB отсутствуют).
        Project detached = dataManager.load(Project.class)
                .id(project.getId())
                .view("project-edit-view")
                .one();

        // Вкладки «Описание проекта» и «Информация в сопроводительном письме»:
        // ровно тот reload(ViewBuilder), что делает loadProjectDescription/loadTemplateLetter.
        Project withDescription = dataManager.reload(detached,
                ViewBuilder.of(Project.class).add("projectDescription").build());
        Project withLetter = dataManager.reload(detached,
                ViewBuilder.of(Project.class).add("templateLetter").build());

        detached.setProjectDescription(withDescription.getProjectDescription());
        detached.setTemplateLetter(withLetter.getTemplateLetter());
        assertNotNull("Описание не загрузилось через reload",
                detached.getProjectDescription());

        // Commit detached после LOB-reload — без LazyInitializationException.
        Project committed = dataManager.commit(detached);
        tracker.track(committed);

        Project reloaded = dataManager.load(Project.class)
                .id(committed.getId())
                .view("project-edit-view")
                .one();
        assertEquals(detached.getProjectDescription(),
                dataManager.reload(reloaded,
                        ViewBuilder.of(Project.class).add("projectDescription").build())
                        .getProjectDescription());
        assertEquals(detached.getTemplateLetter(),
                dataManager.reload(reloaded,
                        ViewBuilder.of(Project.class).add("templateLetter").build())
                        .getTemplateLetter());
    }

    @Test
    public void modifyDetachedProjectAndCommit() {
        Project project = createProject();
        UUID id = project.getId();

        // Форма открыта: detached-объект, пользователь меняет поля.
        Project detached = dataManager.load(Project.class)
                .id(id)
                .view("project-edit-view")
                .one();
        detached.setGeneralChat("https://chat.example.com/updated");
        detached.setProjectIsClosed(true);

        Project committed = dataManager.commit(detached);
        tracker.track(committed);

        Project reloaded = dataManager.load(Project.class)
                .id(id)
                .view("project-edit-view")
                .one();
        assertEquals("https://chat.example.com/updated", reloaded.getGeneralChat());
        assertTrue("projectIsClosed не сохранился", reloaded.getProjectIsClosed());
    }

    private Project createProject() {
        Project project = dataManager.create(Project.class);
        project.setProjectName("Test-Project-" + UUID.randomUUID());
        project.setProjectIsClosed(false);
        project.setDefaultProject(false);
        project.setStartProjectDate(new Date());
        project.setEndProjectDate(new Date());
        project.setGeneralChat("https://chat.example.com");
        project.setChatForCV("https://cv.example.com");
        project.setProjectDescription("Тестовое описание проекта " + UUID.randomUUID());
        project.setTemplateLetter("Тестовый шаблон письма " + UUID.randomUUID());
        return tracker.track(dataManager.commit(project));
    }

    private Project createProjectWithReferences() {
        Project parent = dataManager.create(Project.class);
        parent.setProjectName("Test-Parent-" + UUID.randomUUID());
        parent.setProjectIsClosed(false);
        parent.setDefaultProject(false);
        tracker.track(dataManager.commit(parent));

        Company company = dataManager.create(Company.class);
        company.setComanyName("Test-Company-" + UUID.randomUUID());
        tracker.track(dataManager.commit(company));

        CompanyDepartament department = dataManager.create(CompanyDepartament.class);
        department.setDepartamentRuName("Test-Department-" + UUID.randomUUID());
        department.setCompanyName(company);
        tracker.track(dataManager.commit(department));

        Person person = dataManager.create(Person.class);
        person.setFirstName("Test-First-" + UUID.randomUUID());
        person.setSecondName("Test-Second-" + UUID.randomUUID());
        tracker.track(dataManager.commit(person));

        Project project = dataManager.create(Project.class);
        project.setProjectName("Test-Project-" + UUID.randomUUID());
        project.setProjectIsClosed(false);
        project.setDefaultProject(false);
        project.setStartProjectDate(new Date());
        project.setEndProjectDate(new Date());
        project.setGeneralChat("https://chat.example.com");
        project.setChatForCV("https://cv.example.com");
        project.setProjectTree(parent);
        project.setProjectDepartment(department);
        project.setProjectOwner(person);
        return tracker.track(dataManager.commit(project));
    }
}
