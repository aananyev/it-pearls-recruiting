package com.company.hunttech.web.screens.company;

import com.company.hunttech.entity.Company;
import com.haulmont.cuba.core.app.DataService;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.KeyValueEntity;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.ValueLoadContext;
import com.haulmont.cuba.security.entity.Group;
import com.haulmont.cuba.web.testsupport.TestContainer;
import com.haulmont.cuba.web.testsupport.TestEntityFactory;
import com.haulmont.cuba.web.testsupport.TestEntityState;
import com.haulmont.cuba.web.testsupport.TestUiEnvironment;
import com.haulmont.cuba.web.testsupport.proxy.DataServiceProxy;
import com.haulmont.cuba.web.testsupport.proxy.TestServiceProxy;
import mockit.Delegate;
import mockit.Expectations;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public final class CompanyPerfTestSupport {

    public static final String ADMIN_LOGIN = "admin";
    public static final String COMPANY_ENTITY = "hunttech_Company";

    private CompanyPerfTestSupport() {
    }

    public static Metrics registerDataServiceDelegate(DataService dataService, TestUiEnvironment environment) {
        initializeUserSession(environment);
        TestContainer testContainer = environment.getContainer();
        DataServiceProxy defaultProxy = new DataServiceProxy(testContainer);
        Metrics metrics = new Metrics();

        new Expectations() {{
            dataService.load((LoadContext<? extends Entity>) any);
            result = new Delegate() {
                @SuppressWarnings("unchecked")
                Entity load(LoadContext<? extends Entity> loadContext) {
                    metrics.loadCalls.incrementAndGet();
                    if (isCompany(loadContext)) {
                        metrics.companyLoadCalls.incrementAndGet();
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
                    if (isCompany(loadContext)) {
                        metrics.companyLoadListCalls.incrementAndGet();
                    }
                    return (List<Entity>) defaultProxy.loadList((LoadContext<Entity>) loadContext);
                }
            };
            minTimes = 0;

            dataService.getCount((LoadContext<? extends Entity>) any);
            result = new Delegate() {
                long getCount(LoadContext<? extends Entity> loadContext) {
                    metrics.getCountCalls.incrementAndGet();
                    if (isCompany(loadContext)) {
                        metrics.companyGetCountCalls.incrementAndGet();
                    }
                    return defaultProxy.getCount(loadContext);
                }
            };
            minTimes = 0;

            dataService.loadValues((ValueLoadContext) any);
            result = new Delegate() {
                List<KeyValueEntity> loadValues(ValueLoadContext loadContext) {
                    metrics.loadValuesCalls.incrementAndGet();
                    return defaultProxy.loadValues(loadContext);
                }
            };
            minTimes = 0;

            dataService.commit((CommitContext) any);
            result = new Delegate() {
                Set<Entity> commit(CommitContext commitContext) {
                    return defaultProxy.commit(commitContext);
                }
            };
            minTimes = 0;
        }};

        TestServiceProxy.mock(DataService.class, dataService);
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

    public static Company createCompanyForEdit(TestUiEnvironment environment) {
        TestEntityFactory<Company> factory =
                environment.getContainer().getEntityFactory(Company.class, TestEntityState.NEW);
        Company company = factory.create(
                "comanyName", "Perf Company",
                "companyShortName", "Perf");
        company.setOurClient(false);
        company.setOurLegalEntity(false);
        company.setDepartmentOfCompany(Collections.emptyList());
        return company;
    }

    private static boolean isCompany(LoadContext<? extends Entity> loadContext) {
        return COMPANY_ENTITY.equals(loadContext.getEntityMetaClass());
    }

    public static final class Metrics {
        private final AtomicInteger loadCalls = new AtomicInteger();
        private final AtomicInteger loadListCalls = new AtomicInteger();
        private final AtomicInteger getCountCalls = new AtomicInteger();
        private final AtomicInteger loadValuesCalls = new AtomicInteger();
        private final AtomicInteger companyLoadCalls = new AtomicInteger();
        private final AtomicInteger companyLoadListCalls = new AtomicInteger();
        private final AtomicInteger companyGetCountCalls = new AtomicInteger();

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

        public int getCompanyLoadCalls() {
            return companyLoadCalls.get();
        }

        public int getCompanyLoadListCalls() {
            return companyLoadListCalls.get();
        }

        public int getCompanyGetCountCalls() {
            return companyGetCountCalls.get();
        }

        public void reset() {
            loadCalls.set(0);
            loadListCalls.set(0);
            getCountCalls.set(0);
            loadValuesCalls.set(0);
            companyLoadCalls.set(0);
            companyLoadListCalls.set(0);
            companyGetCountCalls.set(0);
        }
    }
}
