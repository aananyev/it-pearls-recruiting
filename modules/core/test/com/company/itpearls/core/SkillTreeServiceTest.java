package com.company.itpearls.core;

import com.company.itpearls.ItpearlsTestContainer;
import com.company.itpearls.TestEntityTracker;
import com.company.itpearls.entity.SkillTree;
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

public class SkillTreeServiceTest {

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
    public void testCreateSkillTree() {
        SkillTree skill = dataManager.create(SkillTree.class);
        String uniqueName = "TestSkill-" + UUID.randomUUID();
        skill.setSkillName(uniqueName);
        skill.setNotParsing(false);
        skill.setPrioritySkill(0);

        SkillTree saved = tracker.track(dataManager.commit(skill, "skillTree-edit-view"));

        assertNotNull(saved.getId());
        assertEquals(uniqueName, saved.getSkillName());
        assertFalse(saved.getNotParsing());
    }

    @Test
    public void testEditAndSaveSkillTree() {
        SkillTree skill = createTestSkillTree();
        UUID id = skill.getId();

        SkillTree loaded = dataManager.load(SkillTree.class)
                .id(id)
                .view("skillTree-edit-view")
                .one();
        String newWiki = "https://ru.wikipedia.org/wiki/Test_" + UUID.randomUUID();
        loaded.setWikiPage(newWiki);
        dataManager.commit(loaded);

        SkillTree reloaded = dataManager.load(SkillTree.class)
                .id(id)
                .view("skillTree-edit-view")
                .one();
        assertEquals(newWiki, reloaded.getWikiPage());
    }

    @Test
    public void testBrowseLoadSkillTree() {
        SkillTree skill = createTestSkillTree();

        List<SkillTree> skills = dataManager.loadList(LoadContext.create(SkillTree.class)
                .setQuery(LoadContext.createQuery(
                        "select e from itpearls_SkillTree e where e.id = :id")
                        .setParameter("id", skill.getId()))
                .setView("skillTree-browse-view"));

        assertEquals(1, skills.size());
        SkillTree loaded = skills.get(0);
        assertEquals(skill.getSkillName(), loaded.getSkillName());
        assertEquals(skill.getWikiPage(), loaded.getWikiPage());
        assertNull(loaded.getComment());
        assertNull(loaded.getOpenPosition());
    }

    @Test
    public void testSoftDeleteSkillTree() {
        SkillTree skill = createTestSkillTree();
        UUID id = skill.getId();

        dataManager.remove(skill);

        try (Transaction tx = persistence.createTransaction()) {
            SkillTree deleted = persistence.getEntityManager()
                    .createQuery("select e from itpearls_SkillTree e where e.id = :id", SkillTree.class)
                    .setParameter("id", id)
                    .getSingleResult();
            tx.commit();
            assertNotNull(deleted.getDeleteTs());
        }

        List<SkillTree> active = dataManager.loadList(LoadContext.create(SkillTree.class)
                .setQuery(LoadContext.createQuery(
                        "select e from itpearls_SkillTree e where e.id = :id and e.deleteTs is null")
                        .setParameter("id", id))
                .setView(View.LOCAL));
        assertTrue(active.isEmpty());
    }

    private SkillTree createTestSkillTree() {
        SkillTree skill = dataManager.create(SkillTree.class);
        skill.setSkillName("TestSkill-" + UUID.randomUUID());
        skill.setNotParsing(false);
        skill.setPrioritySkill(0);
        skill.setWikiPage("https://example.com/skill");
        return tracker.track(dataManager.commit(skill, "skillTree-edit-view"));
    }
}
