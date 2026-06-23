package com.company.itpearls.core;

import com.company.itpearls.ItpearlsTestContainer;
import com.company.itpearls.TestEntityTracker;
import com.company.itpearls.entity.Grade;
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

public class GradeServiceTest {

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
    public void testCreateGrade() {
        Grade grade = dataManager.create(Grade.class);
        String uniqueName = "TestGrade-" + UUID.randomUUID();
        grade.setGradeName(uniqueName);

        Grade saved = tracker.track(dataManager.commit(grade, "grade-edit-view"));

        assertNotNull(saved.getId());
        assertEquals(uniqueName, saved.getGradeName());
    }

    @Test
    public void testEditAndSaveGrade() {
        Grade grade = createTestGrade();
        UUID id = grade.getId();

        Grade loaded = dataManager.load(Grade.class).id(id).view("grade-edit-view").one();
        String newName = "TestGrade-Updated-" + UUID.randomUUID();
        loaded.setGradeName(newName);
        dataManager.commit(loaded);

        Grade reloaded = dataManager.load(Grade.class).id(id).view("grade-edit-view").one();
        assertEquals(newName, reloaded.getGradeName());
    }

    @Test
    public void testBrowseLoadGrade() {
        Grade grade = createTestGrade();

        List<Grade> grades = dataManager.loadList(LoadContext.create(Grade.class)
                .setQuery(LoadContext.createQuery(
                        "select e from itpearls_Grade e where e.id = :id")
                        .setParameter("id", grade.getId()))
                .setView("grade-browse-view"));

        assertEquals(1, grades.size());
        assertEquals(grade.getGradeName(), grades.get(0).getGradeName());
    }

    @Test
    public void testSoftDeleteGrade() {
        Grade grade = createTestGrade();
        UUID id = grade.getId();

        dataManager.remove(grade);

        try (Transaction tx = persistence.createTransaction()) {
            persistence.getEntityManager().setSoftDeletion(false);
            Grade deleted = persistence.getEntityManager()
                    .createQuery("select e from itpearls_Grade e where e.id = :id", Grade.class)
                    .setParameter("id", id)
                    .getSingleResult();
            tx.commit();
            assertNotNull(deleted.getDeleteTs());
        }

        List<Grade> active = dataManager.loadList(LoadContext.create(Grade.class)
                .setQuery(LoadContext.createQuery(
                        "select e from itpearls_Grade e where e.id = :id and e.deleteTs is null")
                        .setParameter("id", id))
                .setView(View.LOCAL));
        assertTrue(active.isEmpty());
    }

    private Grade createTestGrade() {
        Grade grade = dataManager.create(Grade.class);
        grade.setGradeName("TestGrade-" + UUID.randomUUID());
        return tracker.track(dataManager.commit(grade, "grade-edit-view"));
    }
}
