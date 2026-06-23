package com.company.itpearls.core;

import com.company.itpearls.ItpearlsTestContainer;
import com.company.itpearls.TestEntityTracker;
import com.company.itpearls.entity.City;
import com.company.itpearls.entity.Company;
import com.company.itpearls.entity.CompanyDepartament;
import com.company.itpearls.entity.Country;
import com.company.itpearls.entity.Project;
import com.company.itpearls.entity.Region;
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

public class ProjectServiceTest {

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
    public void testCreateProject() {
        CompanyDepartament department = createTestCompanyDepartament();
        Project project = dataManager.create(Project.class);
        String uniqueName = "TestProject-" + UUID.randomUUID();
        project.setProjectName(uniqueName);
        project.setProjectIsClosed(false);
        project.setDefaultProject(false);
        project.setStartProjectDate(new Date());
        project.setProjectDepartment(department);

        Project saved = tracker.track(dataManager.commit(project, "project-edit-view"));

        assertNotNull(saved.getId());
        assertEquals(uniqueName, saved.getProjectName());
        assertEquals(Boolean.FALSE, saved.getProjectIsClosed());
    }

    @Test
    public void testEditAndSaveProject() {
        Project project = createTestProject();
        UUID id = project.getId();

        Project loaded = dataManager.load(Project.class)
                .id(id)
                .view("project-edit-view")
                .one();
        loaded.setProjectIsClosed(true);
        loaded.setEndProjectDate(new Date());
        dataManager.commit(loaded);

        Project reloaded = dataManager.load(Project.class)
                .id(id)
                .view("project-edit-view")
                .one();
        assertEquals(Boolean.TRUE, reloaded.getProjectIsClosed());
        assertNotNull(reloaded.getEndProjectDate());
    }

    @Test
    public void testBrowseLoadProject() {
        Project project = createTestProject();

        List<Project> projects = dataManager.loadList(LoadContext.create(Project.class)
                .setQuery(LoadContext.createQuery(
                        "select e from itpearls_Project e where e.id = :id")
                        .setParameter("id", project.getId()))
                .setView("project-browse-view"));

        assertEquals(1, projects.size());
        Project loaded = projects.get(0);
        assertEquals(project.getProjectName(), loaded.getProjectName());
        assertNull(loaded.getProjectDescription());
        assertNull(loaded.getTemplateLetter());
    }

    @Test
    public void testSoftDeleteProject() {
        Project project = createTestProject();
        UUID id = project.getId();

        dataManager.remove(project);

        try (Transaction tx = persistence.createTransaction()) {
            Project deleted = persistence.getEntityManager()
                    .createQuery("select e from itpearls_Project e where e.id = :id", Project.class)
                    .setParameter("id", id)
                    .getSingleResult();
            tx.commit();
            assertNotNull(deleted.getDeleteTs());
        }

        List<Project> active = dataManager.loadList(LoadContext.create(Project.class)
                .setQuery(LoadContext.createQuery(
                        "select e from itpearls_Project e where e.id = :id and e.deleteTs is null")
                        .setParameter("id", id))
                .setView(View.LOCAL));
        assertTrue(active.isEmpty());
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
