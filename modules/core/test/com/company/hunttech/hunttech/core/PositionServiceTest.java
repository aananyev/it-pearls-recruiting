package com.company.hunttech.core;

import com.company.hunttech.HunttechTestContainer;
import com.company.hunttech.TestEntityTracker;
import com.company.hunttech.entity.Position;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Transaction;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.core.global.ViewBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class PositionServiceTest {

    @ClassRule
    public static HunttechTestContainer cont = HunttechTestContainer.Common.INSTANCE;

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
    public void testCreatePosition() {
        Position position = dataManager.create(Position.class);
        String uniqueRu = "TestPositionRu-" + UUID.randomUUID();
        position.setPositionRuName(uniqueRu);
        position.setPositionEnName("TestPositionEn-" + UUID.randomUUID());

        Position saved = tracker.track(dataManager.commit(position, "position-edit-view"));

        assertNotNull(saved.getId());
        assertEquals(uniqueRu, saved.getPositionRuName());
    }

    @Test
    public void testEditAndSavePosition() {
        Position position = createTestPosition();
        UUID id = position.getId();

        Position loaded = dataManager.load(Position.class).id(id).view("position-edit-view").one();
        loaded.setPositionEnName("UpdatedEn-" + UUID.randomUUID());
        dataManager.commit(loaded);

        Position reloaded = dataManager.load(Position.class).id(id).view("position-edit-view").one();
        assertNotNull(reloaded.getPositionEnName());
        assertTrue(reloaded.getPositionEnName().startsWith("UpdatedEn-"));
    }

    @Test
    public void testBrowseLoadPosition() {
        Position position = createTestPosition();

        List<Position> positions = dataManager.loadList(LoadContext.create(Position.class)
                .setQuery(LoadContext.createQuery(
                        "select e from hunttech_Position e where e.id = :id")
                        .setParameter("id", position.getId()))
                .setView("position-browse-view"));

        assertEquals(1, positions.size());
        assertEquals(position.getPositionRuName(), positions.get(0).getPositionRuName());
    }

    @Test
    public void testSoftDeletePosition() {
        Position position = createTestPosition();
        UUID id = position.getId();

        dataManager.remove(position);

        try (Transaction tx = persistence.createTransaction()) {
            persistence.getEntityManager().setSoftDeletion(false);
            Position deleted = persistence.getEntityManager()
                    .createQuery("select e from hunttech_Position e where e.id = :id", Position.class)
                    .setParameter("id", id)
                    .getSingleResult();
            tx.commit();
            assertNotNull(deleted.getDeleteTs());
        }

        List<Position> active = dataManager.loadList(LoadContext.create(Position.class)
                .setQuery(LoadContext.createQuery(
                        "select e from hunttech_Position e where e.id = :id and e.deleteTs is null")
                        .setParameter("id", id))
                .setView(View.LOCAL));
        assertTrue(active.isEmpty());
    }

    @Test
    public void testEditViewLoadsLobFields() {
        Position position = createTestPosition();
        UUID id = position.getId();

        // Моделируем загрузку edit-формы: position-edit-view расширен inline LOB-полями
        // (как в position-edit.xml). До фикса 2026-08-11 reload+setter на detached падал
        // IllegalStateException: Cannot get unfetched attribute [standartDescription].
        View view = ViewBuilder.of(Position.class)
                .add("positionRuName")
                .add("positionEnName")
                .add("standartDescription")
                .add("whoIsThisGuy")
                .build();
        Position loaded = dataManager.load(Position.class).id(id).view(view).one();

        assertTrue(PersistenceHelper.isLoaded(loaded, "standartDescription"));
        assertTrue(PersistenceHelper.isLoaded(loaded, "whoIsThisGuy"));

        // Сеттеры LOB-полей на detached не должны бросать IllegalStateException
        // (woven-сеттер вызывает getter для change detection — атрибут обязан быть в fetch group).
        loaded.setStandartDescription("<p>Обновлённое стандартное описание</p>");
        loaded.setWhoIsThisGuy("<p>Обновлённое описание должности</p>");
        dataManager.commit(loaded);

        Position reloaded = dataManager.load(Position.class).id(id).view(view).one();
        assertTrue(reloaded.getStandartDescription().contains("стандартное описание"));
        assertTrue(reloaded.getWhoIsThisGuy().toLowerCase().contains("обновлённое описание"));
    }

    private Position createTestPosition() {
        Position position = dataManager.create(Position.class);
        position.setPositionRuName("TestPositionRu-" + UUID.randomUUID());
        position.setPositionEnName("TestPositionEn-" + UUID.randomUUID());
        return tracker.track(dataManager.commit(position, "position-edit-view"));
    }
}
