package com.company.itpearls.core;

import com.company.itpearls.ItpearlsTestContainer;
import com.company.itpearls.TestEntityTracker;
import com.company.itpearls.entity.*;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Transaction;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.View;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class OpenPositionCommentServiceTest {

    @ClassRule
    public static ItpearlsTestContainer cont = ItpearlsTestContainer.Common.INSTANCE;

    private DataManager dataManager;
    private Persistence persistence;
    private TestEntityTracker tracker;

    @Before
    public void setUp() {
        dataManager = AppBeans.get(DataManager.class);
        persistence = cont.persistence();
        tracker = new TestEntityTracker(dataManager);
    }

    @After
    public void tearDown() {
        tracker.cleanup();
    }

    @Test
    public void testCreateOpenPositionComment() {
        OpenPositionComment comment = createTestCommentEntity();
        OpenPositionComment saved = persistComment(comment);

        assertNotNull(saved.getId());
        assertEquals(Integer.valueOf(3), saved.getRating());
    }

    @Test
    public void testEditAndSaveOpenPositionComment() {
        OpenPositionComment comment = persistComment(createTestCommentEntity());
        UUID id = comment.getId();

        OpenPositionComment loaded = dataManager.load(OpenPositionComment.class)
                .id(id).view("openPositionComment-edit-view").one();
        loaded.setRating(4);
        dataManager.commit(loaded);

        OpenPositionComment reloaded = dataManager.load(OpenPositionComment.class)
                .id(id).view("openPositionComment-edit-view").one();
        assertEquals(Integer.valueOf(4), reloaded.getRating());
    }

    @Test
    public void testBrowseLoadOpenPositionComment() {
        OpenPositionComment comment = persistComment(createTestCommentEntity());

        List<OpenPositionComment> comments = dataManager.loadList(LoadContext.create(OpenPositionComment.class)
                .setQuery(LoadContext.createQuery(
                        "select e from itpearls_OpenPositionComment e where e.id = :id")
                        .setParameter("id", comment.getId()))
                .setView("openPositionComment-browse-view"));

        assertEquals(1, comments.size());
        assertEquals(comment.getRating(), comments.get(0).getRating());
    }

    @Test
    public void testSoftDeleteOpenPositionComment() {
        OpenPositionComment comment = persistComment(createTestCommentEntity());
        UUID id = comment.getId();

        dataManager.remove(comment);

        try (Transaction tx = persistence.createTransaction()) {
            persistence.getEntityManager().setSoftDeletion(false);
            OpenPositionComment deleted = persistence.getEntityManager()
                    .createQuery("select e from itpearls_OpenPositionComment e where e.id = :id",
                            OpenPositionComment.class)
                    .setParameter("id", id)
                    .getSingleResult();
            tx.commit();
            assertNotNull(deleted.getDeleteTs());
        }

        List<OpenPositionComment> active = dataManager.loadList(LoadContext.create(OpenPositionComment.class)
                .setQuery(LoadContext.createQuery(
                        "select e from itpearls_OpenPositionComment e where e.id = :id and e.deleteTs is null")
                        .setParameter("id", id))
                .setView(View.LOCAL));
        assertTrue(active.isEmpty());
    }

    private OpenPositionComment persistComment(OpenPositionComment comment) {
        OpenPosition openPosition = dataManager.load(OpenPosition.class)
                .id(comment.getOpenPosition().getId())
                .view("_minimal")
                .one();
        ExtUser user = dataManager.load(ExtUser.class)
                .id(comment.getUser().getId())
                .view("_minimal")
                .one();
        comment.setOpenPosition(openPosition);
        comment.setUser(user);
        try (Transaction tx = persistence.createTransaction()) {
            persistence.getEntityManager().persist(comment);
            tx.commit();
        }
        return tracker.track(dataManager.load(OpenPositionComment.class)
                .id(comment.getId())
                .view("openPositionComment-edit-view")
                .one());
    }

    private OpenPositionComment createTestCommentEntity() {
        OpenPosition openPosition = new OpenPositionCommentServiceTestHelper(dataManager, tracker)
                .createTestOpenPosition();
        ExtUser user = loadAdminExtUser();
        OpenPositionComment comment = dataManager.create(OpenPositionComment.class);
        comment.setOpenPosition(openPosition);
        comment.setUser(user);
        comment.setRating(3);
        comment.setDateComment(new Date());
        comment.setComment("Test comment " + UUID.randomUUID());
        return comment;
    }

    private ExtUser loadAdminExtUser() {
        return dataManager.load(ExtUser.class)
                .query("select e from itpearls_ExtUser e where e.login = :login")
                .parameter("login", "admin")
                .view("extUser-picker-view")
                .optional()
                .orElseGet(() -> dataManager.load(ExtUser.class)
                        .query("select e from itpearls_ExtUser e")
                        .view("extUser-picker-view")
                        .maxResults(1)
                        .one());
    }

    static class OpenPositionCommentServiceTestHelper {
        private final DataManager dataManager;
        private final TestEntityTracker tracker;

        OpenPositionCommentServiceTestHelper(DataManager dataManager, TestEntityTracker tracker) {
            this.dataManager = dataManager;
            this.tracker = tracker;
        }

        OpenPosition createTestOpenPosition() {
            Project project = createTestProject();
            OpenPosition position = dataManager.create(OpenPosition.class);
            position.setVacansyName("TestVacancy-" + UUID.randomUUID());
            position.setOpenClose(false);
            position.setRemoteWork(1);
            position.setCommandCandidate(1);
            position.setWorkExperience(1);
            position.setInternalProject(false);
            position.setProjectName(project);
            return tracker.track(dataManager.commit(position, "openPosition-edit-view"));
        }

        private Project createTestProject() {
            CompanyDepartament department = createTestCompanyDepartament();
            Project project = dataManager.create(Project.class);
            project.setProjectName("TestProject-" + UUID.randomUUID());
            project.setProjectIsClosed(false);
            project.setDefaultProject(false);
            project.setStartProjectDate(new Date());
            project.setProjectDepartment(department);
            return tracker.track(dataManager.commit(project, "project-edit-view"));
        }

        private CompanyDepartament createTestCompanyDepartament() {
            Company company = createTestCompany();
            CompanyDepartament department = dataManager.create(CompanyDepartament.class);
            department.setDepartamentRuName("TestDept-" + UUID.randomUUID());
            department.setCompanyName(company);
            return tracker.track(dataManager.commit(department, "companyDepartament-edit-view"));
        }

        private Company createTestCompany() {
            City city = createTestCity();
            Company company = dataManager.create(Company.class);
            company.setComanyName("TestCompany-" + UUID.randomUUID());
            company.setCompanyShortName("TP");
            company.setOurClient(false);
            company.setOurLegalEntity(false);
            company.setCityOfCompany(city);
            company.setRegionOfCompany(city.getCityRegion());
            company.setCountryOfCompany(city.getCityRegion().getRegionCountry());
            return tracker.track(dataManager.commit(company, "company-edit-view"));
        }

        private City createTestCity() {
            Country country = dataManager.create(Country.class);
            country.setCountryRuName("TestCountry-" + UUID.randomUUID());
            country.setCountryShortName("T");
            country.setPhoneCode(1);
            country = tracker.track(dataManager.commit(country, "country-edit-view"));

            Region region = dataManager.create(Region.class);
            region.setRegionRuName("TestRegion-" + UUID.randomUUID());
            region.setRegionCountry(country);
            region = tracker.track(dataManager.commit(region, "region-edit-view"));

            City city = dataManager.create(City.class);
            city.setCityRuName("TestCity-" + UUID.randomUUID());
            city.setCityPhoneCode("1234");
            city.setCityRegion(region);
            return tracker.track(dataManager.commit(city, "city-edit-view"));
        }
    }
}
