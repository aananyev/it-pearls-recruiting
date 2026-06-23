package com.company.itpearls.core;

import com.company.itpearls.ItpearlsTestContainer;
import com.company.itpearls.TestEntityTracker;
import com.company.itpearls.entity.ExtUser;
import com.company.itpearls.entity.OpenPosition;
import com.company.itpearls.entity.RecrutiesTasks;
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

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class RecrutiesTasksServiceTest {

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
    public void testCreateRecrutiesTasks() {
        RecrutiesTasks task = createTestTaskEntity();
        RecrutiesTasks saved = tracker.track(dataManager.commit(task, "recrutiesTasks-edit-view"));

        assertNotNull(saved.getId());
        assertEquals(Boolean.FALSE, saved.getClosed());
    }

    @Test
    public void testEditAndSaveRecrutiesTasks() {
        RecrutiesTasks task = tracker.track(
                dataManager.commit(createTestTaskEntity(), "recrutiesTasks-edit-view"));
        UUID id = task.getId();

        RecrutiesTasks loaded = dataManager.load(RecrutiesTasks.class)
                .id(id).view("recrutiesTasks-edit-view").one();
        loaded.setPlanForPeriod(5);
        dataManager.commit(loaded);

        RecrutiesTasks reloaded = dataManager.load(RecrutiesTasks.class)
                .id(id).view("recrutiesTasks-edit-view").one();
        assertEquals(Integer.valueOf(5), reloaded.getPlanForPeriod());
    }

    @Test
    public void testBrowseLoadRecrutiesTasks() {
        RecrutiesTasks task = tracker.track(
                dataManager.commit(createTestTaskEntity(), "recrutiesTasks-edit-view"));

        List<RecrutiesTasks> tasks = dataManager.loadList(LoadContext.create(RecrutiesTasks.class)
                .setQuery(LoadContext.createQuery(
                        "select e from itpearls_RecrutiesTasks e where e.id = :id")
                        .setParameter("id", task.getId()))
                .setView("recrutiesTasks-browse-view"));

        assertEquals(1, tasks.size());
        assertNotNull(tasks.get(0).getOpenPosition());
        assertNotNull(tasks.get(0).getReacrutier());
    }

    @Test
    public void testSoftDeleteRecrutiesTasks() {
        RecrutiesTasks task = tracker.track(
                dataManager.commit(createTestTaskEntity(), "recrutiesTasks-edit-view"));
        UUID id = task.getId();

        dataManager.remove(task);

        try (Transaction tx = persistence.createTransaction()) {
            persistence.getEntityManager().setSoftDeletion(false);
            RecrutiesTasks deleted = persistence.getEntityManager()
                    .createQuery("select e from itpearls_RecrutiesTasks e where e.id = :id",
                            RecrutiesTasks.class)
                    .setParameter("id", id)
                    .getSingleResult();
            tx.commit();
            assertNotNull(deleted.getDeleteTs());
        }

        List<RecrutiesTasks> active = dataManager.loadList(LoadContext.create(RecrutiesTasks.class)
                .setQuery(LoadContext.createQuery(
                        "select e from itpearls_RecrutiesTasks e where e.id = :id and e.deleteTs is null")
                        .setParameter("id", id))
                .setView(View.LOCAL));
        assertTrue(active.isEmpty());
    }

    private RecrutiesTasks createTestTaskEntity() {
        OpenPosition openPosition = new OpenPositionCommentServiceTest.OpenPositionCommentServiceTestHelper(
                dataManager, tracker).createTestOpenPosition();
        ExtUser recruiter = loadAdminExtUser();
        Calendar cal = Calendar.getInstance();
        Date startDate = cal.getTime();
        cal.add(Calendar.MONTH, 1);
        Date endDate = cal.getTime();

        RecrutiesTasks task = dataManager.create(RecrutiesTasks.class);
        task.setOpenPosition(openPosition);
        task.setReacrutier(recruiter);
        task.setStartDate(startDate);
        task.setEndDate(endDate);
        task.setClosed(false);
        task.setSubscribe(true);
        task.setPlanForPeriod(3);
        return task;
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
}
