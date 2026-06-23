package com.company.itpearls.core;

import com.company.itpearls.ItpearlsTestContainer;
import com.company.itpearls.TestEntityTracker;
import com.company.itpearls.entity.CompanyGroup;
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

public class CompanyGroupServiceTest {

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
    public void testCreateCompanyGroup() {
        CompanyGroup group = dataManager.create(CompanyGroup.class);
        String uniqueName = "TestGroup-" + UUID.randomUUID();
        group.setCompanyRuGroupName(uniqueName);

        CompanyGroup saved = tracker.track(dataManager.commit(group, "companyGroup-edit-view"));

        assertNotNull(saved.getId());
        assertEquals(uniqueName, saved.getCompanyRuGroupName());
    }

    @Test
    public void testEditAndSaveCompanyGroup() {
        CompanyGroup group = createTestCompanyGroup();
        UUID id = group.getId();

        CompanyGroup loaded = dataManager.load(CompanyGroup.class)
                .id(id)
                .view("companyGroup-edit-view")
                .one();
        String newName = "TestGroup-Edited-" + UUID.randomUUID();
        loaded.setCompanyRuGroupName(newName);
        dataManager.commit(loaded);

        CompanyGroup reloaded = dataManager.load(CompanyGroup.class)
                .id(id)
                .view("companyGroup-edit-view")
                .one();
        assertEquals(newName, reloaded.getCompanyRuGroupName());
    }

    @Test
    public void testBrowseLoadCompanyGroup() {
        CompanyGroup group = createTestCompanyGroup();

        List<CompanyGroup> groups = dataManager.loadList(LoadContext.create(CompanyGroup.class)
                .setQuery(LoadContext.createQuery(
                        "select e from itpearls_CompanyGroup e where e.id = :id")
                        .setParameter("id", group.getId()))
                .setView("companyGroup-browse-view"));

        assertEquals(1, groups.size());
        assertEquals(group.getCompanyRuGroupName(), groups.get(0).getCompanyRuGroupName());
    }

    @Test
    public void testSoftDeleteCompanyGroup() {
        CompanyGroup group = createTestCompanyGroup();
        UUID id = group.getId();

        dataManager.remove(group);

        try (Transaction tx = persistence.createTransaction()) {
            CompanyGroup deleted = persistence.getEntityManager()
                    .createQuery("select e from itpearls_CompanyGroup e where e.id = :id", CompanyGroup.class)
                    .setParameter("id", id)
                    .getSingleResult();
            tx.commit();
            assertNotNull(deleted.getDeleteTs());
        }

        List<CompanyGroup> active = dataManager.loadList(LoadContext.create(CompanyGroup.class)
                .setQuery(LoadContext.createQuery(
                        "select e from itpearls_CompanyGroup e where e.id = :id and e.deleteTs is null")
                        .setParameter("id", id))
                .setView(View.LOCAL));
        assertTrue(active.isEmpty());
    }

    private CompanyGroup createTestCompanyGroup() {
        CompanyGroup group = dataManager.create(CompanyGroup.class);
        group.setCompanyRuGroupName("TestGroup-" + UUID.randomUUID());
        return tracker.track(dataManager.commit(group, "companyGroup-edit-view"));
    }
}
