package com.company.itpearls.core;

import com.company.itpearls.ItpearlsTestContainer;
import com.company.itpearls.TestEntityTracker;
import com.company.itpearls.entity.City;
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

public class CityServiceTest {

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
    public void testCreateCity() {
        City city = dataManager.create(City.class);
        String uniqueName = "TestCity-" + UUID.randomUUID();
        city.setCityRuName(uniqueName);
        city.setCityPhoneCode(String.valueOf(UUID.randomUUID().hashCode() & 0xFFFF).substring(0, 4));

        City saved = tracker.track(dataManager.commit(city, "city-edit-view"));

        assertNotNull(saved.getId());
        assertEquals(uniqueName, saved.getCityRuName());
    }

    @Test
    public void testEditAndSaveCity() {
        City city = createTestCity();
        UUID id = city.getId();

        City loaded = dataManager.load(City.class)
                .id(id)
                .view("city-edit-view")
                .one();
        String newPhoneCode = String.valueOf(Math.abs(UUID.randomUUID().hashCode()) % 10000);
        loaded.setCityPhoneCode(newPhoneCode);
        dataManager.commit(loaded);

        City reloaded = dataManager.load(City.class)
                .id(id)
                .view("city-edit-view")
                .one();
        assertEquals(newPhoneCode, reloaded.getCityPhoneCode());
    }

    @Test
    public void testBrowseLoadCity() {
        City city = createTestCity();

        List<City> cities = dataManager.loadList(LoadContext.create(City.class)
                .setQuery(LoadContext.createQuery(
                        "select e from itpearls_City e where e.id = :id")
                        .setParameter("id", city.getId()))
                .setView("city-browse-view"));

        assertEquals(1, cities.size());
        City loaded = cities.get(0);
        assertEquals(city.getCityRuName(), loaded.getCityRuName());
        assertEquals(city.getCityPhoneCode(), loaded.getCityPhoneCode());
        assertNull(loaded.getOpenPosition());
    }

    @Test
    public void testSoftDeleteCity() {
        City city = createTestCity();
        UUID id = city.getId();

        dataManager.remove(city);

        try (Transaction tx = persistence.createTransaction()) {
            City deleted = persistence.getEntityManager()
                    .createQuery("select e from itpearls_City e where e.id = :id", City.class)
                    .setParameter("id", id)
                    .getSingleResult();
            tx.commit();
            assertNotNull(deleted.getDeleteTs());
        }

        List<City> active = dataManager.loadList(LoadContext.create(City.class)
                .setQuery(LoadContext.createQuery(
                        "select e from itpearls_City e where e.id = :id and e.deleteTs is null")
                        .setParameter("id", id))
                .setView(View.LOCAL));
        assertTrue(active.isEmpty());
    }

    private City createTestCity() {
        City city = dataManager.create(City.class);
        city.setCityRuName("TestCity-" + UUID.randomUUID());
        city.setCityPhoneCode(String.valueOf(Math.abs(UUID.randomUUID().hashCode()) % 10000));
        return tracker.track(dataManager.commit(city, "city-edit-view"));
    }
}
