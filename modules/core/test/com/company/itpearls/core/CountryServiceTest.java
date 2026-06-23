package com.company.itpearls.core;

import com.company.itpearls.ItpearlsTestContainer;
import com.company.itpearls.TestEntityTracker;
import com.company.itpearls.entity.Country;
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

public class CountryServiceTest {

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
    public void testCreateCountry() {
        Country country = dataManager.create(Country.class);
        String uniqueName = "TestCountry-" + UUID.randomUUID();
        country.setCountryRuName(uniqueName);
        country.setCountryShortName("TC");
        country.setPhoneCode(999);

        Country saved = tracker.track(dataManager.commit(country, "country-edit-view"));

        assertNotNull(saved.getId());
        assertEquals(uniqueName, saved.getCountryRuName());
        assertEquals("TC", saved.getCountryShortName());
        assertEquals(Integer.valueOf(999), saved.getPhoneCode());
    }

    @Test
    public void testEditAndSaveCountry() {
        Country country = createTestCountry();
        UUID id = country.getId();

        Country loaded = dataManager.load(Country.class)
                .id(id)
                .view("country-edit-view")
                .one();
        loaded.setPhoneCode(123);
        loaded.setCountryShortName("T2");
        dataManager.commit(loaded);

        Country reloaded = dataManager.load(Country.class)
                .id(id)
                .view("country-edit-view")
                .one();
        assertEquals(Integer.valueOf(123), reloaded.getPhoneCode());
        assertEquals("T2", reloaded.getCountryShortName());
    }

    @Test
    public void testBrowseLoadCountry() {
        Country country = createTestCountry();

        List<Country> countries = dataManager.loadList(LoadContext.create(Country.class)
                .setQuery(LoadContext.createQuery(
                        "select e from itpearls_Country e where e.id = :id")
                        .setParameter("id", country.getId()))
                .setView("country-browse-view"));

        assertEquals(1, countries.size());
        Country loaded = countries.get(0);
        assertEquals(country.getCountryRuName(), loaded.getCountryRuName());
        assertEquals(country.getCountryShortName(), loaded.getCountryShortName());
        assertEquals(country.getPhoneCode(), loaded.getPhoneCode());
    }

    @Test
    public void testSoftDeleteCountry() {
        Country country = createTestCountry();
        UUID id = country.getId();

        dataManager.remove(country);

        try (Transaction tx = persistence.createTransaction()) {
            Country deleted = persistence.getEntityManager()
                    .createQuery("select e from itpearls_Country e where e.id = :id", Country.class)
                    .setParameter("id", id)
                    .getSingleResult();
            tx.commit();
            assertNotNull(deleted.getDeleteTs());
        }

        List<Country> active = dataManager.loadList(LoadContext.create(Country.class)
                .setQuery(LoadContext.createQuery(
                        "select e from itpearls_Country e where e.id = :id and e.deleteTs is null")
                        .setParameter("id", id))
                .setView(View.LOCAL));
        assertTrue(active.isEmpty());
    }

    private Country createTestCountry() {
        Country country = dataManager.create(Country.class);
        country.setCountryRuName("TestCountry-" + UUID.randomUUID());
        country.setCountryShortName("TX");
        country.setPhoneCode(100);
        return tracker.track(dataManager.commit(country, "country-edit-view"));
    }
}
