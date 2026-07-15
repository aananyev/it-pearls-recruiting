package com.company.hunttech.web.screens.jobcandidate;

import com.company.hunttech.entity.JobCandidate;
import com.haulmont.cuba.core.global.FetchMode;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.core.global.ViewProperty;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Проверяет, что оптимизатор исключает только {@code positionList},
 * сохраняя все остальные свойства, fetch-режимы и вложенные views.
 */
public class JobCandidatePositionInitialViewOptimizerTest {

    private final JobCandidatePositionInitialViewOptimizer optimizer =
            new JobCandidatePositionInitialViewOptimizer();

    @Test
    public void createsViewWithoutPositionList() {
        View source = new View(JobCandidate.class, "test-source", false);
        source.setLoadPartialEntities(true);
        source.addProperty("cityOfResidence", new View(JobCandidate.class, "city-view", false), FetchMode.AUTO);
        source.addProperty("currentCompany", new View(JobCandidate.class, "company-view", false), FetchMode.AUTO);
        source.addProperty("positionList", new View(JobCandidate.class, "position-view", false), FetchMode.AUTO);
        source.addProperty("personPosition", null, FetchMode.AUTO);

        View result = optimizer.createViewWithout(source);

        assertNotNull(result);
        assertEquals(JobCandidate.class, result.getEntityClass());
        assertTrue(result.loadPartialEntities());

        List<String> names = result.getProperties().stream()
                .map(ViewProperty::getName)
                .collect(Collectors.toList());

        assertTrue("cityOfResidence should be preserved", names.contains("cityOfResidence"));
        assertTrue("currentCompany should be preserved", names.contains("currentCompany"));
        assertTrue("personPosition should be preserved", names.contains("personPosition"));
        assertFalse("positionList should be excluded", names.contains("positionList"));

        assertEquals(3, result.getProperties().size());
    }

    @Test
    public void preservesFetchMode() {
        View source = new View(JobCandidate.class, "test-source", false);
        source.addProperty("cityOfResidence", null, FetchMode.UNDEFINED);
        source.addProperty("currentCompany", null, FetchMode.AUTO);

        View result = optimizer.createViewWithout(source);

        assertEquals(FetchMode.UNDEFINED,
                result.getProperty("cityOfResidence").getFetchMode());
        assertEquals(FetchMode.AUTO,
                result.getProperty("currentCompany").getFetchMode());
    }

    @Test
    public void preservesNestedViews() {
        View nested = new View(JobCandidate.class, "nested-view", false);
        View source = new View(JobCandidate.class, "test-source", false);
        source.addProperty("currentCompany", nested, FetchMode.AUTO);

        View result = optimizer.createViewWithout(source);

        assertNotNull(result.getProperty("currentCompany").getView());
        assertEquals("nested-view", result.getProperty("currentCompany").getView().getName());
    }
}
