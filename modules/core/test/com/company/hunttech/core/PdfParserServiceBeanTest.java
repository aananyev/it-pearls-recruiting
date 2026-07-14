package com.company.hunttech.core;

import com.company.hunttech.entity.SkillTree;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class PdfParserServiceBeanTest {

    @Test
    public void nullAndBlankInput_returnEmptyList() {
        assertTrue(PdfParserServiceBean.buildSkillSnapshots(null, Collections.emptyList()).isEmpty());
        assertTrue(PdfParserServiceBean.buildSkillSnapshots("   ", Collections.emptyList()).isEmpty());
    }

    @Test
    public void invalidAndDisabledRows_areSkippedWithoutException() {
        PdfParserServiceBean.SkillProjection invalidName = projection(
                UUID.randomUUID(), null, null, null, null,
                null, null, null, null, null);
        PdfParserServiceBean.SkillProjection disabled = projection(
                UUID.randomUUID(), "Java", true, 4, "Отключён",
                null, null, null, null, null);
        PdfParserServiceBean.SkillProjection enabled = projection(
                UUID.randomUUID(), "PostgreSQL", null, 3, "База данных",
                null, null, null, null, null);

        List<SkillTree> result = PdfParserServiceBean.buildSkillSnapshots(
                "Опыт работы с Java и PostgreSQL",
                Arrays.asList(invalidName, disabled, enabled));

        assertEquals(1, result.size());
        assertEquals("PostgreSQL", result.get(0).getSkillName());
    }

    @Test
    public void matchedSkillAndParent_areCompactSnapshots() throws Exception {
        UUID skillId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        PdfParserServiceBean.SkillProjection projection = projection(
                skillId, "Spring Boot", false, 2, "Фреймворк",
                parentId, "Java", false, 4, "Язык программирования");

        List<SkillTree> result = PdfParserServiceBean.buildSkillSnapshots(
                "Разработка сервисов на Spring Boot",
                Collections.singletonList(projection));

        assertEquals(2, result.size());
        SkillTree skill = result.get(0);
        SkillTree parent = result.get(1);

        assertEquals(skillId, skill.getId());
        assertEquals("Spring Boot", skill.getSkillName());
        assertEquals(Integer.valueOf(2), skill.getPrioritySkill());
        assertEquals("Фреймворк", skill.getComment());
        assertSame(parent, skill.getSkillTree());
        assertEquals(parentId, parent.getId());
        assertEquals("Java", parent.getSkillName());

        // Снимок не содержит тяжёлые обратные связи сущности SkillTree.
        assertNull(skill.getCandidates());
        assertNull(skill.getOpenPosition());
        assertNull(skill.getCandidateCV());
        assertNull(skill.getSpecialisation());
        assertNull(skill.getFileImageLogo());

        // Дополнительная страховка: компактный результат штатно сериализуется.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(output)) {
            objectOutputStream.writeObject(result);
        }
        assertFalse(output.toByteArray().length == 0);
        assertTrue(output.toByteArray().length < 64 * 1024);
    }

    @Test
    public void commonParent_isAddedOnlyOnce() {
        UUID parentId = UUID.randomUUID();
        PdfParserServiceBean.SkillProjection first = projection(
                UUID.randomUUID(), "Spring", false, 2, null,
                parentId, "Java", false, 4, null);
        PdfParserServiceBean.SkillProjection second = projection(
                UUID.randomUUID(), "Hibernate", false, 2, null,
                parentId, "Java", false, 4, null);

        List<SkillTree> result = PdfParserServiceBean.buildSkillSnapshots(
                "Spring Hibernate",
                Arrays.asList(first, second));

        assertEquals(3, result.size());
        long parentCount = result.stream()
                .filter(skill -> parentId.equals(skill.getId()))
                .count();
        assertEquals(1L, parentCount);
    }

    private static PdfParserServiceBean.SkillProjection projection(
            UUID skillId,
            String skillName,
            Boolean notParsing,
            Integer prioritySkill,
            String comment,
            UUID parentId,
            String parentSkillName,
            Boolean parentNotParsing,
            Integer parentPrioritySkill,
            String parentComment) {
        return new PdfParserServiceBean.SkillProjection(
                skillId,
                skillName,
                notParsing,
                prioritySkill,
                comment,
                parentId,
                parentSkillName,
                parentNotParsing,
                parentPrioritySkill,
                parentComment);
    }
}
