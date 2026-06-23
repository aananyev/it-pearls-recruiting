package com.company.itpearls.core;

import com.company.itpearls.ItpearlsTestContainer;
import com.company.itpearls.TestEntityTracker;
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

public class RegionServiceTest {

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
    public void testCreateRegion() {
        Country country = createTestCountry();
        Region region = dataManager.create(Region.class);
        String uniqueName = "TestRegion-" + UUID.randomUUID();
        region.setRegionRuName(uniqueName);
        region.setRegionCode(1000 + (int) (Math.random() * 100000));
        region.setRegionCountry(country);

        Region saved = tracker.track(dataManager.commit(region, "region-edit-view"));

        assertNotNull(saved.getId());
        assertEquals(uniqueName, saved.getRegionRuName());
        assertNotNull(saved.getRegionCountry());
        assertEquals(country.getId(), saved.getRegionCountry().getId());
    }

    @Test
    public void testEditAndSaveRegion() {
        Region region = createTestRegion();
        UUID id = region.getId();

        Region loaded = dataManager.load(Region.class)
                .id(id)
                .view("region-edit-view")
                .one();
        int newCode = 3000 + (int) (Math.random() * 100000);
        loaded.setRegionCode(newCode);
        dataManager.commit(loaded);

        Region reloaded = dataManager.load(Region.class)
                .id(id)
                .view("region-edit-view")
                .one();
        assertEquals(Integer.valueOf(newCode), reloaded.getRegionCode());
    }

    @Test
    public void testBrowseLoadRegion() {
        Region region = createTestRegion();

        List<Region> regions = dataManager.loadList(LoadContext.create(Region.class)
                .setQuery(LoadContext.createQuery(
                        "select e from itpearls_Region e where e.id = :id")
                        .setParameter("id", region.getId()))
                .setView("region-browse-view"));

        assertEquals(1, regions.size());
        Region loaded = regions.get(0);
        assertEquals(region.getRegionRuName(), loaded.getRegionRuName());
        assertEquals(region.getRegionCode(), loaded.getRegionCode());
        assertNotNull(loaded.getRegionCountry());
        assertEquals(region.getRegionCountry().getId(), loaded.getRegionCountry().getId());
    }

    @Test
    public void testSoftDeleteRegion() {
        Region region = createTestRegion();
        UUID id = region.getId();

        dataManager.remove(region);

        try (Transaction tx = persistence.createTransaction()) {
            Region deleted = persistence.getEntityManager()
                    .createQuery("select e from itpearls_Region e where e.id = :id", Region.class)
                    .setParameter("id", id)
                    .getSingleResult();
            tx.commit();
            assertNotNull(deleted.getDeleteTs());
        }

        List<Region> active = dataManager.loadList(LoadContext.create(Region.class)
                .setQuery(LoadContext.createQuery(
                        "select e from itpearls_Region e where e.id = :id and e.deleteTs is null")
                        .setParameter("id", id))
                .setView(View.LOCAL));
        assertTrue(active.isEmpty());
    }

    private Region createTestRegion() {
        Country country = createTestCountry();
        Region region = dataManager.create(Region.class);
        region.setRegionRuName("TestRegion-" + UUID.randomUUID());
        region.setRegionCode(2000 + (int) (Math.random() * 100000));
        region.setRegionCountry(country);
        return tracker.track(dataManager.commit(region, "region-edit-view"));
    }

    private Country createTestCountry() {
        Country country = dataManager.create(Country.class);
        country.setCountryRuName("TestCountry-" + UUID.randomUUID());
        country.setCountryShortName("TR");
        country.setPhoneCode(200);
        return tracker.track(dataManager.commit(country, "country-edit-view"));
    }
}
