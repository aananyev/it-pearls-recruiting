package com.company.hunttech.core;

import com.company.hunttech.HunttechTestContainer;
import com.company.hunttech.TestEntityTracker;
import com.company.hunttech.entity.City;
import com.company.hunttech.entity.Company;
import com.company.hunttech.entity.Country;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.entity.Region;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.DataManager;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.persistence.PersistenceException;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class JobCandidateCompanyPersistenceTest {

    @ClassRule
    public static HunttechTestContainer cont = HunttechTestContainer.Common.INSTANCE;

    private DataManager dataManager;
    private TestEntityTracker tracker;

    @Before
    public void setUp() {
        dataManager = AppBeans.get(DataManager.class);
        tracker = new TestEntityTracker(dataManager);
    }

    @After
    public void tearDown() {
        tracker.cleanup();
    }

    @Test
    public void existingCompanyIsLinkedToJobCandidate() {
        Company company = createTestCompany("Existing");
        JobCandidate candidate = createTestCandidate("Existing", company);

        JobCandidate reloaded = reloadCandidate(candidate.getId());

        assertEquals(company.getId(), reloaded.getCurrentCompany().getId());
    }

    @Test
    public void newCompanyIsSavedAndCandidateFkPointsToIt() {
        Company company = createTestCompany("New");
        JobCandidate candidate = createTestCandidate("New", company);

        JobCandidate reloaded = reloadCandidate(candidate.getId());

        assertNotNull(company.getId());
        assertEquals(company.getId(), reloaded.getCurrentCompany().getId());
    }

    @Test
    public void savingCompanyLinkDoesNotChangeOtherCandidateFields() {
        Company company = createTestCompany("Preserve");
        JobCandidate candidate = createTestCandidate("Preserve", company);

        JobCandidate loaded = reloadCandidate(candidate.getId());
        loaded.setMiddleName("Middle-" + UUID.randomUUID());
        loaded.setCurrentCompany(company);
        dataManager.commit(loaded, "jobCandidate-view");

        JobCandidate reloaded = reloadCandidate(candidate.getId());

        assertEquals(loaded.getFirstName(), reloaded.getFirstName());
        assertEquals(loaded.getSecondName(), reloaded.getSecondName());
        assertEquals(loaded.getMiddleName(), reloaded.getMiddleName());
        assertEquals(company.getId(), reloaded.getCurrentCompany().getId());
    }

    @Test
    public void cancelLikeUnsavedCompanyIsNotPersistedAndCandidateCompanyIsUnchanged() {
        Company initialCompany = createTestCompany("Initial");
        JobCandidate candidate = createTestCandidate("Cancel", initialCompany);
        Company unsavedCompany = dataManager.create(Company.class);
        String unsavedName = "HT-CancelCompany-" + UUID.randomUUID();
        unsavedCompany.setComanyName(unsavedName);

        JobCandidate reloaded = reloadCandidate(candidate.getId());
        List<Company> companies = dataManager.load(Company.class)
                .query("select e from hunttech_Company e where e.comanyName = :name")
                .parameter("name", unsavedName)
                .list();

        assertEquals(initialCompany.getId(), reloaded.getCurrentCompany().getId());
        assertTrue(companies.isEmpty());
    }

    @Test
    public void savingCompanyLinkDoesNotCreateExtraJobCandidate() {
        Company company = createTestCompany("NoExtraCandidate");
        String firstName = "HT-NoExtra-" + UUID.randomUUID();
        JobCandidate candidate = createTestCandidate(firstName, "Candidate", company);

        Long count = dataManager.loadValue(
                        "select count(e) from hunttech_JobCandidate e where e.firstName = :firstName", Long.class)
                .parameter("firstName", firstName)
                .one();

        assertEquals(Long.valueOf(1), count);
        assertEquals(company.getId(), reloadCandidate(candidate.getId()).getCurrentCompany().getId());
    }

    @Test(expected = PersistenceException.class)
    public void companyValidationIsNotBypassed() {
        Company company = dataManager.create(Company.class);
        company.setCompanyShortName("Invalid");

        dataManager.commit(company, "company-edit-view");
    }

    private JobCandidate createTestCandidate(String marker, Company company) {
        return createTestCandidate("HT-" + marker + "-" + UUID.randomUUID(), "Candidate", company);
    }

    private JobCandidate createTestCandidate(String firstName, String secondName, Company company) {
        JobCandidate candidate = dataManager.create(JobCandidate.class);
        candidate.setFirstName(firstName);
        candidate.setSecondName(secondName);
        candidate.setCurrentCompany(company);
        return tracker.track(dataManager.commit(candidate, "jobCandidate-view"));
    }

    private JobCandidate reloadCandidate(UUID id) {
        return dataManager.load(JobCandidate.class)
                .id(id)
                .view("jobCandidate-view")
                .one();
    }

    private Company createTestCompany(String marker) {
        City city = createTestCity(marker);
        Company company = dataManager.create(Company.class);
        company.setComanyName("HT-" + marker + "-Company-" + shortId());
        company.setCompanyShortName("HT-" + marker);
        company.setOurClient(false);
        company.setOurLegalEntity(false);
        company.setCityOfCompany(city);
        company.setRegionOfCompany(city.getCityRegion());
        company.setCountryOfCompany(city.getCityRegion().getRegionCountry());
        return tracker.track(dataManager.commit(company, "company-edit-view"));
    }

    private City createTestCity(String marker) {
        Country country = dataManager.create(Country.class);
        country.setCountryRuName("HT-" + marker + "-Country-" + shortId());
        country.setCountryShortName("HT");
        country = tracker.track(dataManager.commit(country, "country-edit-view"));

        Region region = dataManager.create(Region.class);
        region.setRegionRuName("HT-" + marker + "-Region-" + shortId());
        region.setRegionCountry(country);
        region = tracker.track(dataManager.commit(region, "region-edit-view"));

        City city = dataManager.create(City.class);
        city.setCityRuName("HT-" + marker + "-City-" + shortId());
        city.setCityRegion(region);
        return tracker.track(dataManager.commit(city, "city-edit-view"));
    }

    private String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
