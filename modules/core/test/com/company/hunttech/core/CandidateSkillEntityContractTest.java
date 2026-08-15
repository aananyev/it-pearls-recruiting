package com.company.hunttech.core;

import com.company.hunttech.entity.CandidateSkill;
import com.company.hunttech.entity.CandidateSkillPriority;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.entity.SkillTree;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import static org.junit.Assert.*;

/**
 * Тест контракта сущности CandidateSkill («Навыки кандидата») и миграций базы данных.
 */
public class CandidateSkillEntityContractTest {

    private static final String SQL_FILE = "modules/core/db/update/postgres/26/260815-2-addCandidateSkillEntity.sql";
    private static final String XML_FILE = "modules/core/db/changelog/260815-2-addCandidateSkillEntity.xml";
    private static final String MASTER_CHANGELOG = "modules/core/db/changelog/db.changelog-master.xml";
    private static final String PERSISTENCE_XML = "modules/global/src/com/company/hunttech/persistence.xml";
    private static final String VIEWS_XML = "modules/global/src/com/company/hunttech/views.xml";

    @Test
    public void testCandidateSkillEntityClassProperties() {
        CandidateSkill candidateSkill = new CandidateSkill();
        JobCandidate candidate = new JobCandidate();
        candidate.setFullName("Иван Иванов");

        SkillTree skill = new SkillTree();
        skill.setSkillName("Java");

        candidateSkill.setCandidate(candidate);
        candidateSkill.setSkill(skill);
        candidateSkill.setPriority(CandidateSkillPriority.MAIN);

        assertEquals(candidate, candidateSkill.getCandidate());
        assertEquals(skill, candidateSkill.getSkill());
        assertEquals(CandidateSkillPriority.MAIN, candidateSkill.getPriority());

        candidateSkill.setPriority(CandidateSkillPriority.SECONDARY);
        assertEquals(CandidateSkillPriority.SECONDARY, candidateSkill.getPriority());

        candidateSkill.setPriority(CandidateSkillPriority.TERTIARY);
        assertEquals(CandidateSkillPriority.TERTIARY, candidateSkill.getPriority());
    }

    @Test
    public void testCandidateSkillPriorityEnum() {
        assertEquals(Integer.valueOf(10), CandidateSkillPriority.MAIN.getId());
        assertEquals(Integer.valueOf(20), CandidateSkillPriority.SECONDARY.getId());
        assertEquals(Integer.valueOf(30), CandidateSkillPriority.TERTIARY.getId());

        assertEquals(CandidateSkillPriority.MAIN, CandidateSkillPriority.fromId(10));
        assertEquals(CandidateSkillPriority.SECONDARY, CandidateSkillPriority.fromId(20));
        assertEquals(CandidateSkillPriority.TERTIARY, CandidateSkillPriority.fromId(30));
        assertNull(CandidateSkillPriority.fromId(999));
        assertNull(CandidateSkillPriority.fromId(null));
    }

    @Test
    public void testDatabaseMigrationFilesExistAndIncluded() throws IOException {
        String sql = readProjectFile(SQL_FILE);
        String xml = readProjectFile(XML_FILE);
        String master = readProjectFile(MASTER_CHANGELOG);

        assertTrue("Таблица HUNTTECH_CANDIDATE_SKILL должна создаваться в SQL",
                sql.contains("HUNTTECH_CANDIDATE_SKILL"));
        assertTrue("Таблица HUNTTECH_CANDIDATE_SKILL должна создаваться в XML",
                xml.contains("HUNTTECH_CANDIDATE_SKILL"));
        assertTrue("Миграция должна быть включена в db.changelog-master.xml",
                master.contains("260815-2-addCandidateSkillEntity.xml"));

        assertTrue("Должен быть уникальный индекс от дубликатов навыков у кандидата",
                sql.contains("IDX_HUNTTECH_CANDIDATE_SKILL_UNQ") &&
                xml.contains("IDX_HUNTTECH_CANDIDATE_SKILL_UNQ"));
    }

    @Test
    public void testPersistenceAndViewsRegistration() throws IOException {
        String persistence = readProjectFile(PERSISTENCE_XML);
        String views = readProjectFile(VIEWS_XML);

        assertTrue("CandidateSkill должен быть зарегистрирован в persistence.xml",
                persistence.contains("com.company.hunttech.entity.CandidateSkill"));
        assertTrue("candidateSkill-view должен быть зарегистрирован в views.xml",
                views.contains("name=\"candidateSkill-view\""));
    }

    private static String readProjectFile(String relativePath) throws IOException {
        Path root = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("build.gradle"))) {
            root = root.getParent();
        }
        assertNotNull("Не найден корень проекта HRM HuntTech", root);
        return new String(Files.readAllBytes(root.resolve(relativePath)), StandardCharsets.UTF_8);
    }
}
