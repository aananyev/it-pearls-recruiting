package com.company.itpearls.core;

import com.company.itpearls.ItpearlsTestContainer;
import com.company.itpearls.TestEntityTracker;
import com.company.itpearls.entity.City;
import com.company.itpearls.entity.Company;
import com.company.itpearls.entity.CompanyDepartament;
import com.company.itpearls.entity.Country;
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

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class CompanyDepartamentServiceTest {

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
    public void testCreateCompanyDepartament() {
        Company company = createTestCompany();
        CompanyDepartament department = dataManager.create(CompanyDepartament.class);
        String uniqueName = "TestDept-" + UUID.randomUUID();
        department.setDepartamentRuName(uniqueName);
        department.setCompanyName(company);
        department.setDepartamentNumberOfProgrammers(10);

        CompanyDepartament saved = tracker.track(dataManager.commit(department, "companyDepartament-edit-view"));

        assertNotNull(saved.getId());
        assertEquals(uniqueName, saved.getDepartamentRuName());
        assertEquals(company.getId(), saved.getCompanyName().getId());
    }

    @Test
    public void testEditAndSaveCompanyDepartament() {
        CompanyDepartament department = createTestCompanyDepartament();
        UUID id = department.getId();

        CompanyDepartament loaded = dataManager.load(CompanyDepartament.class)
                .id(id)
                .view("companyDepartament-edit-view")
                .one();
        loaded.setDepartamentNumberOfProgrammers(25);
        dataManager.commit(loaded);

        CompanyDepartament reloaded = dataManager.load(CompanyDepartament.class)
                .id(id)
                .view("companyDepartament-edit-view")
                .one();
        assertEquals(Integer.valueOf(25), reloaded.getDepartamentNumberOfProgrammers());
    }

    @Test
    public void testBrowseLoadCompanyDepartament() {
        CompanyDepartament department = createTestCompanyDepartament();

        List<CompanyDepartament> departments = dataManager.loadList(LoadContext.create(CompanyDepartament.class)
                .setQuery(LoadContext.createQuery(
                        "select e from itpearls_CompanyDepartament e where e.id = :id")
                        .setParameter("id", department.getId()))
                .setView("companyDepartament-browse-view"));

        assertEquals(1, departments.size());
        CompanyDepartament loaded = departments.get(0);
        assertEquals(department.getDepartamentRuName(), loaded.getDepartamentRuName());
        assertNull(loaded.getDepartamentDescription());
    }

    @Test
    public void testSoftDeleteCompanyDepartament() {
        CompanyDepartament department = createTestCompanyDepartament();
        UUID id = department.getId();

        dataManager.remove(department);

        try (Transaction tx = persistence.createTransaction()) {
            CompanyDepartament deleted = persistence.getEntityManager()
                    .createQuery("select e from itpearls_CompanyDepartament e where e.id = :id",
                            CompanyDepartament.class)
                    .setParameter("id", id)
                    .getSingleResult();
            tx.commit();
            assertNotNull(deleted.getDeleteTs());
        }

        List<CompanyDepartament> active = dataManager.loadList(LoadContext.create(CompanyDepartament.class)
                .setQuery(LoadContext.createQuery(
                        "select e from itpearls_CompanyDepartament e where e.id = :id and e.deleteTs is null")
                        .setParameter("id", id))
                .setView(View.LOCAL));
        assertTrue(active.isEmpty());
    }

    private CompanyDepartament createTestCompanyDepartament() {
        Company company = createTestCompany();
        CompanyDepartament department = dataManager.create(CompanyDepartament.class);
        department.setDepartamentRuName("TestDept-" + UUID.randomUUID());
        department.setCompanyName(company);
        department.setDepartamentNumberOfProgrammers(5);
        return tracker.track(dataManager.commit(department, "companyDepartament-edit-view"));
    }

    private Company createTestCompany() {
        City city = createTestCity();
        Company company = dataManager.create(Company.class);
        company.setComanyName("TestCompany-" + UUID.randomUUID());
        company.setCompanyShortName("TD");
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
