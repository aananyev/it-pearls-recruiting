package com.company.itpearls.core;

import com.company.itpearls.ItpearlsTestContainer;
import com.company.itpearls.TestEntityTracker;
import com.company.itpearls.entity.ExtUser;
import com.company.itpearls.entity.OpenPosition;
import com.company.itpearls.entity.OpenPositionNews;
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

public class OpenPositionNewsServiceTest {

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
    public void testCreateOpenPositionNews() {
        OpenPositionNews news = createTestNewsEntity();
        OpenPositionNews saved = tracker.track(dataManager.commit(news, "openPositionNews-edit-view"));

        assertNotNull(saved.getId());
        assertNotNull(saved.getSubject());
    }

    @Test
    public void testEditAndSaveOpenPositionNews() {
        OpenPositionNews news = tracker.track(
                dataManager.commit(createTestNewsEntity(), "openPositionNews-edit-view"));
        UUID id = news.getId();

        OpenPositionNews loaded = dataManager.load(OpenPositionNews.class)
                .id(id).view("openPositionNews-edit-view").one();
        loaded.setPriorityNews(true);
        dataManager.commit(loaded);

        OpenPositionNews reloaded = dataManager.load(OpenPositionNews.class)
                .id(id).view("openPositionNews-edit-view").one();
        assertEquals(Boolean.TRUE, reloaded.getPriorityNews());
    }

    @Test
    public void testBrowseLoadOpenPositionNews() {
        OpenPositionNews news = tracker.track(
                dataManager.commit(createTestNewsEntity(), "openPositionNews-edit-view"));

        List<OpenPositionNews> items = dataManager.loadList(LoadContext.create(OpenPositionNews.class)
                .setQuery(LoadContext.createQuery(
                        "select e from itpearls_OpenPositionNews e where e.id = :id")
                        .setParameter("id", news.getId()))
                .setView("openPositionNews-browse-view"));

        assertEquals(1, items.size());
        assertEquals(news.getSubject(), items.get(0).getSubject());
    }

    @Test
    public void testSoftDeleteOpenPositionNews() {
        OpenPositionNews news = tracker.track(
                dataManager.commit(createTestNewsEntity(), "openPositionNews-edit-view"));
        UUID id = news.getId();

        dataManager.remove(news);

        try (Transaction tx = persistence.createTransaction()) {
            persistence.getEntityManager().setSoftDeletion(false);
            OpenPositionNews deleted = persistence.getEntityManager()
                    .createQuery("select e from itpearls_OpenPositionNews e where e.id = :id",
                            OpenPositionNews.class)
                    .setParameter("id", id)
                    .getSingleResult();
            tx.commit();
            assertNotNull(deleted.getDeleteTs());
        }

        List<OpenPositionNews> active = dataManager.loadList(LoadContext.create(OpenPositionNews.class)
                .setQuery(LoadContext.createQuery(
                        "select e from itpearls_OpenPositionNews e where e.id = :id and e.deleteTs is null")
                        .setParameter("id", id))
                .setView(View.LOCAL));
        assertTrue(active.isEmpty());
    }

    private OpenPositionNews createTestNewsEntity() {
        OpenPosition openPosition = new OpenPositionCommentServiceTest.OpenPositionCommentServiceTestHelper(
                dataManager, tracker).createTestOpenPosition();
        ExtUser author = loadAdminExtUser();
        OpenPositionNews news = dataManager.create(OpenPositionNews.class);
        news.setSubject("TestNews-" + UUID.randomUUID());
        news.setOpenPosition(openPosition);
        news.setAuthor(author);
        news.setDateNews(new Date());
        news.setComment("Test news body");
        news.setPriorityNews(false);
        return news;
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
