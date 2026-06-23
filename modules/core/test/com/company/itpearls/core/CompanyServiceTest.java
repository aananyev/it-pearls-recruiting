package com.company.itpearls.core;

import com.company.itpearls.ItpearlsTestContainer;
import com.company.itpearls.TestEntityTracker;
import com.company.itpearls.entity.City;
import com.company.itpearls.entity.Company;
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

public class CompanyServiceTest {

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
    public void testCreateCompany() {
        City city = createTestCity();
        Company company = dataManager.create(Company.class);
        String uniqueName = "TestCompany-" + UUID.randomUUID();
        company.setComanyName(uniqueName);
        company.setCompanyShortName("TC");
        company.setOurClient(false);
        company.setOurLegalEntity(false);
        company.setCityOfCompany(city);
        company.setRegionOfCompany(city.getCityRegion());
        company.setCountryOfCompany(city.getCityRegion().getRegionCountry());

        Company saved = tracker.track(dataManager.commit(company, "company-edit-view"));

        assertNotNull(saved.getId());
        assertEquals(uniqueName, saved.getComanyName());
        assertEquals("TC", saved.getCompanyShortName());
    }

    @Test
    public void testEditAndSaveCompany() {
        Company company = createTestCompany();
        UUID id = company.getId();

        Company loaded = dataManager.load(Company.class)
                .id(id)
                .view("company-edit-view")
                .one();
        loaded.setCompanyShortName("T2");
        loaded.setOurClient(true);
        dataManager.commit(loaded);

        Company reloaded = dataManager.load(Company.class)
                .id(id)
                .view("company-edit-view")
                .one();
        assertEquals("T2", reloaded.getCompanyShortName());
        assertEquals(Boolean.TRUE, reloaded.getOurClient());
    }

    @Test
    public void testBrowseLoadCompany() {
        Company company = createTestCompany();

        List<Company> companies = dataManager.loadList(LoadContext.create(Company.class)
                .setQuery(LoadContext.createQuery(
                        "select e from itpearls_Company e where e.id = :id")
                        .setParameter("id", company.getId()))
                .setView("company-browse-view"));

        assertEquals(1, companies.size());
        Company loaded = companies.get(0);
        assertEquals(company.getComanyName(), loaded.getComanyName());
        assertEquals(company.getCompanyShortName(), loaded.getCompanyShortName());
        assertNull(loaded.getCompanyDescription());
    }

    @Test
    public void testSoftDeleteCompany() {
        Company company = createTestCompany();
        UUID id = company.getId();

        dataManager.remove(company);

        try (Transaction tx = persistence.createTransaction()) {
            Company deleted = persistence.getEntityManager()
                    .createQuery("select e from itpearls_Company e where e.id = :id", Company.class)
                    .setParameter("id", id)
                    .getSingleResult();
            tx.commit();
            assertNotNull(deleted.getDeleteTs());
        }

        List<Company> active = dataManager.loadList(LoadContext.create(Company.class)
                .setQuery(LoadContext.createQuery(
                        "select e from itpearls_Company e where e.id = :id and e.deleteTs is null")
                        .setParameter("id", id))
                .setView(View.LOCAL));
        assertTrue(active.isEmpty());
    }

    private Company createTestCompany() {
        City city = createTestCity();
        Company company = dataManager.create(Company.class);
        company.setComanyName("TestCompany-" + UUID.randomUUID());
        company.setCompanyShortName("TX");
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
