package com.company.hunttech.core;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PdfParserServiceBeanTest {

    @Test
    public void nullAndBlankInput_returnEmptyList() {
        assertTrue(PdfParserServiceBean.selectSkillIds(null, Collections.emptyList()).isEmpty());
        assertTrue(PdfParserServiceBean.selectSkillIds("   ", Collections.emptyList()).isEmpty());
    }

    @Test
    public void disabledUndefinedAndInvalidRows_areSkipped() {
        UUID enabledId = UUID.randomUUID();
        PdfParserServiceBean.SkillProjection invalidName = projection(
                UUID.randomUUID(), null, false,
                null, null, null);
        PdfParserServiceBean.SkillProjection disabled = projection(
                UUID.randomUUID(), "Java", true,
                null, null, null);
        PdfParserServiceBean.SkillProjection undefined = projection(
                UUID.randomUUID(), "Kotlin", null,
                null, null, null);
        PdfParserServiceBean.SkillProjection enabled = projection(
                enabledId, "PostgreSQL", false,
                null, null, null);

        List<UUID> result = PdfParserServiceBean.selectSkillIds(
                "Опыт работы с Java, Kotlin и PostgreSQL",
                Arrays.asList(invalidName, disabled, undefined, enabled));

        assertEquals(Collections.singletonList(enabledId), result);
    }

    @Test
    public void matchedSkillAndParent_keepBusinessOrder() {
        UUID skillId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        PdfParserServiceBean.SkillProjection projection = projection(
                skillId, "Spring Boot", false,
                parentId, "Java", false);

        List<UUID> result = PdfParserServiceBean.selectSkillIds(
                "Разработка сервисов на Spring Boot",
                Collections.singletonList(projection));

        assertEquals(Arrays.asList(skillId, parentId), result);
    }

    @Test
    public void commonParent_isAddedOnlyOnce() {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        PdfParserServiceBean.SkillProjection first = projection(
                firstId, "Spring", false,
                parentId, "Java", false);
        PdfParserServiceBean.SkillProjection second = projection(
                secondId, "Hibernate", false,
                parentId, "Java", false);

        List<UUID> result = PdfParserServiceBean.selectSkillIds(
                "Spring Hibernate",
                Arrays.asList(first, second));

        assertEquals(Arrays.asList(firstId, parentId, secondId), result);
    }

    @Test
    public void parentWithUndefinedFlag_isNotAdded() {
        UUID skillId = UUID.randomUUID();
        PdfParserServiceBean.SkillProjection projection = projection(
                skillId, "Spring", false,
                UUID.randomUUID(), "Java", null);

        List<UUID> result = PdfParserServiceBean.selectSkillIds(
                "Spring",
                Collections.singletonList(projection));

        assertEquals(Collections.singletonList(skillId), result);
    }

    @Test
    public void parentWithAlreadySelectedName_isNotDuplicated() {
        UUID javaId = UUID.randomUUID();
        UUID springId = UUID.randomUUID();
        UUID duplicatedParentId = UUID.randomUUID();
        PdfParserServiceBean.SkillProjection java = projection(
                javaId, "Java", false,
                null, null, null);
        PdfParserServiceBean.SkillProjection spring = projection(
                springId, "Spring", false,
                duplicatedParentId, "JAVA", false);

        List<UUID> result = PdfParserServiceBean.selectSkillIds(
                "Java Spring",
                Arrays.asList(java, spring));

        assertEquals(Arrays.asList(javaId, springId), result);
    }

    private static PdfParserServiceBean.SkillProjection projection(
            UUID skillId,
            String skillName,
            Boolean notParsing,
            UUID parentId,
            String parentSkillName,
            Boolean parentNotParsing) {
        return new PdfParserServiceBean.SkillProjection(
                skillId,
                skillName,
                notParsing,
                parentId,
                parentSkillName,
                parentNotParsing);
    }
}
