package com.company.hunttech.service;

import com.company.hunttech.dto.integration.ReferenceFilterDto;
import com.company.hunttech.dto.integration.ReferenceListResponseDto;
import com.company.hunttech.entity.City;
import com.company.hunttech.entity.Country;
import com.company.hunttech.entity.Grade;
import com.company.hunttech.entity.Position;
import com.haulmont.cuba.core.global.DataManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ExternalReferenceDataServiceBeanTest {

    private ExternalReferenceDataServiceBean service;
    private DataManager mockDataManager;

    @Before
    public void setUp() throws Exception {
        service = new ExternalReferenceDataServiceBean();
        mockDataManager = mock(DataManager.class, Mockito.RETURNS_DEEP_STUBS);
        injectField(service, "dataManager", mockDataManager);
    }

    @Test
    public void testGetCitiesSuccess() {
        City city = new City();
        city.setCityRuName("Москва");
        city.setCityEngName("Moscow");
        city.setFiasId("0c5b2444-70a0-4932-980c-b4dc0d3f02b5");

        when(mockDataManager.loadValue(anyString(), eq(Long.class)).optional()).thenReturn(Optional.of(1L));
        when(mockDataManager.load(City.class).query(anyString()).firstResult(anyInt()).maxResults(anyInt()).list())
                .thenReturn(Collections.singletonList(city));

        ReferenceFilterDto filter = new ReferenceFilterDto();
        filter.setSearch("Моск");
        filter.setCorrelationId("test-corr-1");

        ReferenceListResponseDto response = service.getCities(filter);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(1, response.getTotalCount());
        assertEquals(1, response.getItems().size());
        assertEquals("Москва", response.getItems().get(0).getName());
        assertEquals("0c5b2444-70a0-4932-980c-b4dc0d3f02b5", response.getItems().get(0).getCode());
        assertEquals("test-corr-1", response.getCorrelationId());
    }

    @Test
    public void testGetPositionsSuccess() {
        Position pos = new Position();
        pos.setPositionRuName("Разработчик Java");
        pos.setPositionEnName("Java Developer");

        when(mockDataManager.loadValue(anyString(), eq(Long.class)).optional()).thenReturn(Optional.of(1L));
        when(mockDataManager.load(Position.class).query(anyString()).firstResult(anyInt()).maxResults(anyInt()).list())
                .thenReturn(Collections.singletonList(pos));

        ReferenceFilterDto filter = new ReferenceFilterDto();
        ReferenceListResponseDto response = service.getPositions(filter);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(1, response.getTotalCount());
        assertEquals("Разработчик Java", response.getItems().get(0).getName());
        assertEquals("Java Developer", response.getItems().get(0).getDescription());
    }

    @Test
    public void testGetGradesSuccess() {
        Grade grade = new Grade();
        grade.setGradeName("Senior");

        when(mockDataManager.loadValue(anyString(), eq(Long.class)).optional()).thenReturn(Optional.of(1L));
        when(mockDataManager.load(Grade.class).query(anyString()).firstResult(anyInt()).maxResults(anyInt()).list())
                .thenReturn(Collections.singletonList(grade));

        ReferenceFilterDto filter = new ReferenceFilterDto();
        ReferenceListResponseDto response = service.getGrades(filter);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Senior", response.getItems().get(0).getName());
    }

    @Test
    public void testGetCountriesSuccess() {
        Country country = new Country();
        country.setCountryRuName("Россия");
        country.setCountryEngName("Russia");
        country.setAlpha3Code("RUS");
        country.setCountryShortName("RU");

        when(mockDataManager.loadValue(anyString(), eq(Long.class)).optional()).thenReturn(Optional.of(1L));
        when(mockDataManager.load(Country.class).query(anyString()).firstResult(anyInt()).maxResults(anyInt()).list())
                .thenReturn(Collections.singletonList(country));

        ReferenceFilterDto filter = new ReferenceFilterDto();
        ReferenceListResponseDto response = service.getCountries(filter);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(1, response.getTotalCount());
        assertEquals("Россия", response.getItems().get(0).getName());
        assertEquals("RUS", response.getItems().get(0).getCode());
        assertEquals("Russia", response.getItems().get(0).getDescription());
    }

    @Test
    public void testErrorHandlingReturnsStructuredResponse() {
        when(mockDataManager.loadValue(anyString(), eq(Long.class)).optional())
                .thenThrow(new RuntimeException("Database connection timeout"));

        ReferenceFilterDto filter = new ReferenceFilterDto();
        filter.setCorrelationId("err-corr-99");
        ReferenceListResponseDto response = service.getCities(filter);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("err-corr-99", response.getCorrelationId());
        assertTrue(response.getErrorMessage().contains("err-corr-99"));
        assertNotNull(response.getErrorDetails());
        assertEquals("SYSTEM_ERROR", response.getErrorDetails().getErrorCode());
    }

    @Test
    public void testInvalidPaginationReturnsValidationError() {
        ReferenceFilterDto filter = new ReferenceFilterDto();
        filter.setLimit(-5);
        filter.setCorrelationId("corr-invalid-limit");

        ReferenceListResponseDto response = service.getCities(filter);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("corr-invalid-limit", response.getCorrelationId());
        assertTrue(response.getErrorMessage().contains("limit"));
        assertNotNull(response.getErrorDetails());
        assertEquals("VALIDATION_ERROR", response.getErrorDetails().getErrorCode());
    }

    @Test
    public void testInvalidParentIdInSkillsReturnsValidationError() {
        ReferenceFilterDto filter = new ReferenceFilterDto();
        filter.setParentId("not-a-valid-uuid");
        filter.setCorrelationId("corr-invalid-uuid");

        ReferenceListResponseDto response = service.getSkills(filter);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("corr-invalid-uuid", response.getCorrelationId());
        assertTrue(response.getErrorMessage().contains("Неверный формат parentId"));
        assertNotNull(response.getErrorDetails());
        assertEquals("VALIDATION_ERROR", response.getErrorDetails().getErrorCode());
    }

    private static void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
