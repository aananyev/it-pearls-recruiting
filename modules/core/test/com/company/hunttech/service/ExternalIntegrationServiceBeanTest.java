package com.company.hunttech.service;

import com.company.hunttech.dto.integration.CompanyCreateRequestDto;
import com.company.hunttech.dto.integration.CompanyResponseDto;
import com.company.hunttech.entity.City;
import com.company.hunttech.entity.Company;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ExternalIntegrationServiceBeanTest {

    private ExternalIntegrationServiceBean service;
    private DataManager mockDataManager;
    private Metadata mockMetadata;

    @Before
    public void setUp() throws Exception {
        service = new ExternalIntegrationServiceBean();
        mockDataManager = mock(DataManager.class, Mockito.RETURNS_DEEP_STUBS);
        mockMetadata = mock(Metadata.class);

        injectField(service, "dataManager", mockDataManager);
        injectField(service, "metadata", mockMetadata);
    }

    @Test
    public void testCreateCompanySuccess() {
        Company newCompany = new Company();
        UUID companyId = UUID.randomUUID();
        newCompany.setId(companyId);

        when(mockMetadata.create(Company.class)).thenReturn(newCompany);
        when(mockDataManager.load(Company.class).query(anyString()).parameter(anyString(), any()).optional())
                .thenReturn(Optional.empty());

        CompanyCreateRequestDto request = new CompanyCreateRequestDto();
        request.setCompanyName("ООО Рога и Копыта");
        request.setInn("7701234567");
        request.setWebsite("https://example.com");
        request.setExternalId("ext-comp-101");
        request.setCorrelationId("corr-comp-1");

        CompanyResponseDto response = service.createCompany(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(companyId.toString(), response.getCompanyId());
        assertEquals("ext-comp-101", response.getExternalId());
        assertEquals("ООО Рога и Копыта", response.getCompanyName());
        assertEquals(CompanyResponseDto.STATUS_CREATED, response.getStatus());
        assertEquals("corr-comp-1", response.getCorrelationId());
        assertEquals("https://example.com", newCompany.getWebsite());

        verify(mockDataManager).commit(newCompany);
    }

    @Test
    public void testCreateCompanyIdempotencyKeyReturnsCachedResponse() {
        Company newCompany = new Company();
        UUID companyId = UUID.randomUUID();
        newCompany.setId(companyId);

        when(mockMetadata.create(Company.class)).thenReturn(newCompany);
        when(mockDataManager.load(Company.class).query(anyString()).parameter(anyString(), any()).optional())
                .thenReturn(Optional.empty());

        CompanyCreateRequestDto request = new CompanyCreateRequestDto();
        request.setCompanyName("Компания Идемпотентность");
        request.setIdempotencyKey("idem-key-100");
        request.setExternalId("ext-100");

        CompanyResponseDto firstResponse = service.createCompany(request);
        assertNotNull(firstResponse);
        assertTrue(firstResponse.isSuccess());
        assertEquals(CompanyResponseDto.STATUS_CREATED, firstResponse.getStatus());

        // Второй запрос с тем же idempotencyKey
        CompanyResponseDto secondResponse = service.createCompany(request);
        assertNotNull(secondResponse);
        assertTrue(secondResponse.isSuccess());
        assertEquals(firstResponse.getCompanyId(), secondResponse.getCompanyId());

        // commit должен быть вызван только один раз
        verify(mockDataManager, times(1)).commit(any(Company.class));
    }

    @Test
    public void testCreateCompanyDeduplicationByInn() {
        Company existing = new Company();
        UUID existingId = UUID.randomUUID();
        existing.setId(existingId);
        existing.setComanyName("Существующая компания");
        existing.setInn("7701234567");

        when(mockDataManager.load(Company.class).query(contains("e.inn = :inn")).parameter(eq("inn"), eq("7701234567")).optional())
                .thenReturn(Optional.of(existing));

        CompanyCreateRequestDto request = new CompanyCreateRequestDto();
        request.setCompanyName("Новое Название");
        request.setInn("7701234567");
        request.setExternalId("ext-dup-inn");
        request.setCorrelationId("corr-dup-1");

        CompanyResponseDto response = service.createCompany(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(existingId.toString(), response.getCompanyId());
        assertEquals("ext-dup-inn", response.getExternalId());
        assertEquals(CompanyResponseDto.STATUS_EXISTING_FOUND, response.getStatus());

        verify(mockDataManager, never()).commit(any(Company.class));
    }

    @Test
    public void testCreateCompanyDeduplicationByName() {
        Company existing = new Company();
        UUID existingId = UUID.randomUUID();
        existing.setId(existingId);
        existing.setComanyName("ООО Вектор");

        when(mockDataManager.load(Company.class).query(contains("lower(e.comanyName) = :name")).parameter(eq("name"), eq("ооо вектор")).optional())
                .thenReturn(Optional.of(existing));

        CompanyCreateRequestDto request = new CompanyCreateRequestDto();
        request.setCompanyName("ООО Вектор");
        request.setExternalId("ext-dup-name");

        CompanyResponseDto response = service.createCompany(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(existingId.toString(), response.getCompanyId());
        assertEquals(CompanyResponseDto.STATUS_EXISTING_FOUND, response.getStatus());
        verify(mockDataManager, never()).commit(any(Company.class));
    }

    @Test
    public void testCreateCompanyValidationFailureEmptyName() {
        CompanyCreateRequestDto request = new CompanyCreateRequestDto();
        request.setCompanyName("   ");
        request.setCorrelationId("corr-fail-name");

        CompanyResponseDto response = service.createCompany(request);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("corr-fail-name", response.getCorrelationId());
        assertNotNull(response.getErrorDetails());
        assertEquals("VALIDATION_ERROR", response.getErrorDetails().getErrorCode());
        assertTrue(response.getMessage().contains("Наименование компании обязательно"));
    }

    @Test
    public void testCreateCompanyValidationFailureInvalidInn() {
        CompanyCreateRequestDto request = new CompanyCreateRequestDto();
        request.setCompanyName("Компания");
        request.setInn("12345"); // Неверная длина

        CompanyResponseDto response = service.createCompany(request);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertNotNull(response.getErrorDetails());
        assertEquals("VALIDATION_ERROR", response.getErrorDetails().getErrorCode());
        assertTrue(response.getMessage().contains("ИНН должен содержать 10 или 12 цифр"));
    }

    private static void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
