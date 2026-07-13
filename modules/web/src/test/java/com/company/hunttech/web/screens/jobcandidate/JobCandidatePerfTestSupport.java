package com.company.hunttech.web.screens.jobcandidate;

import com.company.hunttech.core.InteractionService;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.service.GetRoleService;
import com.haulmont.cuba.core.app.DataService;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.KeyValueEntity;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.ValueLoadContext;
import com.haulmont.cuba.security.entity.Group;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.web.testsupport.TestContainer;
import com.haulmont.cuba.web.testsupport.TestEntityFactory;
import com.haulmont.cuba.web.testsupport.TestEntityState;
import com.haulmont.cuba.web.testsupport.TestUiEnvironment;
import com.haulmont.cuba.web.testsupport.proxy.DataServiceProxy;
import com.haulmont.cuba.web.testsupport.proxy.TestServiceProxy;
import mockit.Delegate;
import mockit.Expectations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Web-tier {@link DataService} mock helpers for JobCandidate screen performance tests.
 */
public final class JobCandidatePerfTestSupport {

    public static final String ADMIN_LOGIN = "admin";
    public static final String JOB_CANDIDATE_ENTITY = "hunttech_JobCandidate";
    public static final String CANDIDATE_CV_ENTITY = "hunttech_CandidateCV";
    public static final String COMPANY_ENTITY = "hunttech_Company";

    private JobCandidatePerfTestSupport() {
    }

    public static Metrics registerDataServiceDelegate(DataService dataService, TestUiEnvironment environment) {
        return registerDataServiceDelegate(dataService, environment, null, null);
    }

    public static Metrics registerDataServiceDelegate(DataService dataService, TestUiEnvironment environment,
                                                   GetRoleService getRoleService,
                                                   InteractionService interactionService) {
        initializeUserSession(environment);
        TestContainer testContainer = environment.getContainer();
        DataServiceProxy defaultProxy = new DataServiceProxy(testContainer);
        Metrics metrics = new Metrics();

        TestEntityFactory<JobCandidate> factory =
                testContainer.getEntityFactory(JobCandidate.class, TestEntityState.DETACHED);
        List<JobCandidate> browseFixtures = createBrowseFixtures(factory);

        new Expectations() {{
            dataService.load((LoadContext<? extends Entity>) any);
            result = new Delegate() {
                @SuppressWarnings("unchecked")
                Entity load(LoadContext<? extends Entity> loadContext) {
                    metrics.loadCalls.incrementAndGet();
                    if (isJobCandidate(loadContext)) {
                        metrics.jobCandidateLoadCalls.incrementAndGet();
                        Object id = loadContext.getId();
                        if (id instanceof UUID) {
                            UUID candidateId = (UUID) id;
                            return browseFixtures.stream()
                                    .filter(c -> candidateId.equals(c.getId()))
                                    .findFirst()
                                    .orElseGet(() -> factory.create(Collections.singletonMap("id", candidateId)));
                        }
                    }
                    return defaultProxy.load(loadContext);
                }
            };
            minTimes = 0;

            dataService.loadList((LoadContext<? extends Entity>) any);
            result = new Delegate() {
                @SuppressWarnings("unchecked")
                List<Entity> loadList(LoadContext<? extends Entity> loadContext) {
                    metrics.loadListCalls.incrementAndGet();
                    if (isJobCandidate(loadContext)) {
                        metrics.jobCandidateLoadListCalls.incrementAndGet();
                        return new ArrayList<>(browseFixtures);
                    }
                    if (isCompany(loadContext)) {
                        metrics.companyLoadListCalls.incrementAndGet();
                    }
                    if (isCandidateCV(loadContext)) {
                        metrics.candidateCvLoadListCalls.incrementAndGet();
                        return Collections.emptyList();
                    }
                    List list = defaultProxy.loadList(loadContext);
                    return list;
                }
            };
            minTimes = 0;

            dataService.getCount((LoadContext<? extends Entity>) any);
            result = new Delegate() {
                long getCount(LoadContext<? extends Entity> loadContext) {
                    metrics.getCountCalls.incrementAndGet();
                    if (isJobCandidate(loadContext)) {
                        metrics.jobCandidateGetCountCalls.incrementAndGet();
                        return browseFixtures.size();
                    }
                    return defaultProxy.getCount(loadContext);
                }
            };
            minTimes = 0;

            dataService.loadValues((ValueLoadContext) any);
            result = new Delegate() {
                List<KeyValueEntity> loadValues(ValueLoadContext loadContext) {
                    // Batch caches in JobCandidateBrowse use loadValues; delegate keeps perf tests realistic.
                    metrics.loadValuesCalls.incrementAndGet();
                    return defaultProxy.loadValues(loadContext);
                }
            };
            minTimes = 0;

            dataService.commit((CommitContext) any);
            result = new Delegate() {
                @SuppressWarnings("unchecked")
                java.util.Set<Entity> commit(CommitContext commitContext) {
                    return defaultProxy.commit(commitContext);
                }
            };
            minTimes = 0;
        }};

        TestServiceProxy.mock(DataService.class, dataService);

        if (getRoleService != null) {
            new Expectations() {{
                getRoleService.isUserRoles((User) any, anyString);
                result = false;
                minTimes = 0;
            }};
            TestServiceProxy.mock(GetRoleService.class, getRoleService);
        }

        if (interactionService != null) {
            new Expectations() {{
                interactionService.getLastIteraction((JobCandidate) any);
                result = null;
                minTimes = 0;
            }};
            TestServiceProxy.mock(InteractionService.class, interactionService);
        }

        return metrics;
    }

