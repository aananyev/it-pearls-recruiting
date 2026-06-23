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
import com.haulmont.cuba.security.entity.User;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class OpenPositionServiceTest {

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
    public void testCreateOpenPosition() {
        Project project = createTestProject();
        OpenPosition position = dataManager.create(OpenPosition.class);
        String uniqueName = "TestVacancy-" + UUID.randomUUID();
        position.setVacansyName(uniqueName);
        position.setOpenClose(false);
        position.setRemoteWork(1);
        position.setCommandCandidate(1);
        position.setWorkExperience(1);
        position.setInternalProject(false);
        position.setProjectName(project);

        OpenPosition saved = tracker.track(dataManager.commit(position, "openPosition-edit-view"));

        assertNotNull(saved.getId());
        assertEquals(uniqueName, saved.getVacansyName());
    }

    @Test
    public void testEditAndSaveOpenPosition() {
        OpenPosition position = createTestOpenPosition();
        UUID id = position.getId();

        OpenPosition loaded = dataManager.load(OpenPosition.class).id(id).view("openPosition-edit-view").one();
        loaded.setPriority(3);
        dataManager.commit(loaded);

        OpenPosition reloaded = dataManager.load(OpenPosition.class).id(id).view("openPosition-edit-view").one();
        assertEquals(Integer.valueOf(3), reloaded.getPriority());
    }

    @Test
    public void testBrowseLoadOpenPosition() {
        OpenPosition position = createTestOpenPositionWithPositionType();
        Position positionType = position.getPositionType();

        List<OpenPosition> positions = dataManager.loadList(LoadContext.create(OpenPosition.class)
                .setQuery(LoadContext.createQuery(
                        "select e from itpearls_OpenPosition e where e.id = :id")
                        .setParameter("id", position.getId()))
                .setView("openPosition-browse-view"));

        assertEquals(1, positions.size());
        assertEquals(position.getVacansyName(), positions.get(0).getVacansyName());
        assertNotNull(positions.get(0).getPositionType());
        assertEquals(positionType.getPositionRuName(), positions.get(0).getPositionType().getPositionRuName());
        assertEquals(positionType.getPositionEnName(), positions.get(0).getPositionType().getPositionEnName());
    }

    @Test
    public void testSoftDeleteOpenPosition() {
        OpenPosition position = createTestOpenPosition();
        UUID id = position.getId();

        dataManager.remove(position);

        try (Transaction tx = persistence.createTransaction()) {
            persistence.getEntityManager().setSoftDeletion(false);
            OpenPosition deleted = persistence.getEntityManager()
                    .createQuery("select e from itpearls_OpenPosition e where e.id = :id", OpenPosition.class)
                    .setParameter("id", id)
                    .getSingleResult();
            tx.commit();
            assertNotNull(deleted.getDeleteTs());
        }

        List<OpenPosition> active = dataManager.loadList(LoadContext.create(OpenPosition.class)
                .setQuery(LoadContext.createQuery(
                        "select e from itpearls_OpenPosition e where e.id = :id and e.deleteTs is null")
                        .setParameter("id", id))
                .setView(View.LOCAL));
        assertTrue(active.isEmpty());
    }

    private OpenPosition createTestOpenPosition() {
        return createTestOpenPositionWithPositionType(null);
    }

    private OpenPosition createTestOpenPositionWithPositionType() {
        Position positionType = createTestPosition();
        return createTestOpenPositionWithPositionType(positionType);
    }

    private OpenPosition createTestOpenPositionWithPositionType(Position positionType) {
        Project project = createTestProject();
        OpenPosition position = dataManager.create(OpenPosition.class);
        position.setVacansyName("TestVacancy-" + UUID.randomUUID());
        position.setOpenClose(false);
        position.setRemoteWork(1);
        position.setCommandCandidate(1);
        position.setWorkExperience(1);
        position.setInternalProject(false);
        position.setProjectName(project);
        if (positionType != null) {
            position.setPositionType(positionType);
        }
        return tracker.track(dataManager.commit(position, "openPosition-edit-view"));
    }

    private Position createTestPosition() {
        Position position = dataManager.create(Position.class);
        position.setPositionRuName("TestPositionRu-" + UUID.randomUUID());
        position.setPositionEnName("TestPositionEn-" + UUID.randomUUID());
        Position saved = tracker.track(dataManager.commit(position, "position-edit-view"));
        return dataManager.reload(saved, "position-picker-view");
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
