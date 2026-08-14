package com.company.hunttech.web.screens.jobcandidate;

import com.company.hunttech.entity.Company;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class JobCandidateCreatedCompanyResolverTest {

    @Test
    public void nullCompanyDoesNotTriggerLoadOrMerge() {
        AtomicBoolean loaderCalled = new AtomicBoolean(false);
        AtomicBoolean mergerCalled = new AtomicBoolean(false);

        Company result = JobCandidateEdit.resolveCreatedCompany(
                null,
                id -> {
                    loaderCalled.set(true);
                    return null;
                },
                company -> {
                    mergerCalled.set(true);
                    return company;
                });

        assertNull(result);
        assertTrue(!loaderCalled.get());
        assertTrue(!mergerCalled.get());
    }

    @Test
    public void companyWithoutIdDoesNotTriggerLoadOrMerge() {
        Company transientCompany = new Company();
        // CUBA entity may auto-generate UUID in constructor;
        // force id to null to simulate truly unsaved entity
        transientCompany.setId(null);
        AtomicBoolean loaderCalled = new AtomicBoolean(false);
        AtomicBoolean mergerCalled = new AtomicBoolean(false);

        Company result = JobCandidateEdit.resolveCreatedCompany(
                transientCompany,
                id -> {
                    loaderCalled.set(true);
                    return null;
                },
                company -> {
                    mergerCalled.set(true);
                    return company;
                });

        assertSame(transientCompany, result);
        assertTrue(!loaderCalled.get());
        assertTrue(!mergerCalled.get());
    }

    @Test
    public void persistedCompanyIsLoadedByIdAndMergedIntoScreenContext() {
        UUID companyId = UUID.randomUUID();
        Company editorResult = new Company();
        editorResult.setId(companyId);
        Company loadedCompany = new Company();
        loadedCompany.setId(companyId);
        Company mergedCompany = new Company();
        mergedCompany.setId(companyId);
        AtomicReference<UUID> loadedId = new AtomicReference<>();
        AtomicReference<Company> mergedValue = new AtomicReference<>();

        Company result = JobCandidateEdit.resolveCreatedCompany(
                editorResult,
                id -> {
                    loadedId.set(id);
                    return loadedCompany;
                },
                company -> {
                    mergedValue.set(company);
                    return mergedCompany;
                });

        assertSame(companyId, loadedId.get());
        assertSame(loadedCompany, mergedValue.get());
        assertSame(mergedCompany, result);
    }
}