    public static void clearServiceMocks() {
        TestServiceProxy.clear();
    }

    public static void initializeUserSession(TestUiEnvironment environment) {
        Group group = environment.getContainer()
                .getEntityFactory(Group.class, TestEntityState.NEW)
                .create("name", "Administrators");
        environment.getUserSession().getUser().setGroup(group);
    }

    public static JobCandidate createJobCandidateForEdit(TestUiEnvironment environment) {
        TestEntityFactory<JobCandidate> factory =
                environment.getContainer().getEntityFactory(JobCandidate.class, TestEntityState.NEW);
        JobCandidate candidate = factory.create(
                "firstName", "Perf",
                "secondName", "Candidate",
                "fullName", "Perf Candidate");
        candidate.setIteractionList(Collections.emptyList());
        candidate.setCandidateCv(Collections.emptyList());
        return candidate;
    }

    private static List<JobCandidate> createBrowseFixtures(TestEntityFactory<JobCandidate> factory) {
        List<JobCandidate> fixtures = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            JobCandidate candidate = factory.create(
                    "firstName", "Perf" + i,
                    "secondName", "Candidate" + i,
                    "fullName", "Perf Candidate " + i);
            candidate.setIteractionList(Collections.emptyList());
            fixtures.add(candidate);
        }
        return fixtures;
    }

    private static boolean isJobCandidate(LoadContext<? extends Entity> loadContext) {
        return JOB_CANDIDATE_ENTITY.equals(loadContext.getEntityMetaClass());
    }

    private static boolean isCompany(LoadContext<? extends Entity> loadContext) {
        return COMPANY_ENTITY.equals(loadContext.getEntityMetaClass());
    }

    private static boolean isCandidateCV(LoadContext<? extends Entity> loadContext) {
        return CANDIDATE_CV_ENTITY.equals(loadContext.getEntityMetaClass());
    }

    public static final class Metrics {
        private final AtomicInteger loadCalls = new AtomicInteger();
        private final AtomicInteger loadListCalls = new AtomicInteger();
        private final AtomicInteger getCountCalls = new AtomicInteger();
        private final AtomicInteger loadValuesCalls = new AtomicInteger();
        private final AtomicInteger jobCandidateLoadCalls = new AtomicInteger();
        private final AtomicInteger jobCandidateLoadListCalls = new AtomicInteger();
        private final AtomicInteger jobCandidateGetCountCalls = new AtomicInteger();
        private final AtomicInteger companyLoadListCalls = new AtomicInteger();
        private final AtomicInteger candidateCvLoadListCalls = new AtomicInteger();

        public int getLoadCalls() {
            return loadCalls.get();
        }

        public int getLoadListCalls() {
            return loadListCalls.get();
        }

        public int getGetCountCalls() {
            return getCountCalls.get();
        }

        public int getLoadValuesCalls() {
            return loadValuesCalls.get();
        }

        public int getJobCandidateLoadCalls() {
            return jobCandidateLoadCalls.get();
        }

        public int getJobCandidateLoadListCalls() {
            return jobCandidateLoadListCalls.get();
        }

        public int getJobCandidateGetCountCalls() {
            return jobCandidateGetCountCalls.get();
        }

        public int getCompanyLoadListCalls() {
            return companyLoadListCalls.get();
        }

        public int getCandidateCvLoadListCalls() {
            return candidateCvLoadListCalls.get();
        }
    }
}
